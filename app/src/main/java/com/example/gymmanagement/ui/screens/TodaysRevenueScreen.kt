package com.example.gymmanagement.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Payment
import java.text.NumberFormat
import java.util.*

// --- UPDATED: Imports for the shared components ---
import com.example.gymmanagement.ui.screens.TransactionListItem
import com.example.gymmanagement.ui.screens.UiTransaction
import com.example.gymmanagement.ui.screens.formatCurrency


// The local definition of UiTransaction has been REMOVED from here.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodaysRevenueScreen(
    navController: NavController,
    todaysTransactions: List<UiTransaction>
) {
    val totalRevenue = todaysTransactions.sumOf { it.paymentDetails.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Revenue") },
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total Revenue Today",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatCurrency(totalRevenue),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(todaysTransactions) { transaction ->
                    // This now calls the public, reusable TransactionListItem
                    TransactionListItem(
                        transaction = transaction,
                        onClick = {
                            if (transaction.paymentDetails.memberId.isNotBlank()) {
                                navController.navigate("member_details/${transaction.paymentDetails.memberId}")
                            }
                        }
                    )
                }
            }
        }
    }
}

// The private TransactionListItem composable has been REMOVED from here.
// The private formatCurrency function has been REMOVED from here.