package com.example.gymmanagement.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.theme.RedAccent
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiringMembersScreen(
    navController: NavController,
    members: List<Member>,
    onDeleteMember: (Member) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    // --- State for the guided reminder flow ---
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderIndex by remember { mutableStateOf(0) }
    val membersExpiringToday = remember(members) {
        members.filter {
            val daysRemaining = TimeUnit.MILLISECONDS.toDays(it.expiryDate - DateUtils.startOfDayMillis())
            daysRemaining == 0L
        }
    }


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

    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${memberToDelete!!.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMember(memberToDelete!!)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Guided Reminder Dialog for members expiring today ---
    if (showReminderDialog) {
        val currentMember = membersExpiringToday.getOrNull(reminderIndex)
        if (currentMember != null) {
            ReminderDialog(
                member = currentMember,
                currentIndex = reminderIndex + 1,
                total = membersExpiringToday.size,
                onDismiss = { showReminderDialog = false },
                onSend = {
                    val firstName = currentMember.name.split(" ").firstOrNull() ?: currentMember.name
                    val message = "Hi $firstName, just a friendly reminder that your gym membership expires today. Thank you!"
                    sendWhatsAppMessage(context, currentMember.contact, message)
                    if (reminderIndex < membersExpiringToday.size - 1) {
                        reminderIndex++
                    } else {
                        showReminderDialog = false
                        Toast.makeText(context, "All reminders sent!", Toast.LENGTH_SHORT).show()
                    }
                },
                onSkip = {
                    if (reminderIndex < membersExpiringToday.size - 1) {
                        reminderIndex++
                    } else {
                        showReminderDialog = false
                        Toast.makeText(context, "Finished reminder session.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expiring Soon") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (membersExpiringToday.isNotEmpty()) {
                            reminderIndex = 0
                            showReminderDialog = true
                        } else {
                            Toast.makeText(context, "No members are expiring today.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Remind members expiring today")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Expiring Members") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMembers) { member ->
                    // --- UPDATED: Using the reusable MemberListItem ---
                    MemberListItem(
                        member = member,
                        onClick = { navController.navigate("member_details/${member.idString}") },
                        trailingContent = {
                            Text(
                                text = member.plan,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Using our new and improved smart text helper
                            ExpiryStatusText(member = member)
                        }
                    )
                }
            }
        }
    }
}

// --- REMOVED: The old ModernExpiringMemberListItem is no longer needed ---

// --- Reusable Reminder Dialog ---
@Composable
private fun ReminderDialog(
    member: Member,
    currentIndex: Int,
    total: Int,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Reminder ($currentIndex/$total)") },
        text = {
            Text("Ready to send a WhatsApp reminder to ${member.name}?")
        },
        confirmButton = {
            Button(onClick = onSend) {
                Text("Send")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Cancel All")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSkip) {
                    Text("Skip")
                }
            }
        }
    )
}