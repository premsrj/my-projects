package dev.prem.foodtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ProgressStatus {
    UNDER,
    NEAR,
    OVER
}

fun progressStatusFor(progress: Float): ProgressStatus {
    return when {
        progress < 0.9f -> ProgressStatus.UNDER
        progress <= 1.05f -> ProgressStatus.NEAR
        else -> ProgressStatus.OVER
    }
}

@Composable
fun DonutChart(
    progress: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    thicknessDp: Float = 14f,
    status: ProgressStatus = progressStatusFor(progress)
) {
    val normalizedProgress = progress.coerceAtLeast(0f)
    val primaryProgress = normalizedProgress.coerceAtMost(1f)
    val overflowProgress = (normalizedProgress - 1f).coerceAtLeast(0f).coerceAtMost(1f)

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val centerColor = MaterialTheme.colorScheme.surface
    val progressColor = when (status) {
        ProgressStatus.UNDER -> MaterialTheme.colorScheme.primary
        ProgressStatus.NEAR -> MaterialTheme.colorScheme.tertiary
        ProgressStatus.OVER -> MaterialTheme.colorScheme.error
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val stroke = Stroke(width = thicknessDp.dp.toPx(), cap = StrokeCap.Round)
            val glowStroke = Stroke(width = (thicknessDp + 6f).dp.toPx(), cap = StrokeCap.Round)
            val overflowStroke = Stroke(width = (thicknessDp * 0.55f).dp.toPx(), cap = StrokeCap.Round)
            val topLeft = Offset(stroke.width / 2f, stroke.width / 2f)
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            if (primaryProgress > 0f) {
                drawArc(
                    color = progressColor.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f * primaryProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = glowStroke
                )
            }

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * primaryProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            if (overflowProgress > 0f) {
                drawArc(
                    color = progressColor.copy(alpha = 0.55f),
                    startAngle = -90f,
                    sweepAngle = 360f * overflowProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = overflowStroke
                )
            }

            drawCircle(
                color = centerColor,
                radius = (size.minDimension / 2f) - (thicknessDp.dp.toPx() * 0.95f)
            )
        }

        Text(
            text = centerText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
