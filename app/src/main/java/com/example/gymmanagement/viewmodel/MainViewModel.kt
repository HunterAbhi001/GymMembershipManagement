package com.example.gymmanagement.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.Plan
import com.example.gymmanagement.ui.screens.MonthlySignupData
import com.example.gymmanagement.ui.screens.PlanPopularityData
import com.example.gymmanagement.ui.utils.CsvUtils
import com.example.gymmanagement.ui.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _collectionDateFilter = MutableStateFlow("Today")
    private val _customDateRange = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null))

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deleteProgress = MutableStateFlow(0)
    val deleteProgress: StateFlow<Int> = _deleteProgress.asStateFlow()
    private val _deleteTotal = MutableStateFlow(0)
    val deleteTotal: StateFlow<Int> = _deleteTotal.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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
                members.filter { it.name.contains(query, ignoreCase = true) || it.contact.contains(query, ignoreCase = true) }
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
        allMembers.map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            members.filter { (it.purchaseDate ?: 0L) in todayStart..todayEnd }.sumOf { it.finalAmount ?: 0.0 }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0.0)

    val todaysRevenueMembers: StateFlow<List<Member>> =
        allMembers.map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            members.filter { (it.purchaseDate ?: 0L) in todayStart..todayEnd }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> =
        allMembers.map { members -> members.sumOf { it.finalAmount ?: 0.0 } }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0.0)

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
            val signupsByMonth = members.groupBy { member -> groupAndSortFormat.format(Date(member.startDate)) }
            val last12Months = (0 downTo -11).map { monthOffset ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, monthOffset)
                groupAndSortFormat.format(cal.time)
            }
            last12Months.reversed().map { monthKey ->
                val date = groupAndSortFormat.parse(monthKey) ?: Date()
                val count = signupsByMonth[monthKey]?.size?.toFloat() ?: 0f
                MonthlySignupData(displayFormat.format(date), count)
            }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val planPopularity: StateFlow<List<PlanPopularityData>> =
        allMembers.map { members ->
            members.groupBy { member -> member.plan.trim().removeSuffix("s").trim() }
                .map { (plan, memberList) -> PlanPopularityData(plan, memberList.size.toFloat()) }
                .sortedByDescending { it.count }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCollection: StateFlow<List<Member>> = combine(allMembers, _collectionDateFilter, _customDateRange) { members, filter, customRange ->
        val calendar = Calendar.getInstance()
        val (startDate, endDate) = when (filter) {
            "Today" -> Pair(DateUtils.startOfDayMillis(calendar.timeInMillis), DateUtils.endOfDayMillis(calendar.timeInMillis))
            "Yesterday" -> {
                calendar.add(Calendar.DATE, -1)
                Pair(DateUtils.startOfDayMillis(calendar.timeInMillis), DateUtils.endOfDayMillis(calendar.timeInMillis))
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
            members.filter { (it.purchaseDate ?: 0L) in startDate..endDate }
        } else {
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            members.filter { (it.purchaseDate ?: 0L) in todayStart..todayEnd }
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    // --- UPDATED: This function now includes image compression ---
    fun addOrUpdateMember(member: Member, photoUri: Uri?, context: Context) = viewModelScope.launch {
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: run {
            _isLoading.value = false
            _errorMessage.value = "Not signed in"
            return@launch
        }
        var memberToSave = member.copy(userId = userId)

        photoUri?.let { uri ->
            val photoRef = storage.reference.child("images/$userId/${System.currentTimeMillis()}_photo.jpg")
            try {
                // --- IMAGE COMPRESSION LOGIC START ---
                // Switch to a background thread for image processing
                val compressedImageData = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    val outputStream = ByteArrayOutputStream()
                    // Compress the bitmap to JPEG with 80% quality. You can adjust this value.
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    outputStream.toByteArray()
                }
                // --- IMAGE COMPRESSION LOGIC END ---

                // Upload the compressed byte array instead of the original file
                val downloadUrl = photoRef.putBytes(compressedImageData).await().storage.downloadUrl.await()
                memberToSave = memberToSave.copy(photoUri = downloadUrl.toString())

            } catch (e: Exception) {
                Log.w("MainVM", "Photo upload/compression failed", e)
                Toast.makeText(context, "Failed to upload photo.", Toast.LENGTH_SHORT).show()
            }
        }

        try {
            if (memberToSave.idString.isBlank()) {
                firestore.collection("users").document(userId).collection("members").add(memberToSave).await()
            } else {
                firestore.collection("users").document(userId).collection("members").document(memberToSave.idString).set(memberToSave).await()
            }
        } catch (e: Exception) {
            Log.e("MainVM", "add/update member failed", e)
            _errorMessage.value = "Save failed: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun deleteMember(member: Member) = viewModelScope.launch {
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: return@launch
        if (member.idString.isNotBlank()) {
            try {
                // Also delete the photo from storage if it exists
                member.photoUri?.let { uri ->
                    if (uri.isNotBlank()) {
                        storage.getReferenceFromUrl(uri).delete().await()
                    }
                }
                firestore.collection("users").document(userId).collection("members").document(member.idString).delete().await()
            } catch (e: Exception) {
                _errorMessage.value = "Delete failed: ${e.message}"
                Log.e("MainVM", "Error deleting member or their photo", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAllMembers(context: Context) = viewModelScope.launch {
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
            _isLoading.value = false
            return@launch
        }

        try {
            val snapshot = firestore.collection("users").document(userId).collection("members").get().await()
            if (snapshot.isEmpty) {
                Toast.makeText(context, "No members to delete", Toast.LENGTH_SHORT).show()
            } else {
                _deleteTotal.value = snapshot.size()
                _deleteProgress.value = 0
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    // Delete photo from storage
                    val member = doc.toObject(Member::class.java)
                    member?.photoUri?.let { uri ->
                        if (uri.isNotBlank()) {
                            try {
                                storage.getReferenceFromUrl(uri).delete().await()
                            } catch (e: Exception) {
                                Log.w("MainVM", "Failed to delete photo for member ${member.name}", e)
                            }
                        }
                    }
                    batch.delete(doc.reference)
                    _deleteProgress.value++
                }
                batch.commit().await()
                Toast.makeText(context, "All members deleted", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to delete all members: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            _isLoading.value = false
            _deleteTotal.value = 0
            _deleteProgress.value = 0
        }
    }

    fun updateDueAdvance(member: Member, amountPaid: Double) = viewModelScope.launch {
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: return@launch
        if (member.idString.isNotBlank()) {
            val newBalance = (member.dueAdvance ?: 0.0) + amountPaid
            try {
                firestore.collection("users").document(userId).collection("members").document(member.idString).update("dueAdvance", newBalance).await()
            } catch (e: Exception) {
                _errorMessage.value = "Update failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun savePlanPrice(plan: Plan) = viewModelScope.launch {
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: return@launch
        try {
            firestore.collection("users").document(userId).collection("plans").document(plan.planName).set(plan).await()
        } catch (e: Exception) {
            _errorMessage.value = "Save plan failed: ${e.message}"
        } finally {
            _isLoading.value = false
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
            Toast.makeText(context, "Export completed: ${allMembers.value.size} members", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            _isLoading.value = false
        }
    }

    fun importMembersFromCsv(context: Context, uri: Uri) = viewModelScope.launch {
        _isLoading.value = true
        Log.d("MainVM_Import", "Starting CSV import process.")
        try {
            val members = CsvUtils.readMembersFromCsv(context, uri)
            Log.d("MainVM_Import", "Successfully read ${members.size} members from CSV file.")

            if (members.isEmpty()) {
                Toast.makeText(context, "CSV file is empty or format is incorrect.", Toast.LENGTH_LONG).show()
                _isLoading.value = false
                return@launch
            }

            var importedCount = 0
            members.forEach { member ->
                addOrUpdateMember(member, null, context)
                importedCount++
                Log.d("MainVM_Import", "Attempting to import member ${importedCount}/${members.size}: ${member.name}")
            }
            Log.d("MainVM_Import", "Finished loop for importing members.")
            Toast.makeText(context, "Imported $importedCount members successfully!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("MainVM_Import", "CSV import failed during read or processing.", e)
            Toast.makeText(context, "Import failed. Check Logcat for details using 'MainVM_Import' tag.", Toast.LENGTH_LONG).show()
        } finally {
            _isLoading.value = false
            Log.d("MainVM_Import", "CSV import process finished.")
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
