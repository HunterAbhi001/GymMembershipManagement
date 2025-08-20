package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.icons.WhatsAppIcon
import com.example.gymmanagement.ui.theme.Green
import com.example.gymmanagement.ui.theme.Red
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import com.example.gymmanagement.ui.utils.sendSmsMessage
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    navController: NavController,
    member: Member?,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

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
                        Icon(WhatsAppIcon, contentDescription = "Send WhatsApp Message", tint = Color.Unspecified)
                    }
                    IconButton(onClick = {
                        navController.navigate("add_edit_member?memberId=${member?.id}&isRenewal=false")
                    }) {
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
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val isExpired = member.expiryDate < todayStart
                DetailCard(member, onImageClick = { showFullImage = true }, isExpired = isExpired)

                if (isExpired) {
                    // --- FIX: Corrected the parameter name from "isRenew" to "isRenewal" ---
                    Button(onClick = { navController.navigate("add_edit_member?memberId=${member.id}&isRenewal=true") }, colors = ButtonDefaults.buttonColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) {
                        Text("Renew Membership")
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
                Button(onClick = { onDelete(); showDeleteDialog = false; navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showFullImage && member?.photoUri != null) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { showFullImage = false }, contentAlignment = Alignment.Center) {
                AsyncImage(model = member.photoUri, contentDescription = "Full Image", contentScale = ContentScale.Fit, placeholder = rememberVectorPainter(Icons.Default.Person), error = rememberVectorPainter(Icons.Default.Person), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun DetailCard(member: Member, onImageClick: () -> Unit, isExpired: Boolean) {
    val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    nf.currency = Currency.getInstance("INR")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = member.photoUri, contentDescription = "Member Photo", modifier = Modifier.size(80.dp).clip(CircleShape).clickable { onImageClick() }, contentScale = ContentScale.Crop, placeholder = rememberVectorPainter(Icons.Default.Person), error = rememberVectorPainter(Icons.Default.Person))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val status = if (!isExpired) "Active" else "Expired"
                    val statusColor = if (!isExpired) Green else Red
                    Text(member.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(member.contact, style = MaterialTheme.typography.bodyLarge)
                    Text(status, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            InfoRow("Gender", member.gender ?: "Not specified")
            InfoRow("Plan", member.plan)
            InfoRow("Batch", member.batch ?: "—")
            InfoRow("Start Date", member.startDate.toDateString())
            InfoRow("Expiry Date", member.expiryDate.toDateString())
            InfoRow("Purchase Date", member.purchaseDate?.toDateString() ?: "—")
            InfoRow("Price", member.price?.let { nf.format(it) } ?: "—")
            InfoRow("Discount", member.discount?.let { nf.format(it) } ?: "—")
            InfoRow("Final Amount", member.finalAmount?.let { nf.format(it) } ?: "—")
            InfoRow("Due / Advance", member.dueAdvance?.let { nf.format(it) } ?: "—")
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
