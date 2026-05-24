package com.example.foldercleaner.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foldercleaner.data.CleanupRunEntity
import com.example.foldercleaner.util.SizeFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunHistoryScreen(
    runs: List<CleanupRunEntity>,
    onBackClick: () -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    }
    val totalReclaimedBytes = remember(runs) { runs.sumOf { it.reclaimedBytes } }
    val totalDeletedFiles = remember(runs) { runs.sumOf { it.deletedCount } }
    val trendPoints = remember(runs) { runs.take(14).asReversed().map { it.reclaimedBytes } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run history") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (runs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No cleanup runs yet.",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Automatic and manual runs will appear here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Storage reclaimed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = SizeFormatter.formatBytes(totalReclaimedBytes),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${runs.size} runs, $totalDeletedFiles files deleted",
                                style = MaterialTheme.typography.bodySmall
                            )
                            ReclaimedTrendChart(
                                values = trendPoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                            )
                        }
                    }
                }

                items(items = runs, key = { it.id }) { run ->
                    val runTime = Instant.ofEpochMilli(run.executedAtMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        .format(formatter)

                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = run.trigger.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = runTime,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Text(
                                text = "Deleted ${run.deletedCount} of ${run.scannedCount} scanned",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Reclaimed ${SizeFormatter.formatBytes(run.reclaimedBytes)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Skipped ${run.skippedCount}, failed ${run.failedCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReclaimedTrendChart(
    values: List<Long>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) {
        Text(
            text = "No chart data yet.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        return
    }

    val maxValue = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val lineColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPadding = 8f
            val rightPadding = 8f
            val topPadding = 8f
            val bottomPadding = 18f
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding
            val xStep = if (values.size > 1) chartWidth / (values.size - 1) else 0f

            drawLine(
                color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.35f),
                start = Offset(leftPadding, topPadding + chartHeight),
                end = Offset(size.width - rightPadding, topPadding + chartHeight),
                strokeWidth = 2f
            )

            val path = Path()
            values.forEachIndexed { index, value ->
                val x = leftPadding + (xStep * index)
                val normalized = value.toFloat() / maxValue.toFloat()
                val y = topPadding + chartHeight - (chartHeight * normalized)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4f)
            )

            values.forEachIndexed { index, value ->
                val x = leftPadding + (xStep * index)
                val normalized = value.toFloat() / maxValue.toFloat()
                val y = topPadding + chartHeight - (chartHeight * normalized)
                drawCircle(
                    color = lineColor,
                    center = Offset(x, y),
                    radius = 4f
                )
            }
        }

        Text(
            text = "Max ${SizeFormatter.formatBytes(maxValue)}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 6.dp, top = 4.dp)
        )
    }
}
