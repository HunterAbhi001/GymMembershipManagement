package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.theme.GreenAccent
import com.example.gymmanagement.ui.theme.RedAccent
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuesScreen(
    navController: NavController,
    membersWithDues: List<Member>,
    onUpdateDues: (Member, Double) -> Unit // Callback to update the dues
) {
    val totalDues = membersWithDues.sumOf { it.dueAdvance ?: 0.0 }
    var memberToPay by remember { mutableStateOf<Member?>(null) }

    // Show the payment dialog when a member is selected
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Outstanding Dues") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SummaryCard("Total Dues", totalDues, RedAccent, Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Text("Members with Dues", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(membersWithDues) { member ->
                    BalanceListItem(
                        member = member,
                        onPayClick = { memberToPay = member } // Set the member to trigger the dialog
                    )
                }
            }
        }
    }
}

// --- FIX: Added the missing SummaryCard composable function ---
@Composable
fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun BalanceListItem(
    member: Member,
    onPayClick: () -> Unit
) {
    val amount = member.dueAdvance ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold)
                Text(
                    text = "Amount Due",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(amount),
                    fontWeight = FontWeight.SemiBold,
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
                Text("Current Due: ${formatCurrency(currentDue)}")
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

private fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    return format.format(value)
}
