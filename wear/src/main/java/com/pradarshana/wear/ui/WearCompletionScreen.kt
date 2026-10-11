package com.pradarshana.wear.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.pradarshana.core.model.DurationFormat
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.wear.ui.theme.Gold

@Composable
fun WearCompletionRoute(sessionId: String, onDone: () -> Unit) {
    val vm = wearViewModel { WearSessionViewModel(it.repository, sessionId) }
    val session by vm.session.collectAsStateWithLifecycle()
    session?.let { WearCompletionScreen(it, onDone) }
}

@Composable
fun WearCompletionScreen(session: PradarshanaSession, onDone: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "glow")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clearAndSetSemantics { },
        ) {
            drawCircle(Gold.copy(alpha = glow), radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(3.dp.toPx()))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Gold, modifier = Modifier.size(28.dp))
            Text(
                "Pradarshana Completed!",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("wear_completed_title"),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${session.completedCount} / ${session.targetCount} rounds",
                style = MaterialTheme.typography.body2,
            )
            Text(
                DurationFormat.formatCompact(session.durationMillis()),
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            CompactChip(onClick = onDone, label = { Text("Done") })
        }
    }
}
