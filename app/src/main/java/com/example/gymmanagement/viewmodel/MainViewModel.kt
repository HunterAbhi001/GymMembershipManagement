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

    private val _collectionDateFilter = MutableStateFlow("Today")
    private val _customDateRange = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null))

    val allMembers: StateFlow<List<Member>> = searchQuery.flatMapLatest { query -> if (query.isBlank()) { memberDao.getAllMembers() } else { memberDao.getAllMembers().map { members -> members.filter { it.name.contains(query, ignoreCase = true) || it.contact.contains(query, ignoreCase = true) } } } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeMembers: StateFlow<List<Member>> = allMembers.map { members -> val todayStart = DateUtils.startOfDayMillis(); members.filter { it.expiryDate >= todayStart } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val membersExpiringSoon: StateFlow<List<Member>> = flow { val todayStart = DateUtils.startOfDayMillis(); val sevenDaysLaterEnd = DateUtils.endOfDayMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)); emitAll(memberDao.getMembersExpiringSoon(todayStart, sevenDaysLaterEnd)) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expiredMembers: StateFlow<List<Member>> = allMembers.map { members -> val todayStart = DateUtils.startOfDayMillis(); members.filter { it.expiryDate < todayStart }.sortedByDescending { it.expiryDate } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val todaysRevenue: StateFlow<Double> = allMembers.map { members -> val todayStart = DateUtils.startOfDayMillis(); val todayEnd = DateUtils.endOfDayMillis(); members.filter { (it.purchaseDate ?: 0L) in todayStart..todayEnd }.sumOf { it.finalAmount ?: 0.0 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val todaysRevenueMembers: StateFlow<List<Member>> = allMembers.map { members -> val todayStart = DateUtils.startOfDayMillis(); val todayEnd = DateUtils.endOfDayMillis(); members.filter { (it.purchaseDate ?: 0L) in todayStart..todayEnd } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalBalance: StateFlow<Double> = allMembers.map { members -> members.sumOf { it.finalAmount ?: 0.0 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val membersWithDues: StateFlow<List<Member>> = allMembers.map { members -> members.filter { (it.dueAdvance ?: 0.0) < 0.0 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalDues: StateFlow<Double> = membersWithDues.map { members -> members.sumOf { it.dueAdvance ?: 0.0 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val netDuesAdvance: StateFlow<Double> = allMembers.map { members -> members.sumOf { it.dueAdvance ?: 0.0 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val monthlySignups: StateFlow<List<MonthlySignupData>> = allMembers.map { members -> val groupAndSortFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault()); val displayFormat = SimpleDateFormat("MMM", Locale.getDefault()); val signupsByMonth = members.groupBy { member -> groupAndSortFormat.format(Date(member.startDate)) }; val last12Months = (0 downTo -11).map { monthOffset -> val cal = Calendar.getInstance(); cal.add(Calendar.MONTH, monthOffset); groupAndSortFormat.format(cal.time) }; last12Months.reversed().map { monthKey -> val date = groupAndSortFormat.parse(monthKey) ?: Date(); val count = signupsByMonth[monthKey]?.size?.toFloat() ?: 0f; MonthlySignupData(displayFormat.format(date), count) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val planPopularity: StateFlow<List<PlanPopularityData>> = allMembers.map { members -> members.groupBy { member -> member.plan.trim().removeSuffix("s").trim() }.map { (plan, memberList) -> PlanPopularityData(plan, memberList.size.toFloat()) }.sortedByDescending { it.count } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCollection: StateFlow<List<Member>> = combine(allMembers, _collectionDateFilter, _customDateRange) { members, filter, customRange ->
        val calendar = Calendar.getInstance()
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
            members.filter { (it.purchaseDate ?: 0L) in startDate..endDate }
        } else {
            val todayStart = DateUtils.startOfDayMillis()
            val todayEnd = DateUtils.endOfDayMillis()
            members.filter { (it.purchaseDate ?: 0L) in todayStart..todayEnd }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun getMemberById(id: Int): Flow<Member?> = memberDao.getMemberById(id)
    fun addOrUpdateMember(member: Member) = viewModelScope.launch { memberDao.upsertMember(member) }
    fun deleteMember(member: Member) = viewModelScope.launch { memberDao.deleteMember(member) }
    fun updateDueAdvance(member: Member, amountPaid: Double) = viewModelScope.launch { val currentBalance = member.dueAdvance ?: 0.0; val newBalance = currentBalance + amountPaid; val updatedMember = member.copy(dueAdvance = newBalance); memberDao.upsertMember(updatedMember) }

    fun onCollectionDateFilterChange(filter: String, startDate: Long?, endDate: Long?) {
        _collectionDateFilter.value = filter
        if (filter == "Custom") {
            _customDateRange.value = Pair(startDate, endDate)
        }
    }

    fun importMembersFromCsv(context: Context, uri: Uri) = viewModelScope.launch { /* ... */ }
    fun exportMembersToCsv(context: Context, uri: Uri) = viewModelScope.launch { /* ... */ }
}

class MainViewModelFactory(private val memberDao: MemberDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(memberDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
