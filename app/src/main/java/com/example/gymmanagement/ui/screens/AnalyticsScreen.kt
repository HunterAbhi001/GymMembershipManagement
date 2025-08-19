package com.example.gymmanagement.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.math.min

// Data classes
data class MonthlySignupData(val month: String, val count: Float)
data class PlanPopularityData(val planName: String, val count: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    monthlySignups: List<MonthlySignupData>,
    planPopularity: List<PlanPopularityData>,
    activeCount: Int,
    expiredCount: Int
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { NewMembersChart(monthlySignups) }
            item { PlanPopularityChart(planPopularity) }
            item { ActiveVsExpiredChart(activeCount, expiredCount) }
        }
    }
}

@Composable
private fun NewMembersChart(data: List<MonthlySignupData>) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "New Members Per Month",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (data.isEmpty()) {
                Text("Not enough data to display chart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            val maxVal = (data.maxOfOrNull { it.count } ?: 1f).coerceAtLeast(1f)
            val barMaxHeight = 180.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barMaxHeight + 50.dp),
                // --- FIX: Removed fixed spacing to allow weights to work correctly ---
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.count.toInt().toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val ratio = (item.count / maxVal).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .height(barMaxHeight * ratio)
                                .width(20.dp) // Slightly reduced width for better spacing
                                .background(
                                    color = colors[index % colors.size],
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val monthParts = item.month.split(" ")
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthParts.getOrElse(0) { "" },
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            if (monthParts.size > 1) {
                                Text(
                                    text = monthParts.getOrElse(1) { "" },
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Max: ${"%.0f".format(maxVal)}", style = MaterialTheme.typography.bodySmall)
                Text("Total: ${"%.0f".format(data.sumOf { it.count.toDouble() })}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlanPopularityChart(data: List<PlanPopularityData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Membership Plan Popularity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (data.isEmpty()) {
                Text("Not enough data to display chart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            val items = data.map { it.planName to it.count }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PieWithLegend(items = items, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActiveVsExpiredChart(active: Int, expired: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Active vs. Expired Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (active <= 0 && expired <= 0) {
                Text("Not enough data to display chart.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            val items = listOf("Active" to active.toFloat(), "Expired" to expired.toFloat())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PieWithLegend(items = items, modifier = Modifier.weight(1f), donut = true)
            }
        }
    }
}

@Composable
private fun PieWithLegend(
    items: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    donut: Boolean = false
) {
    val total = items.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)

    val sliceColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )
    val backgroundColor = MaterialTheme.colorScheme.background
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val diameter = min(w, h)
                val padding = 4f
                val rectSize = Size(diameter - padding * 2f, diameter - padding * 2f)
                var startAngle = -90f
                items.forEachIndexed { index, (_, value) ->
                    val sweep = (value / total) * 360f
                    drawArc(
                        color = sliceColors[index % sliceColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
                        size = rectSize
                    )
                    startAngle += sweep
                }

                if (donut) {
                    val innerFraction = 0.55f
                    val innerSize = Size(rectSize.width * innerFraction, rectSize.height * innerFraction)
                    val offset = androidx.compose.ui.geometry.Offset(
                        (rectSize.width - innerSize.width) / 2f + padding,
                        (rectSize.height - innerSize.height) / 2f + padding
                    )
                    drawArc(
                        color = backgroundColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = offset,
                        size = innerSize
                    )
                }
            }

            if (donut) {
                Text(
                    text = total.toInt().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onBackgroundColor
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { index, (label, value) ->
                val pct = if (total > 0f) (value / total) * 100f else 0f
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(sliceColors[index % sliceColors.size], shape = RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Text("${"%.1f".format(pct)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
