package com.example.gymmanagement.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Plan
import com.example.gymmanagement.viewmodel.MainViewModel
import com.example.gymmanagement.viewmodel.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    plans: List<Plan>,
    onSavePlan: (Plan) -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var errorToShow by remember { mutableStateOf<String?>(null) }
    val priceInputs = remember {
        mutableStateMapOf<String, String>().apply {
            val planMap = plans.associateBy { it.planName }
            (1..12).forEach {
                val planName = "$it Month${if (it > 1) "s" else ""}"
                val price = planMap[planName]?.price ?: 0.0
                if (price > 0) {
                    this[planName] = price.toInt().toString()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShowError -> {
                    errorToShow = event.message
                }
                else -> {}
            }
        }
    }

    if (errorToShow != null) {
        AlertDialog(
            onDismissRequest = { errorToShow = null },
            title = { Text("An Error Occurred") },
            text = { Text(errorToShow!!) },
            confirmButton = {
                TextButton(onClick = { errorToShow = null }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        priceInputs.forEach { (planName, priceString) ->
                            val price = priceString.toDoubleOrNull() ?: 0.0
                            val existingPlan = plans.find { it.planName == planName }
                            val planToSave = (existingPlan ?: Plan(planName = planName))
                                .copy(price = price)
                            onSavePlan(planToSave)
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Save All Changes")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Define Plan Prices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items((1..12).toList()) { month ->
                val planName = "$month Month${if (month > 1) "s" else ""}"
                PlanPriceItem(
                    planName = planName,
                    price = priceInputs[planName] ?: "",
                    onPriceChange = { newPrice ->
                        priceInputs[planName] = newPrice
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                Text("Data Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // --- ADDED: Button to migrate old payment data ---
                OutlinedButton(
                    onClick = { viewModel.migrateLegacyPayments() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upgrade, contentDescription = "Migrate", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Migrate Old Payments")
                }
                Text(
                    "Use this one-time tool to create payment records for members added before the new payment system was implemented.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.cleanUpOrphanedData() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clean Up", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clean Up Old Data")
                }
                Text(
                    "If you have deleted members in the past, their old payment records might still exist. Use this to clean up any orphaned data.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanPriceItem(
    planName: String,
    price: String,
    onPriceChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = planName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = price,
                onValueChange = { onPriceChange(it.filter { char -> char.isDigit() }) },
                label = { Text("Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Text("₹") },
                modifier = Modifier.width(150.dp)
            )
        }
    }
}
