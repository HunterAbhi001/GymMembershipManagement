package com.example.gymmanagement.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberScreen(
    navController: NavController,
    member: Member?,
    onSave: (Member) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var gender by remember { mutableStateOf("") }

    LaunchedEffect(member) {
        if (member != null) {
            name = member.name
            contact = member.contact
            selectedPlan = member.plan
            startDate = member.startDate
            gender = member.gender ?: ""
        }
    }

    val plans = (1..12).map { "$it Month${if (it > 1) "s" else ""}" }
    var planExpanded by remember { mutableStateOf(false) }

    val genders = listOf("Male", "Female", "Other")
    var genderExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startDate

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val newDate = Calendar.getInstance()
            newDate.set(selectedYear, selectedMonth, selectedDay)
            startDate = newDate.timeInMillis
        }, year, month, day
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (member == null) "Add Member" else "Edit Member") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val expiryDate = calculateExpiryDate(startDate, selectedPlan)
                val newOrUpdatedMember = member?.copy(
                    name = name,
                    contact = contact,
                    plan = selectedPlan,
                    startDate = startDate,
                    expiryDate = expiryDate,
                    gender = gender
                ) ?: Member(
                    name = name,
                    contact = contact,
                    plan = selectedPlan,
                    startDate = startDate,
                    expiryDate = expiryDate,
                    gender = gender
                )
                onSave(newOrUpdatedMember)
                navController.popBackStack()
            }) {
                Text("Save")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Contact (Phone with Country Code)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Gender Dropdown
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    genders.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                gender = item
                                genderExpanded = false
                            }
                        )
                    }
                }
            }

            // Plan Dropdown
            ExposedDropdownMenuBox(
                expanded = planExpanded,
                onExpandedChange = { planExpanded = !planExpanded }
            ) {
                OutlinedTextField(
                    value = selectedPlan,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Membership Plan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = planExpanded,
                    onDismissRequest = { planExpanded = false }
                ) {
                    plans.forEach { plan ->
                        DropdownMenuItem(
                            text = { Text(plan) },
                            onClick = {
                                selectedPlan = plan
                                planExpanded = false
                            }
                        )
                    }
                }
            }

            // Start Date Picker
            OutlinedTextField(
                value = startDate.toDateString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            )
        }
    }
}

fun calculateExpiryDate(startDate: Long, plan: String): Long {
    val monthsToAdd = plan.split(" ")[0].toIntOrNull() ?: 0
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startDate
    calendar.add(Calendar.MONTH, monthsToAdd)
    return calendar.timeInMillis
}