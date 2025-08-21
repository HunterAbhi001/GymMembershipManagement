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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Plan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    plans: List<Plan>,
    onSavePlan: (Plan) -> Unit
) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Define Plan Prices") },
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
                            // ✅ Now we can create a Plan without explicitly passing price
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
