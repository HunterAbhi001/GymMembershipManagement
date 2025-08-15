package com.example.gymmanagement.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.common.MemberListItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllMembersListScreen(
    navController: NavController,
    members: List<Member>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onImport: (Uri) -> Unit,
    onExport: (Uri) -> Unit
) {
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { onImport(it) }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? ->
            uri?.let { onExport(it) }
        }
    )

    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Members") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                                importLauncher.launch("*/*")
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to CSV") },
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                exportLauncher.launch("members_$timestamp.csv")
                                menuExpanded = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_edit_member") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search Members") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(members) { member ->
                    MemberListItem(
                        member = member,
                        onClick = { navController.navigate("member_details/${member.id}") }
                    )
                }
            }
        }
    }
}