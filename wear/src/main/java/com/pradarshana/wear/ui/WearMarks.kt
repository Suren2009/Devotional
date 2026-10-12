package com.pradarshana.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.pradarshana.core.model.SessionKind
import com.pradarshana.wear.ui.theme.Gold
import com.pradarshana.wear.ui.theme.Saffron
import kotlin.math.cos
import kotlin.math.sin

/** Japa mala while chanting; the Om used on the watch home for a Pradarshana. */
@Composable
fun WearSessionMark(kind: SessionKind, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val chanting = kind == SessionKind.CHANTING
    Box(
        modifier.size(size).testTag(if (chanting) "japa_mala" else "mandala"),
        contentAlignment = Alignment.Center,
    ) {
        if (chanting) {
            Canvas(Modifier.size(size).clearAndSetSemantics { }) {
                val radius = this.size.minDimension / 2f
                val centerX = center.x
                val centerY = center.y - radius * 0.04f
                val orbit = radius * 0.58f
                val beadRadius = radius * 0.09f
                drawCircle(Gold.copy(alpha = 0.55f), orbit, Offset(centerX, centerY), style = Stroke(1.dp.toPx()))
                val beadCount = 12
                val gap = 36.0
                val start = 90.0 + gap / 2.0
                val sweep = 360.0 - gap
                repeat(beadCount) { index ->
                    val angle = Math.toRadians(start + sweep * index / (beadCount - 1))
                    drawCircle(
                        Gold,
                        beadRadius,
                        Offset(centerX + cos(angle).toFloat() * orbit, centerY + sin(angle).toFloat() * orbit),
                    )
                }
                val guruAngle = Math.toRadians(90.0)
                val guru = Offset(
                    centerX + cos(guruAngle).toFloat() * orbit,
                    centerY + sin(guruAngle).toFloat() * orbit,
                )
                drawCircle(Saffron, beadRadius * 1.6f, guru)
                drawLine(
                    Saffron,
                    Offset(guru.x, guru.y + beadRadius * 1.6f),
                    Offset(guru.x, guru.y + beadRadius * 1.6f + radius * 0.22f),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        } else {
            Text("ॐ", color = MaterialTheme.colors.primary, style = MaterialTheme.typography.title3)
        }
    }
}
