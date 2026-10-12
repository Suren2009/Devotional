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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradarshana.app.ui.theme.LocalTempleColors
import com.pradarshana.core.model.SessionKind
import kotlin.math.cos
import kotlin.math.sin

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

/** A loop of prayer beads with a larger guru bead and tassel. Hidden from accessibility. */
@Composable
fun JapaMala(modifier: Modifier = Modifier, size: Dp = 168.dp) {
    val gold = LocalTempleColors.current.gold
    val saffron = LocalTempleColors.current.saffron
    Box(modifier.size(size).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            val centerX = center.x
            val centerY = center.y - radius * 0.04f
            val orbit = radius * 0.62f
            val beadRadius = radius * 0.075f
            drawCircle(
                color = gold.copy(alpha = 0.4f),
                radius = orbit,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.25.dp.toPx()),
            )
            val beadCount = 20
            val gapDegrees = 28.0
            val sweep = 360.0 - gapDegrees
            val start = 90.0 + gapDegrees / 2.0
            repeat(beadCount) { index ->
                val degrees = start + sweep * index / (beadCount - 1)
                val angle = Math.toRadians(degrees)
                val bead = Offset(
                    centerX + cos(angle).toFloat() * orbit,
                    centerY + sin(angle).toFloat() * orbit,
                )
                drawCircle(gold.copy(alpha = 0.92f), beadRadius, bead)
                drawCircle(saffron.copy(alpha = 0.45f), beadRadius * 0.42f, bead)
            }
            val guruAngle = Math.toRadians(90.0)
            val guru = Offset(
                centerX + cos(guruAngle).toFloat() * orbit,
                centerY + sin(guruAngle).toFloat() * orbit,
            )
            drawCircle(saffron, beadRadius * 1.85f, guru)
            drawCircle(gold, beadRadius * 0.65f, guru)
            val tasselTop = guru.y + beadRadius * 1.85f
            val tasselLength = radius * 0.2f
            drawLine(
                saffron,
                Offset(guru.x, tasselTop),
                Offset(guru.x, tasselTop + tasselLength),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                gold,
                Offset(guru.x, tasselTop),
                Offset(guru.x - radius * 0.08f, tasselTop + tasselLength * 0.82f),
                strokeWidth = 1.3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                gold,
                Offset(guru.x, tasselTop),
                Offset(guru.x + radius * 0.08f, tasselTop + tasselLength * 0.82f),
                strokeWidth = 1.3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Japa mala while chanting; the temple mandala for a Pradarshana. */
@Composable
fun SessionMark(kind: SessionKind, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val chanting = kind == SessionKind.CHANTING
    Box(modifier.testTag(if (chanting) "japa_mala" else "mandala")) {
        if (chanting) JapaMala(size = size) else Mandala(size = size)
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
