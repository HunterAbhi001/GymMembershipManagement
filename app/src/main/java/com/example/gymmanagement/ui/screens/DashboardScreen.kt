package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.theme.GreenAccent
import com.example.gymmanagement.ui.theme.RedAccent
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.utils.sendSmsMessage
import com.example.gymmanagement.ui.utils.sendWhatsAppMessage
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    allMembers: List<Member>,
    membersExpiringSoon: List<Member>,
    expiredMembers: List<Member>,
    onDeleteMember: (Member) -> Unit,
    todaysRevenue: Double = 0.0,
    totalBalance: Double = 0.0,
    totalDues: Double = 0.0
) {
    val context = LocalContext.current
    val todayStart = DateUtils.startOfDayMillis()
    val activeMemberCount = allMembers.count { it.expiryDate >= todayStart }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${memberToDelete!!.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMember(memberToDelete!!)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gym Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("search_members") }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Members")
                    }
                }
            )
        },
        floatingActionButton = {},
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        label = "Active",
                        count = activeMemberCount.toString(),
                        icon = Icons.Default.Group,
                        iconColor = Color(0xFF4CAF50),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("active_members_list") }
                    )
                    StatCard(
                        label = "Expiring Soon",
                        count = membersExpiringSoon.size.toString(),
                        icon = Icons.Default.Warning,
                        iconColor = Color(0xFFFFC107),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("expiring_members") }
                    )
                    StatCard(
                        label = "Expired",
                        count = expiredMembers.size.toString(),
                        icon = Icons.Default.PersonOff,
                        iconColor = RedAccent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("expired_members_list") }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        label = "Today's Revenue",
                        count = formatCurrencyShort(todaysRevenue),
                        icon = Icons.Default.CurrencyRupee,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("todays_revenue") }
                    )
                    StatCard(
                        // --- FIX: Reverted label to "Dues" ---
                        label = "Dues",
                        // --- FIX: Using totalDues which only calculates negative balances ---
                        count = formatCurrencyShort(totalDues),
                        icon = Icons.Default.ReceiptLong,
                        iconColor = Color(0xFFFFC107),
                        // --- FIX: Count color is now always red if there are dues ---
                        countColor = if (totalDues < 0) RedAccent else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("dues_advance") }
                    )
                    StatCard(
                        label = "Collections",
                        count = formatCurrencyShort(totalBalance),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("collections") }
                    )
                }
            }
            item {
                ActionCard(
                    label = "Add New Member",
                    icon = Icons.Default.PersonAdd,
                    onClick = { navController.navigate("add_edit_member") }
                )
            }
            item {
                ActionCard(
                    label = "Manage All Members",
                    icon = Icons.Default.People,
                    onClick = { navController.navigate("all_members_list") }
                )
            }
            item {
                ActionCard(
                    label = "Analytics & Reports",
                    icon = Icons.Default.Analytics,
                    onClick = { navController.navigate("analytics") }
                )
            }
            item {
                Text(
                    "Memberships Expiring Soon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            if (membersExpiringSoon.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎉 No members are expiring soon!", color = Color.Gray)
                    }
                }
            } else {
                items(membersExpiringSoon.take(5)) { member ->
                    ExpiringMemberItem(
                        member = member,
                        onSmsClick = { message ->
                            sendSmsMessage(
                                context,
                                member.contact,
                                message
                            )
                        },
                        onWhatsAppClick = { message ->
                            sendWhatsAppMessage(
                                context,
                                member.contact,
                                message
                            )
                        },
                        onClick = { navController.navigate("member_details/${member.id}") },
                        onRenewClick = { navController.navigate("add_edit_member?memberId=${member.id}&isRenewal=true") },
                        onEditClick = { navController.navigate("add_edit_member?memberId=${member.id}&isRenewal=false") },
                        onDeleteClick = { memberToDelete = member }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    count: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    countColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = countColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ExpiringMemberItem(
    member: Member,
    onSmsClick: (String) -> Unit,
    onWhatsAppClick: (String) -> Unit,
    onClick: () -> Unit,
    onRenewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val todayStart = DateUtils.startOfDayMillis()
    val diff = member.expiryDate - todayStart
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
    var showMemberMenu by remember { mutableStateOf(false) }

    val expiryText = when (daysRemaining) {
        0L -> "Expires today"
        1L -> "Expires in 1 day"
        else -> "Expires in $daysRemaining days"
    }

    val message = "Hi ${
        member.name.split(" ").firstOrNull() ?: ""
    }, a friendly reminder that your gym membership ${
        if (daysRemaining == 0L) "expires today" else "is expiring in $daysRemaining days"
    }. Please visit the front desk to renew. Thank you!"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
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

            Box {
                IconButton(onClick = { showMemberMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.Gray
                    )
                }
                DropdownMenu(
                    expanded = showMemberMenu,
                    onDismissRequest = { showMemberMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Send SMS") },
                        leadingIcon = { Icon(Icons.Default.Sms, contentDescription = "SMS") },
                        onClick = {
                            onSmsClick(message)
                            showMemberMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Send WhatsApp") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = "WhatsApp"
                            )
                        },
                        onClick = {
                            onWhatsAppClick(message)
                            showMemberMenu = false
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Renew") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Autorenew,
                                contentDescription = "Renew"
                            )
                        },
                        onClick = {
                            onRenewClick()
                            showMemberMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                        onClick = {
                            onEditClick()
                            showMemberMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                        onClick = {
                            onDeleteClick()
                            showMemberMenu = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatCurrencyShort(value: Double): String {
    val absValue = abs(value)
    val sign = if (value < 0) "-" else ""
    val currencySymbol = "₹"

    val formatter = { num: Double ->
        if (num % 1.0 == 0.0) {
            num.toLong().toString()
        } else {
            String.format("%.1f", num)
        }
    }

    return when {
        absValue < 1000 -> {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.maximumFractionDigits = 0
            format.currency = Currency.getInstance("INR")
            format.format(value)
        }

        absValue < 1_00_000 -> {
            val thousands = absValue / 1000.0
            "$sign$currencySymbol${formatter(thousands)}K"
        }

        else -> {
            val lakhs = absValue / 1_00_000.0
            "$sign$currencySymbol${formatter(lakhs)}L"
        }
    }
}
