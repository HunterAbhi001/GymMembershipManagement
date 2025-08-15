package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.common.MemberListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveMembersListScreen(
    navController: NavController,
    members: List<Member>
) {
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(members) { member ->
                    MemberListItem(
                        member = member,
                        onClick = { navController.navigate("member_details/${member.id}") }
                    )
                }
            }
        }
    }
}