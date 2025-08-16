package com.example.gymmanagement.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.ui.theme.AppIcons
import com.example.gymmanagement.ui.theme.Green
import com.example.gymmanagement.ui.theme.Red
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import java.util.concurrent.TimeUnit

@Composable
fun MemberListItem(
    member: Member,
    onClick: () -> Unit,
    onWhatsAppClick: (() -> Unit)? = null,
    onSmsClick: (() -> Unit)? = null
) {
    val todayStart = DateUtils.startOfDayMillis()
    val todayEnd = DateUtils.endOfDayMillis()

    val isExpired = member.expiryDate < todayStart
    val isExpiringToday = member.expiryDate in todayStart..todayEnd

    val statusColor = if (isExpired) Red else Green

    val expiryText = when {
        isExpired -> "Expired on ${member.expiryDate.toDateString()}"
        isExpiringToday -> "Expires today"
        else -> {
            // Compute days relative to todayStart so "tomorrow" is 1
            val diff = member.expiryDate - todayStart
            val daysRemaining = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).coerceAtLeast(0)
            "Expires in $daysRemaining days"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = member.photoUri,
            contentDescription = "Member Photo",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentScale = ContentScale.Crop,
            placeholder = rememberVectorPainter(Icons.Default.Person),
            error = rememberVectorPainter(Icons.Default.Person)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                text = expiryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onSmsClick != null) {
            IconButton(onClick = onSmsClick) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = "Send SMS",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
        if (onWhatsAppClick != null) {
            IconButton(onClick = onWhatsAppClick) {
                Icon(
                    imageVector = AppIcons.WhatsApp,
                    contentDescription = "Send WhatsApp Message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
