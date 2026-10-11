package com.pradarshana.app.ui.counter

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pradarshana.app.ui.appViewModel
import com.pradarshana.app.ui.components.PauseIcon
import com.pradarshana.app.ui.components.ProgressRing
import com.pradarshana.app.ui.theme.CounterNumberStyle
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus

@Composable
fun CounterRoute(
    sessionId: String,
    onBack: () -> Unit,
    onCompleted: (String) -> Unit,
    onEnded: () -> Unit,
    viewModel: CounterViewModel = appViewModel { CounterViewModel(it.repository, sessionId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                CounterEvent.RoundCounted -> view.tick()
                CounterEvent.Completed -> {
                    view.confirm()
                    onCompleted(sessionId)
                }
                CounterEvent.Ended -> onEnded()
            }
        }
    }

    when (val s = state) {
        CounterUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        CounterUiState.Missing -> LaunchedEffect(Unit) { onEnded() }
        is CounterUiState.Ready -> {
            KeepScreenOn(s.session.status == SessionStatus.ACTIVE)
            CounterScreen(
                session = s.session,
                onIncrement = viewModel::increment,
                onTogglePause = viewModel::togglePause,
                onEndSession = viewModel::endSession,
                onBack = onBack,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    session: PradarshanaSession,
    onIncrement: () -> Unit,
    onTogglePause: () -> Unit,
    onEndSession: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    val paused = session.status == SessionStatus.PAUSED

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Pradarshana") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription =
                                "${session.completedCount} of ${session.targetCount} rounds completed, ${session.progressPercent} percent"
                            liveRegion = LiveRegionMode.Polite
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    ProgressRing(progress = session.progressFraction, modifier = Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            session.completedCount.toString(),
                            style = CounterNumberStyle,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.testTag("completed_count"),
                        )
                        Text(
                            "of ${session.targetCount}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (paused) {
                            Spacer(Modifier.height(4.dp))
                            Text("Paused", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("Target", session.targetCount.toString(), "target_count")
                Stat("Remaining", session.remainingCount.toString(), "remaining_count")
                Stat("Progress", "${session.progressPercent}%", "progress_percent")
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onIncrement,
                enabled = session.canIncrement,
                shape = CircleShape,
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier
                    .size(136.dp)
                    .testTag("increment_button")
                    .semantics {
                        contentDescription = "Count one round"
                        stateDescription = if (paused) "Paused" else "${session.remainingCount} remaining"
                    },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(72.dp))
            }
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("pause_button"),
                ) {
                    Icon(if (paused) Icons.Filled.PlayArrow else PauseIcon, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (paused) "Resume" else "Pause")
                }
                TextButton(
                    onClick = { confirmEnd = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("end_button"),
                ) {
                    Text("End Session")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End this Pradarshana?") },
            text = {
                Text(
                    "You have completed ${session.completedCount} of ${session.targetCount} rounds. " +
                        "The session will be saved in history as incomplete.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmEnd = false
                        onEndSession()
                    },
                    modifier = Modifier.testTag("confirm_end_button"),
                ) { Text("End Session") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("Keep Counting") }
            },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, tag: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$label $value"
            testTag = tag
        },
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

private fun View.tick() {
    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
}

private fun View.confirm() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
    )
}
