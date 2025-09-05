package com.example.gymmanagement.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.gymmanagement.R
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.Plan
import com.example.gymmanagement.ui.utils.ComposeFileProvider
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage
import com.example.gymmanagement.viewmodel.MainViewModel
import com.example.gymmanagement.viewmodel.UiEvent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.util.*

// --- IMPORTING THE SHARED FUNCTION ---
import com.example.gymmanagement.ui.screens.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberScreen(
    navController: NavController,
    member: Member?,
    viewModel: MainViewModel,
    isRenewal: Boolean = false,
    plans: List<Plan>,
    isDarkTheme: Boolean
) {
    val haptics = LocalHapticFeedback.current

    // Basic fields
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }

    // Date/time fields
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var expiryDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var baseDateForRenewal by remember { mutableStateOf(System.currentTimeMillis()) }

    // Financial fields
    var priceInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var finalAmountInput by remember { mutableStateOf("") }
    var amountReceivedInput by remember { mutableStateOf("") }
    var dueAdvanceInput by remember { mutableStateOf("") }

    var customDaysInput by remember { mutableStateOf("") }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var errorToShow by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShowError -> {
                    errorToShow = event.message
                    isSaving = false
                }
                is UiEvent.SaveSuccess -> {
                    if (event.shouldSendMessage && event.member.contact.isNotBlank()) {
                        val savedMember = event.member
                        val firstName = savedMember.name.split(" ").firstOrNull() ?: ""
                        val formattedFinalAmount = formatCurrency(savedMember.finalAmount ?: 0.0)
                        val formattedStartDate = savedMember.startDate.toDateString()
                        val formattedExpiryDate = savedMember.expiryDate.toDateString()
                        val message = "Hi $firstName, your ${savedMember.plan} membership for $formattedFinalAmount has started on $formattedStartDate and will expire on $formattedExpiryDate. Welcome to the Iron House Gym!"
                        try {
                            sendWhatsAppMessage(context, savedMember.contact, message)
                        } catch (e: Exception) {
                            Log.e("AddEditMemberScreen", "Could not send WhatsApp message", e)
                            Toast.makeText(context, "Could not open WhatsApp.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    navController.popBackStack()
                }
            }
        }
    }

    if (errorToShow != null) {
        AlertDialog(
            onDismissRequest = { errorToShow = null },
            title = { Text("Operation Failed") },
            text = { Text(errorToShow!!) },
            confirmButton = {
                TextButton(onClick = { errorToShow = null }) {
                    Text("OK")
                }
            }
        )
    }


    fun parseDoubleOrNull(text: String): Double? {
        return text.trim().takeIf { it.isNotBlank() }?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull()
    }

    LaunchedEffect(member, isRenewal) {
        if (member == null) { // Brand new member
            startDate = System.currentTimeMillis()
            expiryDate = calculateExpiryDate(startDate, selectedPlan)
            baseDateForRenewal = startDate
        } else {
            // Populate common fields from existing member data
            name = member.name
            contact = member.contact
            gender = member.gender ?: ""
            batch = member.batch ?: ""
            photoUri = member.photoUri?.let { Uri.parse(it) }

            if (isRenewal) {
                startDate = member.expiryDate
                baseDateForRenewal = member.expiryDate
                expiryDate = member.expiryDate
                // Clear fields for the new transaction
                selectedPlan = ""
                priceInput = ""
                discountInput = ""
                finalAmountInput = ""
                amountReceivedInput = ""
                dueAdvanceInput = ""
            } else { // Simple Edit
                selectedPlan = member.plan
                startDate = member.startDate
                expiryDate = member.expiryDate
                priceInput = member.price?.toString() ?: ""
                discountInput = member.discount?.toString() ?: ""
                finalAmountInput = member.finalAmount?.toString() ?: ""
                amountReceivedInput = member.finalAmount?.toString() ?: ""
                dueAdvanceInput = member.dueAdvance?.toString() ?: ""
            }
        }
    }

    LaunchedEffect(customDaysInput, baseDateForRenewal) {
        if (selectedPlan == "Custom Days") {
            val daysToAdd = customDaysInput.toIntOrNull()
            if (daysToAdd != null && daysToAdd > 0) {
                val calendar = Calendar.getInstance().apply { timeInMillis = baseDateForRenewal }
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                expiryDate = calendar.timeInMillis
            } else {
                expiryDate = baseDateForRenewal
            }
        }
    }

    LaunchedEffect(priceInput, discountInput) {
        val price = parseDoubleOrNull(priceInput)
        val discount = parseDoubleOrNull(discountInput)
        if (price != null) {
            val calculatedAmount = price - (discount ?: 0.0)
            finalAmountInput = if (calculatedAmount >= 0) "%.2f".format(calculatedAmount) else "0.00"
        } else {
            finalAmountInput = ""
        }
    }

    LaunchedEffect(finalAmountInput) {
        amountReceivedInput = finalAmountInput
    }

    LaunchedEffect(finalAmountInput, amountReceivedInput) {
        val finalAmount = parseDoubleOrNull(finalAmountInput)
        val amountReceived = parseDoubleOrNull(amountReceivedInput)
        if (finalAmount != null) {
            val dueAdvance = (amountReceived ?: 0.0) - finalAmount
            dueAdvanceInput = "%.2f".format(dueAdvance)
        } else {
            dueAdvanceInput = ""
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success -> if (success) photoUri = tempUri }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted -> if (granted) { val newUri = ComposeFileProvider.getImageUri(context); grantUriPermissionsForCamera(context, newUri); tempUri = newUri; cameraLauncher.launch(newUri) } else { Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show() } }
    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }; photoUri = it } }
    val storagePermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted -> if (!granted) { Toast.makeText(context, "Permission to access photos denied", Toast.LENGTH_SHORT).show() } }
    fun pickImageFromGallery() { val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE; if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) { galleryLauncher.launch(arrayOf("image/*")) } else { storagePermissionLauncher.launch(permission) } }
    fun launchCameraWithCheck() { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { val newUri = ComposeFileProvider.getImageUri(context); grantUriPermissionsForCamera(context, newUri); tempUri = newUri; cameraLauncher.launch(newUri) } else { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) } }

    val datePickerTheme = if (isDarkTheme) {
        R.style.App_Theme_DatePicker_Dark
    } else {
        R.style.App_Theme_DatePicker_Light
    }

    val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
    val startDatePicker = DatePickerDialog(
        context,
        datePickerTheme,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            val newDate = Calendar.getInstance().apply { set(y, m, d) }
            startDate = newDate.timeInMillis
            baseDateForRenewal = newDate.timeInMillis
            expiryDate = calculateExpiryDate(startDate, selectedPlan)
        },
        startCal.get(Calendar.YEAR),
        startCal.get(Calendar.MONTH),
        startCal.get(Calendar.DAY_OF_MONTH)
    )
    val expiryCal = Calendar.getInstance().apply { timeInMillis = expiryDate }
    val expiryDatePicker = DatePickerDialog(
        context,
        datePickerTheme,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            expiryDate = Calendar.getInstance().apply { set(y, m, d) }.timeInMillis
        },
        expiryCal.get(Calendar.YEAR),
        expiryCal.get(Calendar.MONTH),
        expiryCal.get(Calendar.DAY_OF_MONTH)
    )

    val genders = listOf("Male", "Female", "Other")
    val batches = listOf("Morning", "Evening")
    var planExpanded by remember { mutableStateOf(false) }

    val fullPlanList = remember(plans) {
        val planMap = plans.associateBy { it.planName }
        (1..12).map {
            val planName = "$it Month${if (it > 1) "s" else ""}"
            planMap[planName] ?: Plan(planName = planName, price = 0.0)
        }
    }

    val isFormValid by remember { derivedStateOf { name.isNotBlank() } }

    val backgroundModifier = if (isDarkTheme) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF0D1B2A), Color(0xFF1A237E).copy(alpha = 0.5f))
            )
        )
    } else {
        Modifier.background(MaterialTheme.colorScheme.background)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text( when { member == null -> "Add Member"; isRenewal -> "Renew/Extend Member"; else -> "Edit Member" }) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) Color.Transparent else MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSaving = true
                        val planToSave = if (selectedPlan == "Custom Days") {
                            "Custom (${customDaysInput.trim()} Days)"
                        } else {
                            selectedPlan
                        }
                        val purchaseDateToSave = if (isRenewal || member == null) {
                            System.currentTimeMillis()
                        } else {
                            member.purchaseDate ?: startDate
                        }
                        val updatedMember = (member?.copy(
                            name = name.trim(),
                            contact = contact.trim(),
                            plan = planToSave,
                            startDate = startDate,
                            expiryDate = expiryDate,
                            gender = gender,
                            photoUri = photoUri?.toString(),
                            batch = batch.ifBlank { "" },
                            price = parseDoubleOrNull(priceInput),
                            discount = parseDoubleOrNull(discountInput),
                            finalAmount = parseDoubleOrNull(finalAmountInput),
                            purchaseDate = purchaseDateToSave,
                            dueAdvance = parseDoubleOrNull(dueAdvanceInput)
                        ) ?: Member(
                            name = name.trim(),
                            contact = contact.trim(),
                            plan = planToSave,
                            startDate = startDate,
                            expiryDate = expiryDate,
                            gender = gender,
                            photoUri = photoUri?.toString(),
                            batch = batch.ifBlank { "" },
                            price = parseDoubleOrNull(priceInput),
                            discount = parseDoubleOrNull(discountInput),
                            finalAmount = parseDoubleOrNull(finalAmountInput),
                            purchaseDate = purchaseDateToSave,
                            dueAdvance = parseDoubleOrNull(dueAdvanceInput)
                        ))

                        viewModel.addOrUpdateMember(
                            member = updatedMember,
                            photoUri = photoUri,
                            context = context,
                            isRenewal = isRenewal,
                            amountReceived = parseDoubleOrNull(amountReceivedInput) ?: 0.0
                        )
                    },
                    enabled = isFormValid && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = when { member == null -> "Save Member"; isRenewal -> "Renew/Extend"; else -> "Update Member" },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize().then(backgroundModifier)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showPhotoPickerDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = photoUri, error = painterResource(id = R.drawable.ic_person_placeholder)),
                        contentDescription = "Member Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            FormSection(title = "Personal Details", isDarkTheme = isDarkTheme) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact (Phone)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text("Gender", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        genders.forEach { item ->
                            FilterChip(
                                selected = (gender == item),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    gender = item
                                },
                                label = { Text(item) }
                            )
                        }
                    }
                }
            }

            FormSection(title = "Membership Plan", isDarkTheme = isDarkTheme) {
                ExposedDropdownMenuBox(
                    expanded = planExpanded,
                    onExpandedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        planExpanded = !planExpanded
                    }
                ) {
                    OutlinedTextField(value = selectedPlan, onValueChange = {}, readOnly = true, label = { Text("Membership Plan") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = planExpanded, onDismissRequest = { planExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Custom Days", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) },
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedPlan = "Custom Days"
                                priceInput = ""
                                customDaysInput = ""
                                expiryDate = baseDateForRenewal
                                planExpanded = false
                            }
                        )
                        Divider()

                        fullPlanList.forEach { plan ->
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(plan.planName)
                                        if (plan.price > 0) {
                                            Text(text = formatCurrency(plan.price), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedPlan = plan.planName
                                    customDaysInput = ""
                                    expiryDate = calculateExpiryDate(baseDateForRenewal, plan.planName)
                                    val newPrice = if (plan.price > 0) plan.price.toString() else ""
                                    priceInput = newPrice
                                    val price = parseDoubleOrNull(newPrice)
                                    val discount = parseDoubleOrNull(discountInput)
                                    if (price != null) {
                                        val calculatedAmount = price - (discount ?: 0.0)
                                        amountReceivedInput = if (calculatedAmount >= 0) "%.2f".format(calculatedAmount) else "0.00"
                                    } else {
                                        amountReceivedInput = ""
                                    }
                                    planExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedPlan == "Custom Days") {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customDaysInput,
                        onValueChange = { customDaysInput = it },
                        label = { Text("Enter Number of Days") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text("Batch", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp, top = 16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        batches.forEach { item ->
                            FilterChip(
                                selected = (batch == item),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    batch = item
                                },
                                label = { Text(item) }
                            )
                        }
                    }
                }
                DatePickerField("Start Date", startDate.toDateString()) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    startDatePicker.show()
                }
                DatePickerField("Expiry Date", expiryDate.toDateString()) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (selectedPlan != "Custom Days") expiryDatePicker.show()
                }
            }

            FormSection(title = "Payment", isDarkTheme = isDarkTheme) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Price") },
                    readOnly = (selectedPlan != "Custom Days" && priceInput.isNotBlank()),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(value = discountInput, onValueChange = { discountInput = it }, label = { Text("Discount (optional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = finalAmountInput, onValueChange = { }, readOnly = true, label = { Text("Final Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountReceivedInput, onValueChange = { amountReceivedInput = it }, label = { Text("Amount Received") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = dueAdvanceInput, onValueChange = {}, readOnly = true, label = { Text("Due / Advance") }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPhotoPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerDialog = false },
            title = { Text("Select Option") },
            text = {
                Column {
                    Text("Take Photo", modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showPhotoPickerDialog = false; launchCameraWithCheck()
                        }
                        .padding(12.dp))
                    Text("Choose from Gallery", modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showPhotoPickerDialog = false; pickImageFromGallery()
                        }
                        .padding(12.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showPhotoPickerDialog = false
                }) { Text("Cancel") }
            })
    }
}

@Composable
private fun FormSection(
    title: String,
    isDarkTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = if (isDarkTheme) {
        CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    }

    val cardBorder = if (isDarkTheme) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (isDarkTheme) 0.dp else 2.dp),
        colors = cardColors,
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}


@Composable
private fun DatePickerField(label: String, value: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = onClick) { Icon(Icons.Default.DateRange, contentDescription = "Select Date") } })
        Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable(onClick = onClick))
    }
}

fun grantUriPermissionsForCamera(context: Context, uri: Uri) { val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE); val resInfoList = context.packageManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY); for (resolveInfo in resInfoList) { val packageName = resolveInfo.activityInfo.packageName; context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
fun calculateExpiryDate(startDate: Long, plan: String): Long { val monthsToAdd = plan.split(" ").getOrNull(0)?.toIntOrNull() ?: 0; if (monthsToAdd == 0) return startDate; val calendar = Calendar.getInstance(); calendar.timeInMillis = startDate; calendar.add(Calendar.MONTH, monthsToAdd); return calendar.timeInMillis }