package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.icons.MessagesIcon
import com.example.gymmanagement.ui.icons.WhatsAppIcon
import com.example.gymmanagement.ui.theme.RedAccent
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.utils.sendSmsMessage
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiringMembersScreen(
    navController: NavController,
    members: List<Member>
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredMembers = remember(searchQuery, members) {
        if (searchQuery.isBlank()) {
            members
        } else {
            members.filter { member ->
                member.name.contains(searchQuery, ignoreCase = true) ||
                        member.contact.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expiring Soon") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Expiring Members") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(filteredMembers) { member ->
                    ExpiringMemberListItem(
                        member = member,
                        onSmsClick = { message -> sendSmsMessage(context, member.contact, message) },
                        onWhatsAppClick = { message -> sendWhatsAppMessage(context, member.contact, message) },
                        onClick = { navController.navigate("member_details/${member.id}") }
                    )
                }
            }
        }
    }
}


@Composable
fun ExpiringMemberListItem(
    member: Member,
    onSmsClick: (String) -> Unit,
    onWhatsAppClick: (String) -> Unit,
    onClick: () -> Unit
) {
    val todayStart = DateUtils.startOfDayMillis()
    val diff = member.expiryDate - todayStart
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)

    val firstName = remember(member.name) {
        member.name.split(" ").firstOrNull() ?: member.name
    }

    val expiryText = when (daysRemaining) {
        0L -> "Expires today"
        1L -> "Expires in 1 day"
        else -> "Expires in $daysRemaining days"
    }

    // The message still uses the first name for a personal touch
    val message = "Hi $firstName, a friendly reminder that your gym membership ${
        if (daysRemaining == 0L) "expires today" else "is expiring in $daysRemaining days"
    }. Please visit the front desk to renew. Thank you!"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.firstOrNull()?.toString() ?: "",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // --- FIX: Display the member's full name on the screen ---
                    text = member.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = expiryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysRemaining <= 3) RedAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onSmsClick(message) }) {
                Icon(
                    imageVector = MessagesIcon,
                    contentDescription = "Send SMS",
                    tint = Color.Unspecified
                )
            }
            IconButton(onClick = { onWhatsAppClick(message) }) {
                Icon(
                    imageVector = WhatsAppIcon,
                    contentDescription = "Send WhatsApp",
                    tint = Color.Unspecified
                )
            }
        }
    }
}
