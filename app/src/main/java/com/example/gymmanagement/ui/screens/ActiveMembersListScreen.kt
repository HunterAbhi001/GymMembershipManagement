package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.common.MemberListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveMembersListScreen(
    navController: NavController,
    members: List<Member>
) {
    // --- State for the local search query ---
    var searchQuery by remember { mutableStateOf("") }

    // --- Filter the list based on the search query ---
    val filteredMembers = remember(searchQuery, members) {
        if (searchQuery.isBlank()) {
            members
        } else {
            members.filter { member ->
                member.name.contains(searchQuery, ignoreCase = true) ||
                        member.contact.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Members") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // --- Search bar UI ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Active Members") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredMembers) { member ->
                    MemberListItem(
                        member = member,
                        onClick = {
                            // Use idString instead of id
                            navController.navigate("member_details/${member.idString}")
                        }
                    )
                }
            }
        }
    }
}
