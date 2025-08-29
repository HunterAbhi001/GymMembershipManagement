package com.example.gymmanagement.ui.screens

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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymmanagement.data.database.Member
import androidx.compose.ui.graphics.Color
import com.example.gymmanagement.ui.theme.RedAccent
import com.example.gymmanagement.ui.theme.Green
import com.example.gymmanagement.ui.theme.Red
import com.example.gymmanagement.ui.utils.DateUtils
import com.example.gymmanagement.ui.utils.DateUtils.toDateString
import java.util.concurrent.TimeUnit

/**
 * The master, reusable list item for displaying a member.
 * It provides a consistent look and feel for all member lists.
 * The `trailingContent` slot makes it flexible for different screens.
 */
@Composable
fun MemberListItem(
    member: Member,
    onClick: () -> Unit,
    trailingContent: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Using AsyncImage for better placeholder/error handling
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

/**
 * A helper composable that displays the smart expiry status text.
 */
// In CommonComposables.kt

/**
 * A helper composable that displays the smart expiry status text.
 * It includes logic for expired, today, tomorrow, and future dates with color coding.
 */
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

    // Determine color based on urgency
    val daysRemaining = if (isExpired) -1 else TimeUnit.MILLISECONDS.toDays(member.expiryDate - todayStart)
    val statusColor = when {
        isExpired -> MaterialTheme.colorScheme.error
        daysRemaining <= 3 -> RedAccent // Or another urgent color
        daysRemaining <= 7 -> Color(0xFFFFC107) // Amber/Warning color
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = expiryText,
        style = MaterialTheme.typography.bodySmall,
        color = statusColor,
        fontWeight = if (daysRemaining <= 7) FontWeight.SemiBold else FontWeight.Normal
    )
}