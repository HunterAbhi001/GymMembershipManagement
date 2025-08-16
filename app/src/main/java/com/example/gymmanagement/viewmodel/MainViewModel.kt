package com.example.gymmanagement.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.*
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.MemberDao
import com.example.gymmanagement.ui.utils.CsvUtils
import com.example.gymmanagement.ui.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainViewModel(
    private val memberDao: MemberDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    /**
     * allMembers returns the list optionally filtered by the search query.
     * Filtering is applied here for the app-wide "all members" usage, but
     * the All Members screen may also do local filtering if desired.
     */
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

    /**
     * Active members are those whose expiryDate is >= start of today.
     */
    val activeMembers: StateFlow<List<Member>> = allMembers
        .map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            members.filter { it.expiryDate >= todayStart }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Members expiring soon: query DAO for the range [todayStart .. sevenDaysLaterEnd]
     */
    val membersExpiringSoon: StateFlow<List<Member>> = flow {
        val todayStart = DateUtils.startOfDayMillis()
        val sevenDaysLaterEnd = DateUtils.endOfDayMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))
        emitAll(memberDao.getMembersExpiringSoon(todayStart, sevenDaysLaterEnd))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Expired members are those whose expiryDate is strictly before today's start.
     */
    val expiredMembers: StateFlow<List<Member>> = allMembers
        .map { members ->
            val todayStart = DateUtils.startOfDayMillis()
            members
                .filter { it.expiryDate < todayStart }
                .sortedByDescending { it.expiryDate } // most recently expired first
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
                val members = allMembers.first() // Get current list of members
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
