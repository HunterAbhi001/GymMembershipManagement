package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.common.MemberListItem
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(members) { member ->
                    // Use DateUtils startOfDay so today shows "expires today"
                    val daysRemaining = run {
                        val todayStart = DateUtils.startOfDayMillis()
                        val diff = member.expiryDate - todayStart
                        TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).coerceAtLeast(0)
                    }

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
