package com.example.gymmanagement.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.*
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.MemberDao
import com.example.gymmanagement.ui.utils.CsvUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

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
                    members.filter { it.name.contains(query, ignoreCase = true) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMembers: StateFlow<List<Member>> = allMembers
        .map { members ->
            members.filter { it.expiryDate >= System.currentTimeMillis() }
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

    val expiredMembers: StateFlow<List<Member>> = allMembers
        .map { members ->
            members
                .filter { it.expiryDate < System.currentTimeMillis() }
                .sortedByDescending { it.expiryDate } // Sort by most recently expired
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