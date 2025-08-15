package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.theme.AppIcons
import com.example.gymmanagement.ui.theme.Green
import com.example.gymmanagement.ui.theme.Red
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import com.example.gymmanagement.ui.utils.sendSmsMessage
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    navController: NavController,
    member: Member?,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                    IconButton(onClick = {
                        val message = "Hi ${member?.name}, regarding your gym membership..."
                        sendSmsMessage(context, member?.contact ?: "", message)
                    }) {
                        Icon(Icons.Default.Sms, contentDescription = "Send SMS")
                    }
                    IconButton(onClick = {
                        val message = "Hi ${member?.name}, regarding your gym membership..."
                        sendWhatsAppMessage(context, member?.contact ?: "", message)
                    }) {
                        Icon(AppIcons.WhatsApp, contentDescription = "Send WhatsApp Message")
                    }
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
            InfoRow("Gender", member.gender ?: "Not specified")
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