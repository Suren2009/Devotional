package com.pradarshana.app.ui.completion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pradarshana.app.ui.Formatting
import com.pradarshana.app.ui.appViewModel
import com.pradarshana.app.ui.components.Mandala
import com.pradarshana.app.ui.session.SessionViewModel
import com.pradarshana.app.ui.theme.LocalTempleColors
import com.pradarshana.core.model.DurationFormat
import com.pradarshana.core.model.PradarshanaSession

@Composable
fun CompletionRoute(sessionId: String, onStartAnother: () -> Unit, onHistory: () -> Unit, onHome: () -> Unit) {
    val vm = appViewModel { SessionViewModel(it.repository, sessionId) }
    val session by vm.session.collectAsStateWithLifecycle()
    session?.let { CompletionScreen(it, onStartAnother, onHistory, onHome) }
}

@Composable
fun CompletionScreen(
    session: PradarshanaSession,
    onStartAnother: () -> Unit,
    onHistory: () -> Unit,
    onHome: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Radiance(Modifier.size(240.dp))
                    Mandala(size = 150.dp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Pradarshana Completed!",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag("completion_title"),
                )
                Spacer(Modifier.height(6.dp))
                Text(session.displayTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "May your devotion bring you peace.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Summary("Completed", session.completedCount.toString())
                        Summary("Target", session.targetCount.toString())
                        Summary("Duration", DurationFormat.format(session.durationMillis()))
                    }
                }
                session.endTime?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Formatting.dateTime(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(28.dp))
                Button(onClick = onStartAnother, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Start Another Pradarshana", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("View History")
                }
                TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Home")
                }
            }
        }
    }
}

@Composable
private fun Summary(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Slow, soft rings expanding outward like ripples from a lamp. */
@Composable
private fun Radiance(modifier: Modifier) {
    val gold = LocalTempleColors.current.gold
    val transition = rememberInfiniteTransition(label = "radiance")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val glow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    Canvas(modifier.alpha(glow).clearAndSetSemantics { }) {
        val maxR = size.minDimension / 2
        repeat(3) { i ->
            val p = (phase + i / 3f) % 1f
            drawCircle(
                color = gold.copy(alpha = (1f - p) * 0.45f),
                radius = maxR * (0.6f + 0.4f * p),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
