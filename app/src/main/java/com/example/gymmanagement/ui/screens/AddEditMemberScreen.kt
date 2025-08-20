package com.example.gymmanagement.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.utils.ComposeFileProvider
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberScreen(
    navController: NavController,
    member: Member?,
    onSave: (Member) -> Unit,
    isRenewal: Boolean = false
) {
    // Basic fields
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }

    // Date/time fields
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var expiryDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Financial fields
    var priceInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var finalAmountInput by remember { mutableStateOf("") } // Auto-calculated
    var amountReceivedInput by remember { mutableStateOf("") } // NEW: User input for payment
    var dueAdvanceInput by remember { mutableStateOf("") } // Auto-calculated

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun parseDoubleOrNull(text: String): Double? {
        return text.trim().takeIf { it.isNotBlank() }?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull()
    }

    LaunchedEffect(member, isRenewal) {
        if (member == null) {
            // New member defaults
            startDate = System.currentTimeMillis()
            expiryDate = calculateExpiryDate(startDate, selectedPlan)
        } else {
            // Editing existing member
            name = member.name
            contact = member.contact
            selectedPlan = member.plan
            startDate = member.startDate
            expiryDate = member.expiryDate
            gender = member.gender ?: ""
            batch = member.batch ?: ""
            priceInput = member.price?.toString() ?: ""
            discountInput = member.discount?.toString() ?: ""
            finalAmountInput = member.finalAmount?.toString() ?: ""
            amountReceivedInput = member.finalAmount?.toString() ?: ""
            dueAdvanceInput = member.dueAdvance?.toString() ?: ""
            photoUri = member.photoUri?.let(Uri::parse)

            if (isRenewal) {
                startDate = member.expiryDate
                expiryDate = calculateExpiryDate(startDate, selectedPlan)
                priceInput = ""
                discountInput = ""
                finalAmountInput = ""
                amountReceivedInput = ""
                dueAdvanceInput = ""
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

    val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
    val startDatePicker = DatePickerDialog(context, { _: DatePicker, y: Int, m: Int, d: Int -> val newDate = Calendar.getInstance().apply { set(y, m, d) }; startDate = newDate.timeInMillis; expiryDate = calculateExpiryDate(startDate, selectedPlan) }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH))
    val expiryCal = Calendar.getInstance().apply { timeInMillis = expiryDate }
    val expiryDatePicker = DatePickerDialog(context, { _: DatePicker, y: Int, m: Int, d: Int -> expiryDate = Calendar.getInstance().apply { set(y, m, d) }.timeInMillis }, expiryCal.get(Calendar.YEAR), expiryCal.get(Calendar.MONTH), expiryCal.get(Calendar.DAY_OF_MONTH))

    val plans = (1..12).map { "$it Month${if (it > 1) "s" else ""}" }
    val genders = listOf("Male", "Female", "Other")
    val batches = listOf("Morning", "Evening")
    var planExpanded by remember { mutableStateOf(false) }

    val isFormValid by remember { derivedStateOf { name.isNotBlank() && contact.isNotBlank() && selectedPlan.isNotBlank() && gender.isNotBlank() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text( when { member == null -> "Add Member"; isRenewal -> "Renew Member"; else -> "Edit Member" }) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            if (isFormValid) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = {
                        val updatedMember = (member?.copy(
                            name = name.trim(),
                            contact = contact.trim(),
                            plan = selectedPlan,
                            startDate = startDate,
                            expiryDate = expiryDate,
                            gender = gender,
                            photoUri = photoUri?.toString(),
                            batch = batch.ifBlank { null },
                            price = parseDoubleOrNull(priceInput),
                            discount = parseDoubleOrNull(discountInput),
                            finalAmount = parseDoubleOrNull(finalAmountInput),
                            purchaseDate = startDate,
                            dueAdvance = parseDoubleOrNull(dueAdvanceInput)
                        ) ?: Member(
                            name = name.trim(),
                            contact = contact.trim(),
                            plan = selectedPlan,
                            startDate = startDate,
                            expiryDate = expiryDate,
                            gender = gender,
                            photoUri = photoUri?.toString(),
                            batch = batch.ifBlank { null },
                            price = parseDoubleOrNull(priceInput),
                            discount = parseDoubleOrNull(discountInput),
                            finalAmount = parseDoubleOrNull(finalAmountInput),
                            purchaseDate = startDate,
                            dueAdvance = parseDoubleOrNull(dueAdvanceInput)
                        ))
                        onSave(updatedMember)
                        navController.popBackStack()
                    }) {
                        Text(when { member == null -> "Save"; isRenewal -> "Renew"; else -> "Update" })
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable { showPhotoPickerDialog = true }, contentAlignment = Alignment.Center) {
                if (photoUri != null) {
                    Image(painter = rememberAsyncImagePainter(photoUri), contentDescription = "Member Photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo", modifier = Modifier.size(48.dp))
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact (Phone)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)

            // --- NEW: Gender selection with FilterChips ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text("Gender", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    genders.forEach { item ->
                        FilterChip(
                            selected = (gender == item),
                            onClick = { gender = item },
                            label = { Text(item) }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = planExpanded, onExpandedChange = { planExpanded = !planExpanded }) {
                OutlinedTextField(value = selectedPlan, onValueChange = {}, readOnly = true, label = { Text("Membership Plan") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = planExpanded, onDismissRequest = { planExpanded = false }) {
                    plans.forEach { plan ->
                        DropdownMenuItem(text = { Text(plan) }, onClick = {
                            selectedPlan = plan
                            expiryDate = calculateExpiryDate(startDate, plan)
                            planExpanded = false
                        })
                    }
                }
            }

            // --- NEW: Batch selection with FilterChips ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text("Batch", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    batches.forEach { item ->
                        FilterChip(
                            selected = (batch == item),
                            onClick = { batch = item },
                            label = { Text(item) }
                        )
                    }
                }
            }

            // Price Fields
            OutlinedTextField(value = priceInput, onValueChange = { priceInput = it }, label = { Text("Price") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(value = discountInput, onValueChange = { discountInput = it }, label = { Text("Discount") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(value = finalAmountInput, onValueChange = { }, readOnly = true, label = { Text("Final Amount") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = amountReceivedInput, onValueChange = { amountReceivedInput = it }, label = { Text("Amount Received") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(value = dueAdvanceInput, onValueChange = {}, readOnly = true, label = { Text("Due / Advance") }, modifier = Modifier.fillMaxWidth())

            // Date Fields
            DatePickerField("Start Date", startDate.toDateString()) { startDatePicker.show() }
            DatePickerField("Expiry Date", expiryDate.toDateString()) { expiryDatePicker.show() }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (showPhotoPickerDialog) {
        AlertDialog(onDismissRequest = { showPhotoPickerDialog = false }, title = { Text("Select Option") }, text = { Column { Text("Take Photo", modifier = Modifier.fillMaxWidth().clickable { showPhotoPickerDialog = false; launchCameraWithCheck() }.padding(12.dp)); Text("Choose from Gallery", modifier = Modifier.fillMaxWidth().clickable { showPhotoPickerDialog = false; pickImageFromGallery() }.padding(12.dp)) } }, confirmButton = { TextButton(onClick = { showPhotoPickerDialog = false }) { Text("Cancel") } })
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
