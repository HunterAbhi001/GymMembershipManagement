package com.example.gymmanagement.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.*
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.MemberDao
import com.example.gymmanagement.ui.screens.MonthlySignupData
import com.example.gymmanagement.ui.screens.PlanPopularityData
import com.example.gymmanagement.ui.utils.CsvUtils
import com.example.gymmanagement.ui.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel(
    private val memberDao: MemberDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val allMembers: StateFlow<List<Member>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                memberDao.getAllMembers()
            } else {
                memberDao.getAllMembers().map { members ->
                    members.filter { it.name.contains(query, ignoreCase = true) || it.contact.contains(query, ignoreCase = true) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMembers: StateFlow<List<Member>> = allMembers
        .map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            members.filter { it.expiryDate >= todayStart }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val membersExpiringSoon: StateFlow<List<Member>> = flow {
        val todayStart = DateUtils.startOfDayMillis()
        val sevenDaysLaterEnd = DateUtils.endOfDayMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))
        emitAll(memberDao.getMembersExpiringSoon(todayStart, sevenDaysLaterEnd))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiredMembers: StateFlow<List<Member>> = allMembers
        .map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            members
                .filter { it.expiryDate < todayStart }
                .sortedByDescending { it.expiryDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysRevenue: StateFlow<Double> = allMembers
        .map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            members
                .filter { it.purchaseDate in todayStart..todayEnd }
                .sumOf { it.finalAmount ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todaysRevenueMembers: StateFlow<List<Member>> = allMembers
        .map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            members.filter { it.purchaseDate in todayStart..todayEnd }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = allMembers
        .map { members ->
            members.sumOf { it.finalAmount ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val duesAndAdvanceMembers: StateFlow<List<Member>> = allMembers
        .map { members ->
            members.filter { (it.dueAdvance ?: 0.0) != 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDues: StateFlow<Double> = allMembers
        .map { members ->
            members
                .filter { (it.dueAdvance ?: 0.0) < 0 }
                .sumOf { it.dueAdvance ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netDuesAdvance: StateFlow<Double> = allMembers
        .map { members ->
            members.sumOf { it.dueAdvance ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySignups: StateFlow<List<MonthlySignupData>> = allMembers
        .map { members ->
            val groupAndSortFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            // --- FIX: Changed format to "MMM" for a short, clean label (e.g., "Feb") ---
            val displayFormat = SimpleDateFormat("MMM", Locale.getDefault())
            val calendar = Calendar.getInstance()

            val signupsByMonth = members
                .groupBy { member -> groupAndSortFormat.format(Date(member.startDate)) }

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
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val planPopularity: StateFlow<List<PlanPopularityData>> = allMembers
        .map { members ->
            members
                .groupBy { member ->
                    member.plan.trim().removeSuffix("s").trim()
                }
                .map { (plan, memberList) -> PlanPopularityData(plan, memberList.size.toFloat()) }
                .sortedByDescending { it.count }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun getMemberById(id: Int): Flow<Member?> = memberDao.getMemberById(id)

    fun addOrUpdateMember(member: Member) = viewModelScope.launch {
        memberDao.upsertMember(member)
    }

    fun deleteMember(member: Member) = viewModelScope.launch {
        memberDao.deleteMember(member)
    }

    fun clearDueAdvance(member: Member) = viewModelScope.launch {
        val updatedMember = member.copy(dueAdvance = 0.0)
        memberDao.upsertMember(updatedMember)
    }

    fun importMembersFromCsv(context: Context, uri: Uri) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val result = CsvUtils.readMembersFromCsv(context, uri)
                val members = result.first
                val failures = result.second
                if (members.isNotEmpty()) {
                    memberDao.insertAll(members)
                }
                withContext(Dispatchers.Main) {
                    val successMessage = if (members.isNotEmpty()) "${members.size} members imported." else "No new members imported."
                    val failureMessage = if (failures > 0) " $failures rows failed due to formatting issues." else ""
                    Toast.makeText(context, successMessage + failureMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error importing file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun exportMembersToCsv(context: Context, uri: Uri) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val members = allMembers.first()
                CsvUtils.writeMembersToCsv(context, uri, members)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Exported successfully to Documents folder.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error exporting file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

class MainViewModelFactory(
    private val memberDao: MemberDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(memberDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
