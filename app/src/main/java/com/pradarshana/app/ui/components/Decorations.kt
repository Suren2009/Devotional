package com.pradarshana.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradarshana.app.ui.theme.LocalTempleColors

/** Decorative lotus mandala with an Om at its centre. Purely ornamental, hidden from accessibility. */
@Composable
fun Mandala(modifier: Modifier = Modifier, size: Dp = 168.dp, showOm: Boolean = true) {
    val gold = LocalTempleColors.current.gold
    val saffron = LocalTempleColors.current.saffron
    Box(modifier.size(size).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2
            val stroke = Stroke(width = 1.5.dp.toPx())
            drawCircle(gold.copy(alpha = 0.55f), radius = r * 0.98f, style = stroke)
            drawCircle(gold.copy(alpha = 0.35f), radius = r * 0.88f, style = stroke)
            val petals = 16
            repeat(petals) { i ->
                rotate(360f / petals * i) {
                    drawOval(
                        color = saffron.copy(alpha = 0.16f),
                        topLeft = Offset(center.x - r * 0.12f, center.y - r * 0.86f),
                        size = androidx.compose.ui.geometry.Size(r * 0.24f, r * 0.5f),
                    )
                }
            }
            repeat(8) { i ->
                rotate(360f / 8 * i + 22.5f) {
                    drawOval(
                        color = gold.copy(alpha = 0.22f),
                        topLeft = Offset(center.x - r * 0.1f, center.y - r * 0.6f),
                        size = androidx.compose.ui.geometry.Size(r * 0.2f, r * 0.36f),
                    )
                }
            }
            drawCircle(gold.copy(alpha = 0.5f), radius = r * 0.3f, style = stroke)
        }
        if (showOm) {
            Text("ॐ", color = MaterialTheme.colorScheme.primary, fontSize = (size.value * 0.22f).sp)
        }
    }
}

/** Circular progress ring with rounded ends. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        if (progress > 0f) {
            drawArc(
                progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

val PauseIcon: ImageVector by lazy {
    materialIcon(name = "Filled.Pause") {
        materialPath {
            moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); close()
            moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); close()
        }
    }
}
