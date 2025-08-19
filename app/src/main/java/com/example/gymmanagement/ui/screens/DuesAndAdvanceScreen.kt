package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun DuesAndAdvanceScreen(
    navController: NavController,
    duesAndAdvanceMembers: List<Member>
) {
    val totalDues = duesAndAdvanceMembers.filter { (it.dueAdvance ?: 0.0) < 0 }.sumOf { it.dueAdvance ?: 0.0 }
    val totalAdvance = duesAndAdvanceMembers.filter { (it.dueAdvance ?: 0.0) > 0 }.sumOf { it.dueAdvance ?: 0.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dues & Advances") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard("Total Dues", totalDues, RedAccent, Modifier.weight(1f))
                SummaryCard("Total Advance", totalAdvance, GreenAccent, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("All Outstanding Balances", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(duesAndAdvanceMembers) { member ->
                    BalanceListItem(member = member)
                }
            }
        }
    }
}

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
fun BalanceListItem(member: Member) {
    val amount = member.dueAdvance ?: 0.0
    val isDue = amount < 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isDue) "Amount Due" else "Advance Paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatCurrency(amount),
                fontWeight = FontWeight.SemiBold,
                color = if (isDue) RedAccent else GreenAccent
            )
        }
    }
}

private fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    // We use absolute value and then add the sign back if needed, for cleaner formatting
    return format.format(value)
}
