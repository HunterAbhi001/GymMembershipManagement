package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.common.MemberListItem
import com.example.gymmanagement.ui.theme.Green
import com.example.gymmanagement.ui.theme.Orange
import com.example.gymmanagement.ui.theme.Red
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.utils.sendSmsMessage
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    allMembers: List<Member>,
    membersExpiringSoon: List<Member>,
    expiredMembers: List<Member>
) {
    val context = LocalContext.current

    val todayStart = DateUtils.startOfDayMillis()
    val todayEnd = DateUtils.endOfDayMillis()
    val sevenDaysEnd = DateUtils.endOfDayMillis(todayStart + TimeUnit.DAYS.toMillis(7))

    val activeMemberCount = allMembers.count { it.expiryDate >= todayStart }

    // Use the VM-provided membersExpiringSoon (already computed to cover today..7 days).
    // For dashboard preview, just show the VM list (which now uses startOfDay semantics).
    val previewExpiring = membersExpiringSoon

    val fixedExpired = allMembers.filter { it.expiryDate < todayStart }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gym Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { navController.navigate("add_edit_member") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
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
            // Stats Cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    label = "Active Members",
                    value = activeMemberCount.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("active_members_list") }
                )
                StatCard(
                    label = "Expiring Soon",
                    value = previewExpiring.size.toString(),
                    modifier = Modifier.weight(1f),
                    color = Orange,
                    onClick = { navController.navigate("expiring_members") }
                )
            }
            StatCard(
                label = "Expired Members",
                value = fixedExpired.size.toString(),
                modifier = Modifier.fillMaxWidth(),
                color = Red,
                onClick = { navController.navigate("expired_members_list") }
            )

            Button(onClick = { navController.navigate("all_members_list") }, modifier = Modifier.fillMaxWidth()) {
                Text("Manage All Members (Import/Export)")
            }

            // Expiring Soon List
            Text("Memberships Expiring Soon", style = MaterialTheme.typography.titleLarge)

            if (previewExpiring.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No memberships are expiring soon.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(previewExpiring.take(3)) { member ->
                        val todayStartLocal = DateUtils.startOfDayMillis()
                        val diff = member.expiryDate - todayStartLocal
                        val daysRemaining = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).coerceAtLeast(0)

                        val message = if (daysRemaining == 0L) {
                            "Hi ${member.name}, your gym membership expires today. Please renew to continue."
                        } else {
                            "Hi ${member.name}, your gym membership is expiring in $daysRemaining days. Please contact us to renew."
                        }

                        MemberListItem(
                            member = member,
                            onClick = { navController.navigate("member_details/${member.id}") },
                            onWhatsAppClick = { sendWhatsAppMessage(context, member.contact, message) },
                            onSmsClick = { sendSmsMessage(context, member.contact, message) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color = Green, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
