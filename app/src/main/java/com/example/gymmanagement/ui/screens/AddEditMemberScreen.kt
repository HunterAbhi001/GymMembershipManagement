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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onSave: (Member) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var gender by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(member) {
        member?.let {
            name = it.name
            contact = it.contact
            selectedPlan = it.plan
            startDate = it.startDate
            gender = it.gender ?: ""
            photoUri = it.photoUri?.let(Uri::parse)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                photoUri = tempUri
            }
        }
    )

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val newUri = ComposeFileProvider.getImageUri(context)
                grantUriPermissionsForCamera(context, newUri)
                tempUri = newUri
                cameraLauncher.launch(newUri)
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Gallery launcher with persistable permission
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
                photoUri = it
            }
        }
    )

    // Storage permission
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Permission to access photos denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    )

    fun pickImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                galleryLauncher.launch(arrayOf("image/*"))
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                galleryLauncher.launch(arrayOf("image/*"))
            }
        } else {
            galleryLauncher.launch(arrayOf("image/*"))
        }
    }

    fun launchCameraWithCheck() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val newUri = ComposeFileProvider.getImageUri(context)
            grantUriPermissionsForCamera(context, newUri)
            tempUri = newUri
            cameraLauncher.launch(newUri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Date picker
    val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            startDate = newDate.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val plans = (1..12).map { "$it Month${if (it > 1) "s" else ""}" }
    val genders = listOf("Male", "Female", "Other")
    var planExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    val isFormValid by remember {
        derivedStateOf {
            name.isNotBlank() &&
                    contact.isNotBlank() &&
                    selectedPlan.isNotBlank() &&
                    gender.isNotBlank()
        }
    }

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
            FloatingActionButton(
                onClick = {
                    val expiryDate = calculateExpiryDate(startDate, selectedPlan)
                    val newOrUpdatedMember = member?.copy(
                        name = name.trim(),
                        contact = contact.trim(),
                        plan = selectedPlan,
                        startDate = startDate,
                        expiryDate = expiryDate,
                        gender = gender,
                        photoUri = photoUri?.toString()
                    ) ?: Member(
                        name = name.trim(),
                        contact = contact.trim(),
                        plan = selectedPlan,
                        startDate = startDate,
                        expiryDate = expiryDate,
                        gender = gender,
                        photoUri = photoUri?.toString()
                    )
                    onSave(newOrUpdatedMember)
                    navController.popBackStack()
                },
                containerColor = if (isFormValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (isFormValid) {
                    Icon(Icons.Default.Save, contentDescription = "Save Member")
                } else {
                    Text("Save", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo picker
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { showPhotoPickerDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Member Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Add Photo",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Contact (Phone with Country Code)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

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
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
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
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
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

    // Compose AlertDialog for choosing photo source
    if (showPhotoPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerDialog = false },
            title = { Text("Select Option") },
            text = {
                Column {
                    Text(
                        "Take Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoPickerDialog = false
                                launchCameraWithCheck()
                            }
                            .padding(8.dp)
                    )
                    Text(
                        "Choose from Gallery",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoPickerDialog = false
                                pickImageFromGallery()
                            }
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {}
        )
    }
}

fun grantUriPermissionsForCamera(context: Context, uri: Uri) {
    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val resInfoList =
        context.packageManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
    for (resolveInfo in resInfoList) {
        val packageName = resolveInfo.activityInfo.packageName
        context.grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

fun calculateExpiryDate(startDate: Long, plan: String): Long {
    val monthsToAdd = plan.split(" ")[0].toIntOrNull() ?: 0
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startDate
    calendar.add(Calendar.MONTH, monthsToAdd)
    return calendar.timeInMillis
}
