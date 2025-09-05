package com.example.gymmanagement.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymmanagement.R
import com.example.gymmanagement.data.database.Member
import com.example.gymmanagement.data.database.Payment
import com.example.gymmanagement.ui.theme.RedAccent
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// Data class for combined Transaction UI state
data class UiTransaction(
    val paymentDetails: Payment,
    val memberPhotoUri: String?
)

@Composable
fun MemberListItem(
    member: Member,
    onClick: () -> Unit,
    trailingContent: @Composable (ColumnScope.() -> Unit)
) {
    val haptics = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = member.photoUri,
                contentDescription = "Member Photo",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.Person),
                error = rememberVectorPainter(Icons.Default.Person)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = member.contact,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                trailingContent()
            }
        }
    }
}

@Composable
fun ExpiryStatusText(member: Member) {
    val todayStart = DateUtils.startOfDayMillis()
    val isExpired = member.expiryDate < todayStart

    val expiryText = when {
        isExpired -> "Expired on ${member.expiryDate.toDateString()}"
        else -> {
            val diff = member.expiryDate - todayStart
            val daysRemaining = TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
            when (daysRemaining) {
                0L -> "Expires today"
                1L -> "Expires in 1 day"
                else -> "Expires in $daysRemaining days"
            }
        }
    }

    val daysRemaining = if (isExpired) -1 else TimeUnit.MILLISECONDS.toDays(member.expiryDate - todayStart)
    val statusColor = when {
        isExpired -> RedAccent
        daysRemaining <= 3 -> RedAccent
        daysRemaining <= 7 -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = expiryText,
        style = MaterialTheme.typography.bodySmall,
        color = statusColor,
        fontWeight = if (daysRemaining <= 7) FontWeight.SemiBold else FontWeight.Normal
    )
}

@Composable
fun TransactionListItem(
    transaction: UiTransaction,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (transaction.paymentDetails.memberId.isNotBlank()) {
                    onClick()
                } else {
                    Toast
                        .makeText(context, "Cannot open details for this transaction.", Toast.LENGTH_SHORT)
                        .show()
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- UPDATED: Using AsyncImage for consistency ---
            AsyncImage(
                model = transaction.memberPhotoUri,
                contentDescription = "Member Photo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.Person),
                error = rememberVectorPainter(Icons.Default.Person)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.paymentDetails.memberName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.paymentDetails.type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatCurrency(transaction.paymentDetails.amount),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    format.maximumFractionDigits = 2
    return format.format(value)
}