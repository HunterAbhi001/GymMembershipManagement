package com.example.gymmanagement.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class FilterState(
    val statuses: Set<String> = emptySet(),
    val balanceOption: String = "All",
    val dateRangeOption: String = "All",
    val customStartDate: Long? = null,
    val customEndDate: Long? = null
)

data class SortState(
    val option: String = "Name (A-Z)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllMembersListScreen(
    navController: NavController,
    allMembers: List<Member>,
    viewModel: MainViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var filterState by remember { mutableStateOf(FilterState()) }
    var sortState by remember { mutableStateOf(SortState()) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoading by viewModel.isLoading.collectAsState()
    val deleteProgress by viewModel.deleteProgress.collectAsState()
    val deleteTotal by viewModel.deleteTotal.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch {
                    snackbarHostState.showSnackbar("Import started — completion will be shown by a Toast.")
                }
                viewModel.importMembersFromCsv(context, it)
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch {
                    snackbarHostState.showSnackbar("Export started — completion will be shown by a Toast.")
                }
                viewModel.exportMembersToCsv(context, it)
            }
        }
    )

    val processedMembers = remember(searchQuery, allMembers, filterState, sortState) {
        val todayStart = DateUtils.startOfDayMillis()
        var members = allMembers

        if (searchQuery.isNotBlank()) {
            members = members.filter { member ->
                member.name.contains(searchQuery, ignoreCase = true) ||
                        member.contact.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filterState.statuses.isNotEmpty()) {
            members = members.filter { member ->
                val isActive = member.expiryDate >= todayStart
                val isExpired = member.expiryDate < todayStart
                (filterState.statuses.contains("Active") && isActive) ||
                        (filterState.statuses.contains("Expired") && isExpired)
            }
        }

        when (filterState.balanceOption) {
            "Has Dues" -> members = members.filter { (it.dueAdvance ?: 0.0) < 0 }
            "Has Advance" -> members = members.filter { (it.dueAdvance ?: 0.0) > 0 }
        }

        val calendar = Calendar.getInstance()
        val startDate = when (filterState.dateRangeOption) {
            "Last 1 Month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.timeInMillis
            }
            "Last 2 Months" -> {
                calendar.add(Calendar.MONTH, -2)
                calendar.timeInMillis
            }
            "Custom" -> filterState.customStartDate
            else -> null
        }
        val endDate = if (filterState.dateRangeOption == "Custom") filterState.customEndDate else null

        startDate?.let { start -> members = members.filter { it.startDate >= start } }
        endDate?.let { end -> members = members.filter { it.startDate <= end } }

        members.sortedWith(
            when (sortState.option) {
                "Name (A-Z)" -> compareBy(Member::name)
                "Name (Z-A)" -> compareByDescending(Member::name)
                "Expiry (Soonest First)" -> compareBy(Member::expiryDate)
                "Expiry (Latest First)" -> compareByDescending(Member::expiryDate)
                "Dues (Highest First)" -> Comparator { a, b ->
                    compareValues(a.dueAdvance ?: 0.0, b.dueAdvance ?: 0.0)
                }
                else -> compareBy(Member::name)
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                if (deleteTotal > 0) {
                    val progressFraction: Float = if (deleteTotal > 0) deleteProgress.toFloat() / deleteTotal.toFloat() else 0f
                    LinearProgressIndicator(progress = { progressFraction }, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Deleting members: $deleteProgress / $deleteTotal",
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                TopAppBar(
                    title = { Text("All Members") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter and Sort")
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import from CSV") },
                                onClick = {
                                    menuExpanded = false
                                    if (!isLoading) {
                                        importLauncher.launch("*/*")
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Wait until current operation finishes.") }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export to CSV") },
                                onClick = {
                                    menuExpanded = false
                                    if (!isLoading) {
                                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                        exportLauncher.launch("members_$timestamp.csv")
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Wait until current operation finishes.") }
                                    }
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Delete all members") },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_edit_member") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Members") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(processedMembers) { member ->
                    // --- UPDATED: Using our single reusable MemberListItem ---
                    MemberListItem(
                        member = member,
                        onClick = {
                            navController.navigate("member_details/${member.idString}")
                        },
                        trailingContent = {
                            // Our smart helper does all the work of showing the status
                            ExpiryStatusText(member = member)
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete all members") },
            text = { Text("This will permanently delete ALL members for the signed-in user. This action cannot be undone. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteAllMembers(context)
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            FilterSortSheetContent(
                currentFilterState = filterState,
                currentSortState = sortState,
                onApply = { newFilter, newSort ->
                    scope.launch {
                        filterState = newFilter
                        sortState = newSort
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showFilterSheet = false
                        }
                    }
                },
                onClear = {
                    scope.launch {
                        filterState = FilterState()
                        sortState = SortState()
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showFilterSheet = false
                        }
                    }
                }
            )
        }
    }
}

// --- REMOVED: The old ModernMemberListItem is no longer needed ---

@Composable
private fun FilterSortSheetContent(
    currentFilterState: FilterState,
    currentSortState: SortState,
    onApply: (FilterState, SortState) -> Unit,
    onClear: () -> Unit
) {
    var tempFilters by remember { mutableStateOf(currentFilterState) }
    var tempSort by remember { mutableStateOf(currentSortState) }

    val statusOptions = listOf("Active", "Expired")
    val balanceOptions = listOf("All", "Has Dues", "Has Advance")
    val dateRangeOptions = listOf("All", "Last 1 Month", "Last 2 Months", "Custom")
    val sortOptions = listOf("Name (A-Z)", "Name (Z-A)", "Expiry (Soonest First)", "Expiry (Latest First)", "Dues (Highest First)")

    Column(
        modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Filter By", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text("Status", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            statusOptions.forEach { status ->
                val isSelected = tempFilters.statuses.contains(status)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newStatuses = tempFilters.statuses.toMutableSet()
                        if (isSelected) newStatuses.remove(status) else newStatuses.add(status)
                        tempFilters = tempFilters.copy(statuses = newStatuses)
                    },
                    label = { Text(status) }
                )
            }
        }

        Text("Balance", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            balanceOptions.forEach { option ->
                val isSelected = tempFilters.balanceOption == option
                FilterChip(
                    selected = isSelected,
                    onClick = { tempFilters = tempFilters.copy(balanceOption = option) },
                    label = { Text(option) }
                )
            }
        }

        Text("Joining Date", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dateRangeOptions.forEach { option ->
                FilterChip(
                    selected = tempFilters.dateRangeOption == option,
                    onClick = { tempFilters = tempFilters.copy(dateRangeOption = option) },
                    label = { Text(option) }
                )
            }
        }

        if (tempFilters.dateRangeOption == "Custom") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DatePickerField(
                    label = "Start Date",
                    date = tempFilters.customStartDate,
                    onDateSelected = { tempFilters = tempFilters.copy(customStartDate = it) },
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "End Date",
                    date = tempFilters.customEndDate,
                    onDateSelected = { tempFilters = tempFilters.copy(customEndDate = it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Sort By", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Column {
            sortOptions.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = tempSort.option == option,
                            onValueChange = { tempSort = tempSort.copy(option = option) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = tempSort.option == option, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(option)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text("Clear")
            }
            Button(onClick = { onApply(tempFilters, tempSort) }, modifier = Modifier.weight(1f)) {
                Text("Apply")
            }
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