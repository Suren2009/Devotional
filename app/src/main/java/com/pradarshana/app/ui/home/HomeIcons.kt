package com.pradarshana.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** A temple inside a circular arrow: one round walked around the shrine. */
@Composable
fun PradakshinaIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier.size(32.dp)) {
        val stroke = 2.2.dp.toPx()
        val inset = stroke + 1.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(
            color = tint,
            startAngle = 40f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val radius = arcSize.minDimension / 2
        val end = Math.toRadians(320.0)
        val tip = Offset(
            (size.width / 2 + radius * cos(end)).toFloat(),
            (size.height / 2 + radius * sin(end)).toFloat(),
        )
        val head = 5.5.dp.toPx()
        drawLine(tint, tip, tip + Offset(-head, -head * 0.15f), stroke, StrokeCap.Round)
        drawLine(tint, tip, tip + Offset(-head * 0.15f, head), stroke, StrokeCap.Round)
        val cx = size.width / 2
        val cy = size.height / 2
        val roof = Path().apply {
            moveTo(cx, cy - radius * 0.38f)
            lineTo(cx + radius * 0.36f, cy - radius * 0.02f)
            lineTo(cx - radius * 0.36f, cy - radius * 0.02f)
            close()
        }
        drawPath(roof, tint)
        drawRect(
            color = tint,
            topLeft = Offset(cx - radius * 0.18f, cy - radius * 0.02f),
            size = Size(radius * 0.36f, radius * 0.34f),
        )
    }
}

/** A short mala: beads on a cord. */
@Composable
fun MalaIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier.size(32.dp)) {
        val cx = size.width / 2
        drawLine(
            color = tint,
            start = Offset(cx, size.height * 0.12f),
            end = Offset(cx, size.height * 0.88f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        val bead = 3.4.dp.toPx()
        repeat(4) { index ->
            val y = size.height * (0.24f + index * 0.18f)
            drawCircle(tint, bead, Offset(cx, y))
        }
    }
}

@Composable
fun BookIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier.size(32.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.22f
        val bottom = size.height * 0.78f
        val mid = size.width / 2
        drawLine(tint, Offset(left, top), Offset(mid, top + 3.dp.toPx()), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(mid, top + 3.dp.toPx()), Offset(right, top), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(left, top), Offset(left, bottom), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(right, top), Offset(right, bottom), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(left, bottom), Offset(mid, bottom - 3.dp.toPx()), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(mid, bottom - 3.dp.toPx()), Offset(right, bottom), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(mid, top + 3.dp.toPx()), Offset(mid, bottom - 3.dp.toPx()), stroke.width, StrokeCap.Round)
    }
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier.size(32.dp)) {
        val bar = Size(size.width * 0.16f, size.height * 0.46f)
        val top = size.height * 0.27f
        drawRoundRect(tint, Offset(size.width * 0.28f, top), bar, androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        drawRoundRect(tint, Offset(size.width * 0.56f, top), bar, androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
    }
}
