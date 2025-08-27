package com.example.gymmanagement.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.gymmanagement.R
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.icons.WhatsAppIcon
import com.example.gymmanagement.ui.theme.RedAccent
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuesScreen(
    navController: NavController,
    membersWithDues: List<Member>,
    onUpdateDues: (Member, Double) -> Unit
) {
    val totalDues = membersWithDues.sumOf { it.dueAdvance ?: 0.0 }
    var memberToPay by remember { mutableStateOf<Member?>(null) }
    val context = LocalContext.current

    // --- State for the guided reminder flow ---
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderIndex by remember { mutableStateOf(0) }


    if (memberToPay != null) {
        PaymentDialog(
            member = memberToPay!!,
            onDismiss = { memberToPay = null },
            onConfirm = { amountPaid ->
                onUpdateDues(memberToPay!!, amountPaid)
                memberToPay = null
            }
        )
    }

    // --- Guided Reminder Dialog ---
    if (showReminderDialog) {
        val currentMember = membersWithDues.getOrNull(reminderIndex)
        if (currentMember != null) {
            ReminderDialog(
                member = currentMember,
                currentIndex = reminderIndex + 1,
                total = membersWithDues.size,
                onDismiss = { showReminderDialog = false },
                onSend = {
                    val firstName = currentMember.name.split(" ").firstOrNull() ?: currentMember.name
                    val dueAmount = abs(currentMember.dueAdvance ?: 0.0)
                    val message = "Hi $firstName, just a friendly reminder about your outstanding balance of ${formatCurrency(dueAmount)} at the gym. Thank you!"
                    sendWhatsAppMessage(context, currentMember.contact, message)
                    if (reminderIndex < membersWithDues.size - 1) {
                        reminderIndex++
                    } else {
                        showReminderDialog = false
                        Toast.makeText(context, "All reminders sent!", Toast.LENGTH_SHORT).show()
                    }
                },
                onSkip = {
                    if (reminderIndex < membersWithDues.size - 1) {
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
                title = { Text("Outstanding Dues") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            SummaryCard(
                title = "Total Dues",
                amount = totalDues,
                color = RedAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (membersWithDues.isNotEmpty()) {
                        reminderIndex = 0
                        showReminderDialog = true
                    } else {
                        Toast.makeText(context, "No members with dues to remind.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = membersWithDues.isNotEmpty()
            ) {
                Icon(WhatsAppIcon, contentDescription = "WhatsApp", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remind All via WhatsApp")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Members with Dues", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(membersWithDues) { member ->
                    ModernDuesListItem(
                        member = member,
                        onPayClick = { memberToPay = member },
                        onClick = { navController.navigate("member_details/${member.idString}") }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = formatCurrency(abs(amount)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun ModernDuesListItem(
    member: Member,
    onPayClick: () -> Unit,
    onClick: () -> Unit
) {
    val amount = member.dueAdvance ?: 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = member.photoUri,
                    error = painterResource(id = R.drawable.ic_person_placeholder)
                ),
                contentDescription = "Member Photo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold)
                Text(
                    text = "Amount Due: ${formatCurrency(abs(amount))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RedAccent
                )
            }
            Button(onClick = onPayClick) {
                Text("Pay")
            }
        }
    }
}

@Composable
private fun PaymentDialog(
    member: Member,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountPaidInput by remember { mutableStateOf("") }
    val currentDue = member.dueAdvance ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Payment for ${member.name}") },
        text = {
            Column {
                Text("Current Due: ${formatCurrency(abs(currentDue))}")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountPaidInput,
                    onValueChange = { amountPaidInput = it },
                    label = { Text("Amount Received") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountPaid = amountPaidInput.toDoubleOrNull() ?: 0.0
                    if (amountPaid > 0) {
                        onConfirm(amountPaid)
                    }
                }
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- ADDED: Composable for the guided reminder flow ---
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

private fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    format.maximumFractionDigits = 2
    return format.format(value)
}
