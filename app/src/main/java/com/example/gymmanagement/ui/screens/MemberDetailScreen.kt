package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.CheckIn
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.theme.Green
import com.example.gymmanagement.ui.theme.Red
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    navController: NavController,
    member: Member?,
    checkIns: List<CheckIn>,
    onCheckIn: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(member?.name ?: "Member Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("add_edit_member?memberId=${member?.id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Member")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Member", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (member == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                DetailCard(member)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCheckIn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = member.expiryDate >= System.currentTimeMillis()
                ) {
                    Text("Record Check-in")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Check-in History", style = MaterialTheme.typography.titleLarge)
                LazyColumn {
                    items(checkIns) { checkIn ->
                        CheckInHistoryItem(checkIn)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Member") },
            text = { Text("Are you sure you want to delete ${member?.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailCard(member: Member) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val status = if (member.expiryDate >= System.currentTimeMillis()) "Active" else "Expired"
            val statusColor = if (status == "Active") Green else Red

            Text(member.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(member.contact, style = MaterialTheme.typography.bodyLarge)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            InfoRow("Status", status, contentColor = statusColor)
            InfoRow("Plan", member.plan)
            InfoRow("Start Date", member.startDate.toDateString())
            InfoRow("Expiry Date", member.expiryDate.toDateString())
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, contentColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}

@Composable
fun CheckInHistoryItem(checkIn: CheckIn) {
    Text(
        text = checkIn.timestamp.toFullDateTimeString(),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFullDateTimeString(): String {
    val sdf = SimpleDateFormat("EEE, dd MMM yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}