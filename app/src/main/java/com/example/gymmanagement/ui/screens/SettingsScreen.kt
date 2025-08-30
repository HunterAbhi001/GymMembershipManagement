package com.example.gymmanagement.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Plan
import com.example.gymmanagement.ui.auth.BiometricAuthenticator
import com.example.gymmanagement.ui.theme.RedAccent
import com.example.gymmanagement.ui.utils.security.SecurePrefs
import com.example.gymmanagement.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- NEW: A sealed class to define what action requires authentication ---
sealed class AuthAction {
    object ToDisableLock : AuthAction()
    object ToChangePin : AuthAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    plans: List<Plan>,
    onSavePlan: (Plan) -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val activity = context as AppCompatActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricAuthenticator = remember { BiometricAuthenticator(context) }

    // State for dialogs and actions
    var showPinDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var appLockEnabled by remember { mutableStateOf(SecurePrefs.isAppLockEnabled(context)) }

    // --- NEW: State to manage which secure action is being attempted ---
    var authAction by remember { mutableStateOf<AuthAction?>(null) }

    // --- NEW: This LaunchedEffect handles the authentication prompt ---
    LaunchedEffect(authAction) {
        if (authAction != null && biometricAuthenticator.isBiometricAuthAvailable()) {
            biometricAuthenticator.promptBiometricAuth(
                title = "Authentication Required",
                subtitle = "Please authenticate to continue",
                activity = activity,
                onSuccess = {
                    // On successful authentication, perform the requested action
                    when (authAction) {
                        is AuthAction.ToDisableLock -> {
                            appLockEnabled = false
                            SecurePrefs.setAppLockEnabled(context, false)
                            Toast.makeText(context, "App Lock Disabled", Toast.LENGTH_SHORT).show()
                        }
                        is AuthAction.ToChangePin -> {
                            showPinDialog = true // Open the dialog to set a new PIN
                        }
                        null -> {}
                    }
                    authAction = null // Reset the action
                },
                onFailed = {
                    Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                    authAction = null // Reset the action
                },
                onError = { _, _ ->
                    // User might cancel, so we just reset the action
                    authAction = null
                }
            )
        } else if (authAction != null) {
            // Fallback for devices without biometrics (show PIN entry dialog)
            // For simplicity, we're currently just showing the PinSetupDialog directly for changing PIN
            // A full implementation would have a "verify old PIN" dialog here.
            when (authAction) {
                is AuthAction.ToDisableLock -> {
                    appLockEnabled = false
                    SecurePrefs.setAppLockEnabled(context, false)
                    Toast.makeText(context, "App Lock Disabled (No Biometrics)", Toast.LENGTH_SHORT).show()
                }
                is AuthAction.ToChangePin -> showPinDialog = true
                null -> {}
            }
            authAction = null
        }
    }

    // State for editable plan prices
    val fullPlanList = remember(plans) {
        val planMap = plans.associateBy { it.planName }
        (1..12).map {
            val planName = "$it Month${if (it > 1) "s" else ""}"
            planMap[planName] ?: Plan(planName = planName, price = 0.0)
        }
    }
    var planPrices by remember { mutableStateOf(fullPlanList.associate { it.planName to it.price.toString() }) }
    LaunchedEffect(fullPlanList) {
        planPrices = fullPlanList.associate { it.planName to it.price.toString() }
    }

    // File pickers for data management
    val isLoading by viewModel.isLoading.collectAsState()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch { snackbarHostState.showSnackbar("Import started...") }
                viewModel.importMembersFromCsv(context, it)
            }
        }
    )
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? ->
            uri?.let {
                scope.launch { snackbarHostState.showSnackbar("Export started...") }
                viewModel.exportMembersToCsv(context, it)
            }
        }
    )

    // PIN setup dialog
    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onPinSet = { pin ->
                SecurePrefs.savePin(context, pin)
                appLockEnabled = true
                SecurePrefs.setAppLockEnabled(context, true)
                showPinDialog = false
                Toast.makeText(context, "PIN Updated Successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete All confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Warning") },
            title = { Text("Delete All Members?") },
            text = { Text("This is irreversible and will permanently delete all member data. Are you absolutely sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllMembers(context)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) { Text("Yes, Delete All") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Security
            SettingsSection(title = "Security", icon = Icons.Default.Security) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable App Lock", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = { isEnabled ->
                            if (isEnabled) {
                                if (SecurePrefs.hasPinSetup(context)) {
                                    appLockEnabled = true
                                    SecurePrefs.setAppLockEnabled(context, true)
                                } else {
                                    showPinDialog = true
                                }
                            } else {
                                authAction = AuthAction.ToDisableLock
                            }
                        }
                    )
                }
                AnimatedVisibility(visible = appLockEnabled) {
                    Column {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsDataItem(
                            label = "Change PIN",
                            icon = Icons.Default.Password,
                            onClick = {
                                authAction = AuthAction.ToChangePin
                            }
                        )
                    }
                }
            }

            // Section 2: Membership Plans
            SettingsSection(title = "Membership Plans", icon = Icons.Default.CardMembership, initiallyExpanded = false) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fullPlanList.forEach { plan ->
                        OutlinedTextField(
                            value = planPrices[plan.planName] ?: "0.0",
                            onValueChange = { newValue ->
                                planPrices = planPrices.toMutableMap().apply { this[plan.planName] = newValue }
                            },
                            label = { Text(plan.planName) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            planPrices.forEach { (name, price) ->
                                onSavePlan(Plan(planName = name, price = price.toDoubleOrNull() ?: 0.0))
                            }
                            scope.launch { snackbarHostState.showSnackbar("Plan prices saved!") }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Plans")
                    }
                }
            }

            // Section 3: Data Management
            SettingsSection(title = "Data Management", icon = Icons.Default.Storage, initiallyExpanded = false) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsDataItem(
                        label = "Import from CSV",
                        icon = Icons.Default.FileUpload,
                        onClick = {
                            if (!isLoading) {
                                importLauncher.launch("*/*")
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Please wait for current operation to finish.") }
                            }
                        }
                    )
                    SettingsDataItem(
                        label = "Export to CSV",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            if (!isLoading) {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                exportLauncher.launch("members_$timestamp.csv")
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Please wait for current operation to finish.") }
                            }
                        }
                    )
                    Divider()
                    SettingsDataItem(
                        label = "Delete All Members",
                        icon = Icons.Default.DeleteForever,
                        onClick = { showDeleteConfirmDialog = true },
                        isDestructive = true
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                    Divider(modifier = Modifier.padding(bottom = 16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsDataItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDestructive) RedAccent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) RedAccent else MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val isError = pin.length == 4 && confirmPin.length == 4 && pin != confirmPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a 4-Digit PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This PIN can be used as a backup if biometric authentication fails.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 4) pin = it },
                    label = { Text("Enter PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 4) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = isError,
                    supportingText = { if (isError) Text("PINs do not match") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPinSet(pin) },
                enabled = pin.length == 4 && pin == confirmPin
            ) { Text("Set PIN") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}