package com.pradarshana.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.pradarshana.core.data.CompletionVibration
import com.pradarshana.core.data.PradarshanaContainerOwner
import com.pradarshana.core.data.presentation.CounterEvent
import com.pradarshana.core.data.presentation.CounterUiState
import com.pradarshana.core.data.presentation.CounterViewModel
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus

@Composable
fun WearCounterRoute(
    sessionId: String,
    onCompleted: (String) -> Unit,
    onEnded: () -> Unit,
    viewModel: CounterViewModel = wearViewModel { CounterViewModel(it.repository, sessionId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val feedback = (LocalContext.current.applicationContext as? PradarshanaContainerOwner)?.container?.countFeedback
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                CounterEvent.RoundCounted -> if (feedback?.vibrateOnCount?.value != false) CompletionVibration.tap(view.context)
                CounterEvent.Completed -> {
                    CompletionVibration.start(view.context)
                    onCompleted(sessionId)
                }
                CounterEvent.Ended -> onEnded()
            }
        }
    }
    when (val s = state) {
        CounterUiState.Loading -> Unit
        CounterUiState.Missing -> LaunchedEffect(Unit) { onEnded() }
        is CounterUiState.Ready -> WearCounterScreen(
            session = s.session,
            onIncrement = viewModel::increment,
            onTogglePause = viewModel::togglePause,
            onEnd = viewModel::endSession,
        )
    }
}

@Composable
fun WearCounterScreen(
    session: PradarshanaSession,
    onIncrement: () -> Unit,
    onTogglePause: () -> Unit,
    onEnd: () -> Unit,
) {
    val paused = session.status == SessionStatus.PAUSED
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    var blackout by rememberSaveable { mutableStateOf(false) }
    var plusClicks by rememberSaveable { mutableIntStateOf(0) }

    fun wake() {
        blackout = false
        plusClicks = 0
    }

    fun onPlus() {
        if (!session.canIncrement) return
        val completesNow = session.completedCount + 1 >= session.targetCount
        onIncrement()
        if (completesNow) {
            wake()
            return
        }
        if (!blackout) {
            plusClicks += 1
            if (plusClicks >= 2) blackout = true
        }
    }

    if (blackout && session.status != SessionStatus.COMPLETED) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { wake() }) }
                .testTag("battery_saver"),
            contentAlignment = Alignment.Center,
        ) {
            val plusSize = (maxWidth * 0.36f).coerceIn(56.dp, 84.dp)
            Button(
                onClick = ::onPlus,
                enabled = session.canIncrement,
                modifier = Modifier
                    .size(plusSize)
                    .testTag("wear_increment"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Count one round", modifier = Modifier.size(plusSize * 0.55f))
            }
        }
        return
    }

    Scaffold(timeText = { TimeText() }) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val plusSize = (maxWidth * 0.36f).coerceIn(56.dp, 84.dp)
            CircularProgressIndicator(
                progress = session.progressFraction,
                startAngle = 300f,
                endAngle = 240f,
                strokeWidth = 6.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                WearSessionMark(session.kind, size = 26.dp)
                Spacer(Modifier.height(2.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "${session.completedCount} of ${session.targetCount} rounds, " +
                            "${session.remainingCount} remaining"
                        liveRegion = LiveRegionMode.Polite
                    },
                ) {
                    Text(
                        "${session.completedCount}",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.testTag("wear_count"),
                    )
                    Text(
                        if (paused) "Paused" else "of ${session.targetCount} · ${session.remainingCount} left",
                        style = MaterialTheme.typography.caption2,
                        color = if (paused) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactButton(
                        onClick = onTogglePause,
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.testTag("wear_pause"),
                    ) {
                        Icon(
                            if (paused) Icons.Filled.PlayArrow else WearIcons.Pause,
                            contentDescription = if (paused) "Resume" else "Pause",
                        )
                    }
                    Button(
                        onClick = ::onPlus,
                        enabled = session.canIncrement,
                        modifier = Modifier
                            .size(plusSize)
                            .testTag("wear_increment"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Count one round", modifier = Modifier.size(plusSize * 0.55f))
                    }
                    // Ending is only offered while paused, so a stray touch mid-walk cannot end the session.
                    Box(Modifier.size(ButtonDefaults.ExtraSmallButtonSize + 16.dp), contentAlignment = Alignment.Center) {
                        if (paused) {
                            CompactButton(
                                onClick = { confirmEnd = true },
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.testTag("wear_end"),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "End session")
                            }
                        }
                    }
                }
            }
        }
    }

    Dialog(showDialog = confirmEnd, onDismissRequest = { confirmEnd = false }) {
        Alert(
            title = { Text("End session?") },
            negativeButton = {
                Button(onClick = { confirmEnd = false }, colors = ButtonDefaults.secondaryButtonColors()) {
                    Icon(Icons.Filled.Close, contentDescription = "Keep counting")
                }
            },
            positiveButton = {
                Button(
                    onClick = {
                        confirmEnd = false
                        onEnd()
                    },
                    modifier = Modifier.testTag("wear_confirm_end"),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "End session")
                }
            },
        ) {
            Text(
                "${session.completedCount} of ${session.targetCount} rounds will be saved as incomplete.",
                style = MaterialTheme.typography.body2,
            )
        }
    }
}
