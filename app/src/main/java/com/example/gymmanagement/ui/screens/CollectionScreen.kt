package com.example.gymmanagement.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

// --- UPDATED: Imports for all the shared components ---
import com.example.gymmanagement.ui.screens.TransactionListItem
import com.example.gymmanagement.ui.screens.UiTransaction
import com.example.gymmanagement.ui.screens.formatCurrency

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CollectionScreen(
    navController: NavController,
    filteredTransactions: List<UiTransaction>,
    onDateFilterChange: (String, Long?, Long?) -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val totalCollection = filteredTransactions.sumOf { it.paymentDetails.amount }

    LaunchedEffect(Unit) {
        onDateFilterChange("This Month", null, null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collections") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by Date")
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
                        "Total Collection for Period",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatCurrency(totalCollection), // This now calls the public helper
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
                items(filteredTransactions) { transaction ->
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

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            CollectionFilterSheetContent(
                onApply = { filter, start, end ->
                    onDateFilterChange(filter, start, end)
                    showFilterSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionFilterSheetContent(
    onApply: (String, Long?, Long?) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("This Month") }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val filterOptions = listOf("Today", "Yesterday", "This Week", "Last Week", "This Month", "Last Month", "Custom")

    Column(
        modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Filter by Date", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { option ->
                FilterChip(
                    selected = (selectedFilter == option),
                    onClick = { selectedFilter = option },
                    label = { Text(option) }
                )
            }
        }

        if (selectedFilter == "Custom") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DatePickerField(
                    label = "Start Date",
                    date = customStartDate,
                    onDateSelected = { customStartDate = it },
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "End Date",
                    date = customEndDate,
                    onDateSelected = { customEndDate = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = { onApply(selectedFilter, customStartDate, customEndDate) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Apply Filter")
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    date: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    date?.let { calendar.timeInMillis = it }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            onDateSelected(newDate.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .clickable { datePickerDialog.show() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = date?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
        }
    }
}

// The duplicate, private formatCurrency function has been REMOVED from this file.