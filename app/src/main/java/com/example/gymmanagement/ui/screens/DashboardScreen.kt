package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.common.MemberListItem
import com.example.gymmanagement.ui.theme.Green
import com.example.gymmanagement.ui.theme.Orange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    activeMemberCount: Int,
    membersExpiringSoon: List<Member>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gym Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { navController.navigate("add_edit_member") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Stats Cards
            Row(Modifier.fillMaxWidth()) {
                StatCard(
                    label = "Active Members",
                    value = activeMemberCount.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("members_list") }
                )
                Spacer(modifier = Modifier.width(16.dp))
                StatCard(
                    label = "Expiring Soon",
                    value = membersExpiringSoon.size.toString(),
                    modifier = Modifier.weight(1f),
                    color = Orange,
                    onClick = { navController.navigate("expiring_members") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Expiring Soon List
            Text("Memberships Expiring Soon", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (membersExpiringSoon.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No memberships are expiring soon.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(membersExpiringSoon) { member ->
                        MemberListItem(member = member, onClick = {
                            navController.navigate("member_details/${member.id}")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color = Green, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}