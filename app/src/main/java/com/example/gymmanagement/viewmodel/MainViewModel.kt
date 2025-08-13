package com.example.gymmanagement.viewmodel

import androidx.lifecycle.*
import com.example.gymmanagement.data.database.CheckIn
import com.example.gymmanagement.data.database.CheckInDao
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.MemberDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    private val memberDao: MemberDao,
    private val checkInDao: CheckInDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val allMembers: StateFlow<List<Member>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                memberDao.getAllMembers()
            } else {
                memberDao.getAllMembers().map { members ->
                    members.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val membersExpiringSoon: StateFlow<List<Member>> = flow {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sevenDaysLater = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
        }.timeInMillis

        emitAll(memberDao.getMembersExpiringSoon(today, sevenDaysLater))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun getMemberById(id: Int): Flow<Member?> = memberDao.getMemberById(id)

    fun getCheckInsForMember(memberId: Int): Flow<List<CheckIn>> = checkInDao.getCheckInsForMember(memberId)

    fun addOrUpdateMember(member: Member) = viewModelScope.launch {
        memberDao.upsertMember(member)
    }

    fun deleteMember(member: Member) = viewModelScope.launch {
        memberDao.deleteMember(member)
    }

    fun recordCheckIn(memberId: Int) = viewModelScope.launch {
        checkInDao.insertCheckIn(CheckIn(memberId = memberId, timestamp = System.currentTimeMillis()))
    }
}

class MainViewModelFactory(
    private val memberDao: MemberDao,
    private val checkInDao: CheckInDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(memberDao, checkInDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}