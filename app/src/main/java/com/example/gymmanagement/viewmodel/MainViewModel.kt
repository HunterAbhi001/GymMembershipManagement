package com.example.gymmanagement.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.MembershipHistory
import com.example.gymmanagement.data.database.Payment
import com.example.gymmanagement.data.database.Plan
import com.example.gymmanagement.ui.screens.MonthlySignupData
import com.example.gymmanagement.ui.screens.PlanPopularityData
import com.example.gymmanagement.ui.utils.CsvUtils
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.screens.UiTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    data class SaveSuccess(val member: Member, val shouldSendMessage: Boolean) : UiEvent()
}

class MainViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _collectionDateFilter = MutableStateFlow("This Month")
    private val _customDateRange = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null))

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deleteProgress = MutableStateFlow(0)
    val deleteProgress: StateFlow<Int> = _deleteProgress.asStateFlow()
    private val _deleteTotal = MutableStateFlow(0)
    val deleteTotal: StateFlow<Int> = _deleteTotal.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _memberHistory = MutableStateFlow<List<MembershipHistory>>(emptyList())
    val memberHistory: StateFlow<List<MembershipHistory>> = _memberHistory.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private val userUidFlow = callbackFlow<String?> {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allMembers: StateFlow<List<Member>> = userUidFlow
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                firestore.collection("users").document(userId).collection("members")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Member::class.java)?.copy(idString = doc.id)
                        }
                    }
            }
        }
        .combine(searchQuery) { members, query ->
            if (query.isBlank()) {
                members
            } else {
                members.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.contact.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allPayments: StateFlow<List<Payment>> = userUidFlow
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                firestore.collectionGroup("payments")
                    .whereEqualTo("userId", userId)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Payment::class.java) }
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allPlans: StateFlow<List<Plan>> = userUidFlow
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                firestore.collection("users").document(userId).collection("plans")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Plan::class.java) }
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMembers: StateFlow<List<Member>> =
        allMembers.map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            members
                .filter { it.expiryDate >= todayStart }
                .sortedBy { it.name }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val membersExpiringSoon: StateFlow<List<Member>> =
        allMembers.map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            val sevenDaysLaterEnd = DateUtils.endOfDayMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))
            members
                .filter { it.expiryDate in todayStart..sevenDaysLaterEnd }
                .sortedBy { it.expiryDate }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val expiredMembers: StateFlow<List<Member>> =
        allMembers.map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            members.filter { it.expiryDate < todayStart }.sortedByDescending { it.expiryDate }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysRevenue: StateFlow<Double> =
        allPayments.map { payments ->
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            payments
                .filter { (it.transactionDate?.time ?: 0L) in todayStart..todayEnd }
                .sumOf { it.amount }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0.0)

    val thisMonthsCollection: StateFlow<Double> =
        allPayments.map { payments ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = DateUtils.startOfDayMillis(calendar.timeInMillis)
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endOfMonth = DateUtils.endOfDayMillis(calendar.timeInMillis)

            payments
                .filter { (it.transactionDate?.time ?: 0L) in startOfMonth..endOfMonth }
                .sumOf { it.amount }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0.0)

    val todaysTransactions: StateFlow<List<Payment>> =
        allPayments.map { payments ->
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            payments
                .filter { (it.transactionDate?.time ?: 0L) in todayStart..todayEnd }
                .sortedByDescending { it.transactionDate }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysTransactionsWithDetails: StateFlow<List<UiTransaction>> =
        combine(todaysTransactions, allMembers) { transactions, members ->
            // Create a quick lookup map of memberId to photoUri
            val memberPhotoMap = members.associateBy({ it.idString }, { it.photoUri })

            // Map each transaction to our new UI-specific data class
            transactions.map { payment ->
                UiTransaction(
                    paymentDetails = payment,
                    memberPhotoUri = memberPhotoMap[payment.memberId] // Find the photoUri using the memberId
                )
            }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    val membersWithDues: StateFlow<List<Member>> =
        allMembers.map { members -> members.filter { (it.dueAdvance ?: 0.0) < 0.0 } }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDues: StateFlow<Double> = membersWithDues.map { members -> members.sumOf { it.dueAdvance ?: 0.0 } }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0.0)

    val netDuesAdvance: StateFlow<Double> =
        allMembers.map { members -> members.sumOf { it.dueAdvance ?: 0.0 } }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySignups: StateFlow<List<MonthlySignupData>> =
        allMembers.map { members ->
            val groupAndSortFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val displayFormat = SimpleDateFormat("MMM", Locale.getDefault())
            val signupsByMonth = members
                .filter { it.startDate > 0L }
                .groupBy { member -> groupAndSortFormat.format(Date(member.startDate)) }

            val last12Months = (11 downTo 0).map { monthOffset ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -monthOffset)
                groupAndSortFormat.format(cal.time)
            }

            last12Months.map { monthKey ->
                val date = groupAndSortFormat.parse(monthKey) ?: Date()
                val count = signupsByMonth[monthKey]?.size?.toFloat() ?: 0f
                MonthlySignupData(displayFormat.format(date), count)
            }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val planPopularity: StateFlow<List<PlanPopularityData>> =
        allMembers.map { members ->
            members.groupBy { member -> member.plan.trim().removeSuffix("s").trim().ifBlank { "Unknown" } }
                .map { (plan, memberList) -> PlanPopularityData(plan, memberList.size.toFloat()) }
                .sortedByDescending { it.count }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<Payment>> = combine(allPayments, _collectionDateFilter, _customDateRange) { payments, filter, customRange ->
        val tz = TimeZone.getDefault()
        val calendar = Calendar.getInstance(tz)

        val (startDate, endDate) = when (filter) {
            "Today" -> {
                val start = DateUtils.startOfDayMillis(calendar.timeInMillis)
                val end = DateUtils.endOfDayMillis(calendar.timeInMillis)
                Pair(start, end)
            }
            "Yesterday" -> {
                calendar.add(Calendar.DATE, -1)
                val start = DateUtils.startOfDayMillis(calendar.timeInMillis)
                val end = DateUtils.endOfDayMillis(calendar.timeInMillis)
                Pair(start, end)
            }
            "This Week" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val start = DateUtils.startOfDayMillis(calendar.timeInMillis)
                calendar.add(Calendar.DATE, 6)
                val end = DateUtils.endOfDayMillis(calendar.timeInMillis)
                Pair(start, end)
            }
            "Last Week" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val start = DateUtils.startOfDayMillis(calendar.timeInMillis)
                calendar.add(Calendar.DATE, 6)
                val end = DateUtils.endOfDayMillis(calendar.timeInMillis)
                Pair(start, end)
            }
            "This Month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = DateUtils.startOfDayMillis(calendar.timeInMillis)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = DateUtils.endOfDayMillis(calendar.timeInMillis)
                Pair(start, end)
            }
            "Last Month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = DateUtils.startOfDayMillis(calendar.timeInMillis)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = DateUtils.endOfDayMillis(calendar.timeInMillis)
                Pair(start, end)
            }
            "Custom" -> Pair(customRange.first, customRange.second)
            else -> Pair(null, null)
        }

        if (startDate != null && endDate != null) {
            payments.filter { (it.transactionDate?.time ?: 0L) in startDate..endDate }
        } else {
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            payments.filter { (it.transactionDate?.time ?: 0L) in todayStart..todayEnd }
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactionsWithDetails: StateFlow<List<UiTransaction>> =
        combine(filteredTransactions, allMembers) { transactions, members ->
            val memberPhotoMap = members.associateBy({ it.idString }, { it.photoUri })

            transactions.map { payment ->
                UiTransaction(
                    paymentDetails = payment,
                    memberPhotoUri = memberPhotoMap[payment.memberId]
                )
            }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun addOrUpdateMember(member: Member, photoUri: Uri?, context: Context, isRenewal: Boolean, amountReceived: Double) = viewModelScope.launch {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiEvents.emit(UiEvent.ShowError("Not signed in"))
            return@launch
        }

        _isLoading.value = true
        try {
            var memberToSave = member.copy(userId = userId)
            var memberId = member.idString

            photoUri?.let { uri ->
                // --- THIS IS THE FIX ---
                // Only upload if the URI is a new local file from the gallery or camera.
                // Existing photos are https:// URLs and should not be re-uploaded.
                if (uri.scheme == "content" || uri.scheme == "file") {
                    try {
                        val storagePath = "images/$userId/${System.currentTimeMillis()}_photo.jpg"
                        val downloadUrl = compressAndUploadImage(context, uri, storagePath)
                        memberToSave = memberToSave.copy(photoUri = downloadUrl)
                    } catch (e: Exception) {
                        Log.e("MainVM", "Photo upload/compression failed", e)
                        _uiEvents.emit(UiEvent.ShowError("Photo upload failed. Please try again."))
                        return@launch
                    }
                }
                // If the scheme is 'https', we do nothing and the existing URL in 'memberToSave' is preserved.
            }

            if (memberId.isBlank()) {
                val newMemberRef = firestore.collection("users").document(userId).collection("members").add(memberToSave).await()
                memberId = newMemberRef.id
            } else {
                firestore.collection("users").document(userId).collection("members").document(memberId).set(memberToSave).await()
            }

            val isNewMember = member.idString.isBlank()
            if (amountReceived > 0 && (isNewMember || isRenewal)) {
                val paymentRecord = Payment(
                    amount = amountReceived,
                    memberName = memberToSave.name,
                    type = if (isRenewal) "Renewal" else "Membership",
                    userId = userId,
                    memberId = memberId
                )
                firestore.collection("users").document(userId)
                    .collection("members").document(memberId)
                    .collection("payments").add(paymentRecord).await()
            }

            if (member.idString.isBlank() || isRenewal) {
                val historyRecord = MembershipHistory(
                    plan = memberToSave.plan,
                    startDate = memberToSave.startDate,
                    expiryDate = memberToSave.expiryDate,
                    finalAmount = memberToSave.finalAmount,
                    userId = userId
                )
                firestore.collection("users").document(userId)
                    .collection("members").document(memberId)
                    .collection("history").add(historyRecord).await()
            }

            val shouldSendMessage = member.idString.isBlank() || isRenewal
            val finalMember = memberToSave.copy(idString = memberId)
            _uiEvents.emit(UiEvent.SaveSuccess(finalMember, shouldSendMessage))

        } catch (e: Exception) {
            Log.e("MainVM", "add/update member failed", e)
            _uiEvents.emit(UiEvent.ShowError("Save failed: ${e.message}"))
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteMember(member: Member) = viewModelScope.launch {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiEvents.emit(UiEvent.ShowError("Not signed in"))
            return@launch
        }

        if (member.idString.isBlank()) {
            _uiEvents.emit(UiEvent.ShowError("Invalid member id"))
            return@launch
        }

        _isLoading.value = true
        try {
            val memberRef = firestore.collection("users").document(userId)
                .collection("members").document(member.idString)

            val paymentsSnap = memberRef.collection("payments").get().await()
            if (!paymentsSnap.isEmpty) {
                val paymentRefs = paymentsSnap.documents.map { it.reference }
                chunkedBatchDelete(paymentRefs)
            }

            val historySnap = memberRef.collection("history").get().await()
            if (!historySnap.isEmpty) {
                val historyRefs = historySnap.documents.map { it.reference }
                chunkedBatchDelete(historyRefs)
            }

            member.photoUri?.takeIf { it.isNotBlank() }?.let { url ->
                try {
                    storage.getReferenceFromUrl(url).delete().await()
                } catch (e: Exception) {
                    Log.w("MainVM", "Failed to delete member photo: $url", e)
                }
            }

            memberRef.delete().await()

            _uiEvents.emit(UiEvent.ShowToast("Member and related data deleted"))
        } catch (e: Exception) {
            Log.e("MainVM", "Error deleting member and subcollections", e)
            _uiEvents.emit(UiEvent.ShowError("Delete failed: ${e.message}"))
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteAllMembers(context: Context) = viewModelScope.launch {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiEvents.emit(UiEvent.ShowError("Not signed in"))
            return@launch
        }

        _isLoading.value = true
        try {
            val membersSnap = firestore.collection("users").document(userId).collection("members").get().await()
            if (membersSnap.isEmpty) {
                _uiEvents.emit(UiEvent.ShowToast("No members to delete"))
            } else {
                val docs = membersSnap.documents
                _deleteTotal.value = docs.size
                _deleteProgress.value = 0

                val deleteJobs = docs.map { doc ->
                    async {
                        val m = doc.toObject(Member::class.java)
                        m?.photoUri?.takeIf { it.isNotBlank() }?.let { url ->
                            try {
                                storage.getReferenceFromUrl(url).delete().await()
                            } catch (e: Exception) {
                                Log.w("MainVM", "Failed to delete photo for member ${m?.name}", e)
                            }
                        }
                        deleteSubCollection(doc.reference.collection("history"))
                        deleteSubCollection(doc.reference.collection("payments"))
                        doc.reference.delete().await()

                        withContext(Dispatchers.Main) {
                            _deleteProgress.value++
                        }
                    }
                }
                deleteJobs.awaitAll()
                _uiEvents.emit(UiEvent.ShowToast("All members deleted"))
            }
        } catch (e: Exception) {
            Log.e("MainVM", "Failed to delete all members and data", e)
            _uiEvents.emit(UiEvent.ShowError("Failed to delete all members: ${e.message}"))
        } finally {
            _isLoading.value = false
            _deleteTotal.value = 0
            _deleteProgress.value = 0
        }
    }

    fun updateDueAdvance(member: Member, amountPaid: Double) = viewModelScope.launch {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiEvents.emit(UiEvent.ShowError("Not signed in"))
            return@launch
        }

        _isLoading.value = true
        try {
            if (member.idString.isNotBlank()) {
                val newBalance = (member.dueAdvance ?: 0.0) + amountPaid
                firestore.collection("users").document(userId).collection("members").document(member.idString)
                    .update("dueAdvance", newBalance).await()

                if (amountPaid > 0) {
                    val paymentRecord = Payment(
                        amount = amountPaid,
                        memberName = member.name,
                        type = "Due Clearance",
                        userId = userId,
                        memberId = member.idString // --- FIXED: Save the memberId with the payment ---
                    )
                    firestore.collection("users").document(userId)
                        .collection("members").document(member.idString)
                        .collection("payments").add(paymentRecord).await()
                }

                _uiEvents.emit(UiEvent.ShowToast("Updated successfully"))
            }
        } catch (e: Exception) {
            _errorMessage.value = "Update failed: ${e.message}"
            _uiEvents.emit(UiEvent.ShowError("Update failed: ${e.message}"))
        } finally {
            _isLoading.value = false
        }
    }

    fun savePlanPrice(plan: Plan) = viewModelScope.launch {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiEvents.emit(UiEvent.ShowError("Not signed in"))
            return@launch
        }

        _isLoading.value = true
        try {
            firestore.collection("users").document(userId).collection("plans").document(plan.planName).set(plan).await()
            _uiEvents.emit(UiEvent.ShowToast("Plan saved"))
        } catch (e: Exception) {
            _errorMessage.value = "Save plan failed: ${e.message}"
            _uiEvents.emit(UiEvent.ShowError("Save plan failed: ${e.message}"))
        } finally {
            _isLoading.value = false
        }
    }

    // Add this StateFlow to hold the payments for a single member
    private val _memberPayments = MutableStateFlow<List<Payment>>(emptyList())
    val memberPayments: StateFlow<List<Payment>> = _memberPayments.asStateFlow()

    fun fetchPaymentsForMember(memberId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            if (memberId.isBlank()) {
                _memberPayments.value = emptyList()
                return@launch
            }
            try {
                // Listen for real-time updates
                firestore.collection("users").document(userId)
                    .collection("members").document(memberId)
                    .collection("payments")
                    .orderBy("transactionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .snapshots() // Use snapshots() to get real-time updates
                    .collect { snapshot ->
                        _memberPayments.value = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Payment::class.java)?.copy(paymentId = doc.id)
                        }
                    }
            } catch (e: Exception) {
                Log.e("MainVM", "Failed to fetch member payments", e)
                _uiEvents.emit(UiEvent.ShowError("Could not load payments"))
            }
        }
    }

    fun deletePayment(paymentId: String, memberId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null || paymentId.isBlank() || memberId.isBlank()) {
                _uiEvents.emit(UiEvent.ShowError("Invalid data for deletion."))
                return@launch
            }

            _isLoading.value = true
            try {
                firestore.collection("users").document(userId)
                    .collection("members").document(memberId)
                    .collection("payments").document(paymentId)
                    .delete()
                    .await()
                _uiEvents.emit(UiEvent.ShowToast("Payment deleted successfully"))
            } catch (e: Exception) {
                Log.e("MainVM", "Error deleting payment", e)
                _uiEvents.emit(UiEvent.ShowError("Delete failed: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onCollectionDateFilterChange(filter: String, startDate: Long?, endDate: Long?) {
        _collectionDateFilter.value = filter
        if (filter == "Custom") {
            _customDateRange.value = Pair(startDate, endDate)
        }
    }

    fun exportMembersToCsv(context: Context, uri: Uri) = viewModelScope.launch {
        _isLoading.value = true
        try {
            CsvUtils.writeMembersToCsv(context, uri, allMembers.value)
            _uiEvents.emit(UiEvent.ShowToast("Export completed: ${allMembers.value.size} members"))
        } catch (e: Exception) {
            Log.e("MainVM", "Export failed", e)
            _uiEvents.emit(UiEvent.ShowError("Export failed: ${e.message}"))
        } finally {
            _isLoading.value = false
        }
    }

    fun importMembersFromCsv(context: Context, uri: Uri) = viewModelScope.launch {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiEvents.emit(UiEvent.ShowError("Not signed in"))
            return@launch
        }

        _isLoading.value = true
        Log.d("MainVM_Import", "Starting CSV import process.")
        try {
            val members = CsvUtils.readMembersFromCsv(context, uri)
            Log.d("MainVM_Import", "Successfully read ${members.size} members from CSV file.")

            if (members.isEmpty()) {
                _uiEvents.emit(UiEvent.ShowError("CSV file is empty or format is incorrect."))
                _isLoading.value = false
                return@launch
            }

            importMembersBatch(userId, members)
            _uiEvents.emit(UiEvent.ShowToast("Imported ${members.size} members successfully!"))
        } catch (e: Exception) {
            Log.e("MainVM_Import", "CSV import failed during read or processing.", e)
            _uiEvents.emit(UiEvent.ShowError("Import failed. Check logs."))
        } finally {
            _isLoading.value = false
            Log.d("MainVM_Import", "CSV import process finished.")
        }
    }

    fun fetchMemberHistory(memberId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            if (memberId.isBlank()) {
                _memberHistory.value = emptyList()
                return@launch
            }
            try {
                val snapshot = firestore.collection("users").document(userId)
                    .collection("members").document(memberId)
                    .collection("history")
                    .orderBy("transactionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()

                _memberHistory.value = snapshot.toObjects(MembershipHistory::class.java)
            } catch (e: Exception) {
                Log.e("MainVM", "Failed to fetch member history", e)
                _uiEvents.emit(UiEvent.ShowError("Could not load history"))
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun cleanUpOrphanedData() = viewModelScope.launch {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiEvents.emit(UiEvent.ShowError("Not signed in"))
            return@launch
        }
        _isLoading.value = true
        try {
            val membersSnapshot = firestore.collection("users").document(userId).collection("members").get().await()
            val validMemberIds = membersSnapshot.documents.map { it.id }.toSet()

            var orphanedCount = 0

            val paymentsSnapshot = firestore.collectionGroup("payments").whereEqualTo("userId", userId).get().await()
            val paymentsToDelete = paymentsSnapshot.documents.filter {
                val memberId = it.reference.parent.parent?.id
                memberId !in validMemberIds
            }
            if (paymentsToDelete.isNotEmpty()) {
                val batch = firestore.batch()
                paymentsToDelete.forEach { batch.delete(it.reference) }
                batch.commit().await()
                orphanedCount += paymentsToDelete.size
            }

            val historySnapshot = firestore.collectionGroup("history").whereEqualTo("userId", userId).get().await()
            val historyToDelete = historySnapshot.documents.filter {
                val memberId = it.reference.parent.parent?.id
                memberId !in validMemberIds
            }
            if (historyToDelete.isNotEmpty()) {
                val batch = firestore.batch()
                historyToDelete.forEach { batch.delete(it.reference) }
                batch.commit().await()
                orphanedCount += historyToDelete.size
            }

            if (orphanedCount > 0) {
                _uiEvents.emit(UiEvent.ShowToast("Cleaned up $orphanedCount old records."))
            } else {
                _uiEvents.emit(UiEvent.ShowToast("No old data found to clean up."))
            }

        } catch (e: Exception) {
            Log.e("MainVM", "Failed to clean up orphaned data", e)
            _uiEvents.emit(UiEvent.ShowError("Cleanup failed: ${e.message}"))
        } finally {
            _isLoading.value = false
        }
    }

    fun migrateLegacyPayments() = viewModelScope.launch {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiEvents.emit(UiEvent.ShowError("Not signed in"))
            return@launch
        }
        _isLoading.value = true
        try {
            val membersSnapshot = firestore.collection("users").document(userId).collection("members").get().await()
            val calendar2025 = Calendar.getInstance().apply { set(Calendar.YEAR, 2025) }
            val year2025 = calendar2025.get(Calendar.YEAR)

            val membersToMigrate = membersSnapshot.documents.mapNotNull { doc ->
                val member = doc.toObject(Member::class.java)?.copy(idString = doc.id)
                val memberCalendar = Calendar.getInstance().apply { timeInMillis = member?.startDate ?: 0L }
                if (member != null && memberCalendar.get(Calendar.YEAR) == year2025) {
                    member
                } else {
                    null
                }
            }

            if (membersToMigrate.isEmpty()) {
                _uiEvents.emit(UiEvent.ShowToast("No relevant member data from 2025 to migrate."))
                return@launch
            }

            var migratedCount = 0
            val batch = firestore.batch()
            for (member in membersToMigrate) {
                val paymentsExist = member.idString.let {
                    firestore.collection("users").document(userId).collection("members").document(it)
                        .collection("payments").limit(1).get().await().isEmpty.not()
                }

                if (!paymentsExist && (member.finalAmount ?: 0.0) > 0) {
                    val transactionDate = if ((member.purchaseDate ?: 0L) > 0L && Calendar.getInstance().apply { timeInMillis = member.purchaseDate!! }.get(Calendar.YEAR) != 1970) {
                        Date(member.purchaseDate!!)
                    } else {
                        Date(member.startDate)
                    }

                    val paymentRecord = Payment(
                        amount = member.finalAmount ?: 0.0,
                        memberName = member.name,
                        type = "Membership (Migrated)",
                        userId = userId,
                        transactionDate = transactionDate,
                        memberId = member.idString
                    )
                    val paymentRef = firestore.collection("users").document(userId)
                        .collection("members").document(member.idString)
                        .collection("payments").document()

                    batch.set(paymentRef, paymentRecord)
                    migratedCount++
                }
            }

            if (migratedCount > 0) {
                batch.commit().await()
                _uiEvents.emit(UiEvent.ShowToast("Successfully migrated $migratedCount payment records."))
            } else {
                _uiEvents.emit(UiEvent.ShowToast("All payment records are already up to date."))
            }

        } catch (e: Exception) {
            Log.e("MainVM", "Failed to migrate legacy payments", e)
            _uiEvents.emit(UiEvent.ShowError("Migration failed: ${e.message}"))
        } finally {
            _isLoading.value = false
        }
    }

    /* ---------------------- Helpers ---------------------- */

    private suspend fun compressAndUploadImage(
        context: Context,
        uri: Uri,
        storagePath: String
    ): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val compressedBytes = resolver.openInputStream(uri)?.use { inputStream ->
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
                ?: throw Exception("Failed to decode bitmap from URI: $uri")

            val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: ExifInterface.ORIENTATION_NORMAL
            val rotatedBitmap = applyExifTransform(originalBitmap, orientation)

            val outputStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            originalBitmap.recycle()
            outputStream.toByteArray()
        } ?: throw Exception("Unable to open image stream")

        val ref = storage.reference.child(storagePath)
        val uploadResult = ref.putBytes(compressedBytes).await()
        return@withContext uploadResult.storage.downloadUrl.await().toString()
    }

    private fun applyExifTransform(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(-90f); matrix.preScale(-1f, 1f) }
        }
        return if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private suspend fun chunkedBatchDelete(docRefs: List<DocumentReference>) {
        docRefs.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    private suspend fun deleteSubCollection(collection: com.google.firebase.firestore.CollectionReference) {
        val snapshot = collection.limit(500).get().await()
        if (snapshot.isEmpty) return
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        if (snapshot.size() == 500) {
            deleteSubCollection(collection)
        }
    }

    private suspend fun importMembersBatch(userId: String, members: List<Member>) = withContext(Dispatchers.IO) {
        members.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { m ->
                val docRef = if (m.idString.isBlank()) firestore.collection("users").document(userId).collection("members").document() else firestore.collection("users").document(userId).collection("members").document(m.idString)
                batch.set(docRef, m)
            }
            batch.commit().await()
        }
    }
}

class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
