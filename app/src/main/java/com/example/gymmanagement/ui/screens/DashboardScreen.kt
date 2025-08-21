package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Brightness7
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    totalDues: Double = 0.0,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val todayStart = DateUtils.startOfDayMillis()
    val activeMemberCount = allMembers.count { it.expiryDate >= todayStart }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    // --- Gradient brushes for the cards (these look good in both light and dark mode) ---
    val activeGradient = Brush.verticalGradient(listOf(Color(0xFF66BB6A), Color(0xFF388E3C)))
    val expiringGradient = Brush.verticalGradient(listOf(Color(0xFFFFCA28), Color(0xFFFFA000)))
    val expiredGradient = Brush.verticalGradient(listOf(Color(0xFFEF5350), Color(0xFFD32F2F)))
    val revenueGradient = Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFF1976D2)))
    val duesGradient = Brush.verticalGradient(listOf(Color(0xFFFFA726), Color(0xFFF57C00)))
    val collectionsGradient = Brush.verticalGradient(listOf(Color(0xFF26A69A), Color(0xFF00796B)))


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

    // --- FIXED: Changed to a vertical gradient to resolve the compiler error ---
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
                title = { Text("Gym Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) Color.Transparent else MaterialTheme.colorScheme.surface,
                    titleContentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("search_members") }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Members")
                    }
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.Brightness7 else Icons.Outlined.Brightness4,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = { navController.navigate("settings_screen") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize().then(backgroundModifier)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        gradient = activeGradient,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("active_members_list") }
                    )
                    StatCard(
                        label = "Expiring Soon",
                        count = membersExpiringSoon.size.toString(),
                        icon = Icons.Default.Warning,
                        gradient = expiringGradient,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("expiring_members") }
                    )
                    StatCard(
                        label = "Expired",
                        count = expiredMembers.size.toString(),
                        icon = Icons.Default.PersonOff,
                        gradient = expiredGradient,
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
                        gradient = revenueGradient,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("todays_revenue") }
                    )
                    StatCard(
                        label = "Dues",
                        count = formatCurrencyShort(totalDues),
                        icon = Icons.Default.ReceiptLong,
                        gradient = duesGradient,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { navController.navigate("dues_advance") }
                    )
                    StatCard(
                        label = "Collections",
                        count = formatCurrencyShort(totalBalance),
                        icon = Icons.Default.AccountBalanceWallet,
                        gradient = collectionsGradient,
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
                    isDarkTheme = isDarkTheme,
                    onClick = { navController.navigate("add_edit_member") }
                )
            }
            item {
                ActionCard(
                    label = "Manage All Members",
                    icon = Icons.Default.People,
                    isDarkTheme = isDarkTheme,
                    onClick = { navController.navigate("all_members_list") }
                )
            }
            item {
                ActionCard(
                    label = "Analytics & Reports",
                    icon = Icons.Default.Analytics,
                    isDarkTheme = isDarkTheme,
                    onClick = { navController.navigate("analytics") }
                )
            }
            item {
                Text(
                    "Memberships Expiring Soon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
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
                        isDarkTheme = isDarkTheme,
                        onSmsClick = { message -> sendSmsMessage(context, member.contact, message) },
                        onWhatsAppClick = { message -> sendWhatsAppMessage(context, member.contact, message) },
                        onClick = { navController.navigate("member_details/${member.idString}") },
                        onRenewClick = { navController.navigate("add_edit_member?memberId=${member.idString}&isRenewal=true") },
                        onEditClick = { navController.navigate("add_edit_member?memberId=${member.idString}&isRenewal=false") },
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
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .clickable(onClick = onClick)
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
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ActionCard(
    label: String,
    icon: ImageVector,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardModifier = if (isDarkTheme) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    } else {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    }

    Box(
        modifier = cardModifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = (if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ExpiringMemberItem(
    member: Member,
    isDarkTheme: Boolean,
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

    val borderColor = if (daysRemaining <= 3) RedAccent else if (isDarkTheme) Color(0xFFFFCA28) else MaterialTheme.colorScheme.primary

    val message = "Hi ${member.name.split(" ").firstOrNull() ?: ""}, a friendly reminder that your gym membership ${
        if (daysRemaining == 0L) "expires today" else "is expiring in $daysRemaining days"
    }. Please visit the front desk to renew. Thank you!"

    val cardModifier = if (isDarkTheme) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    }

    Box(
        modifier = cardModifier.clickable(onClick = onClick)
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
                    .background(borderColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.firstOrNull()?.toString() ?: "",
                    color = Color.White,
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
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showMemberMenu,
                    onDismissRequest = { showMemberMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Send SMS") },
                        leadingIcon = { Icon(MessagesIcon, contentDescription = "SMS", tint = Color.Unspecified) },
                        onClick = {
                            onSmsClick(message)
                            showMemberMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Send WhatsApp") },
                        leadingIcon = { Icon(WhatsAppIcon, contentDescription = "WhatsApp", tint = Color.Unspecified) },
                        onClick = {
                            onWhatsAppClick(message)
                            showMemberMenu = false
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Renew") },
                        leadingIcon = { Icon(Icons.Default.Autorenew, contentDescription = "Renew") },
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
