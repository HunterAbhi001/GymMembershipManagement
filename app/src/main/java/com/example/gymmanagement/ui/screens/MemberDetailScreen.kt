package com.example.gymmanagement.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.gymmanagement.R
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.MembershipHistory
// --- ADDED: Import for the Payment data class ---
import com.example.gymmanagement.data.database.Payment
import com.example.gymmanagement.ui.icons.MessagesIcon
import com.example.gymmanagement.ui.icons.WhatsAppIcon
import com.example.gymmanagement.ui.theme.RedAccent
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import com.example.gymmanagement.ui.utils.sendSmsMessage
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage
import com.example.gymmanagement.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    navController: NavController,
    member: Member?,
    onDelete: () -> Unit,
    viewModel: MainViewModel
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- ADDED: State for payments list and the delete confirmation dialog ---
    val payments by viewModel.memberPayments.collectAsState()
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }


    LaunchedEffect(member) {
        if (member != null) {
            viewModel.fetchMemberHistory(member.idString)
            // --- ADDED: Fetch the payment transaction history as well ---
            viewModel.fetchPaymentsForMember(member.idString)
        }
    }
    val history by viewModel.memberHistory.collectAsState()


    if (showDeleteConfirm && member != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${member.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- ADDED: New dialog to confirm payment deletion ---
    if (paymentToDelete != null && member != null) {
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this payment of ${formatCurrency(paymentToDelete!!.amount)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(paymentToDelete!!.paymentId, member.idString)
                        paymentToDelete = null // Close the dialog
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { paymentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Member Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (member != null) {
                        val sevenDaysFromNow = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
                        if (member.expiryDate < sevenDaysFromNow) {
                            IconButton(onClick = { navController.navigate("add_edit_member?memberId=${member.idString}&isRenewal=true") }) {
                                Icon(Icons.Default.Autorenew, contentDescription = "Renew Membership")
                            }
                        }
                        IconButton(onClick = { navController.navigate("add_edit_member?memberId=${member.idString}&isRenewal=false") }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Member")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Member")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (member == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Member not found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    MemberHeader(member = member)
                }
                item {
                    MembershipInfoCard(member = member)
                }
                item {
                    PaymentInfoCard(member = member)
                }
                item {
                    MembershipHistoryCard(history = history)
                }
                // --- ADDED: New item for the payment history card ---
                item {
                    PaymentHistoryCard(payments = payments, onDeleteClick = { payment ->
                        paymentToDelete = payment
                    })
                }
            }
        }
    }
}

@Composable
private fun MemberHeader(member: Member) {
    val context = LocalContext.current
    val status = if (member.expiryDate >= DateUtils.startOfDayMillis()) "Active" else "Expired"
    val statusColor = if (status == "Active") Color(0xFF4CAF50) else RedAccent
    var showImagePreview by remember { mutableStateOf(false) }

    val message = "Hi ${member.name.split(" ").firstOrNull() ?: ""}, this is a message from the gym regarding your membership. Your current status is '$status' and your membership is/was valid till ${member.expiryDate.toDateString()}."

    if (showImagePreview && member.photoUri != null) {
        FullScreenImageViewer(
            uri = Uri.parse(member.photoUri),
            onDismiss = { showImagePreview = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { if (member.photoUri != null) showImagePreview = true },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = member.photoUri,
                    error = painterResource(id = R.drawable.ic_person_placeholder)
                ),
                contentDescription = "Member Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            IconButton(onClick = { sendSmsMessage(context, member.contact, message) }) {
                Icon(MessagesIcon, contentDescription = "Send SMS", modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { sendWhatsAppMessage(context, member.contact, message) }) {
                Icon(WhatsAppIcon, contentDescription = "Send WhatsApp", modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.contact}"))
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun MembershipInfoCard(member: Member) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Membership Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(icon = Icons.Default.Badge, label = "Gender", value = member.gender ?: "N/A")
            InfoRow(icon = Icons.Default.FitnessCenter, label = "Plan", value = member.plan)
            InfoRow(icon = Icons.Default.AccessTime, label = "Batch", value = member.batch ?: "N/A")
            InfoRow(icon = Icons.Default.Event, label = "Start Date", value = member.startDate.toDateString())
            InfoRow(icon = Icons.Default.EventBusy, label = "Expiry Date", value = member.expiryDate.toDateString())
        }
    }
}

@Composable
private fun PaymentInfoCard(member: Member) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Payment Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(icon = Icons.Default.CalendarToday, label = "Purchase Date", value = member.purchaseDate?.toDateString() ?: "N/A")
            InfoRow(icon = Icons.Default.PriceCheck, label = "Price", value = formatCurrency(member.price))
            InfoRow(icon = Icons.Default.TrendingDown, label = "Discount", value = formatCurrency(member.discount))
            InfoRow(icon = Icons.Default.ReceiptLong, label = "Final Amount", value = formatCurrency(member.finalAmount), isHighlight = true)
            InfoRow(icon = Icons.Default.AccountBalanceWallet, label = "Due / Advance", value = formatCurrency(member.dueAdvance))
        }
    }
}

@Composable
private fun MembershipHistoryCard(history: List<MembershipHistory>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Membership History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (history.isEmpty()) {
                Text("No past membership records found.", color = Color.Gray)
            } else {
                history.forEach { record ->
                    HistoryListItem(record)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

// --- ADDED: New Composable for the interactive Payment History Card ---
@Composable
private fun PaymentHistoryCard(
    payments: List<Payment>,
    onDeleteClick: (Payment) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Payment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (payments.isEmpty()) {
                Text("No payment records found.", color = Color.Gray)
            } else {
                payments.forEach { payment ->
                    PaymentListItem(payment = payment, onDeleteClick = { onDeleteClick(payment) })
                    if (payment != payments.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

// --- ADDED: New Composable for each item in the Payment History list ---
@Composable
private fun PaymentListItem(
    payment: Payment,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Payment Entry",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Type: ${payment.type}", fontWeight = FontWeight.SemiBold)
            Text(
                text = payment.transactionDate?.time?.toDateString() ?: "N/A",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatCurrency(payment.amount),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.DeleteOutline, "Delete Payment", tint = RedAccent)
        }
    }
}


@Composable
private fun HistoryListItem(record: MembershipHistory) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = "History Entry",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = record.plan, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${record.startDate.toDateString()} - ${record.expiryDate.toDateString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatCurrency(record.finalAmount),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}


@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FullScreenImageViewer(uri: Uri, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = uri),
                contentDescription = "Full Screen Member Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

private fun formatCurrency(value: Double?): String {
    if (value == null) return "N/A"
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    format.maximumFractionDigits = 2
    return format.format(value)
}