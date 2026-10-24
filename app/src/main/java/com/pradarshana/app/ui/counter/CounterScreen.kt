package com.pradarshana.app.ui.counter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sqrt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pradarshana.app.count.AutoCountHost
import com.pradarshana.app.count.AutoCountService
import com.pradarshana.app.ui.appViewModel
import com.pradarshana.core.data.CompletionVibration
import com.pradarshana.core.data.PradarshanaContainerOwner
import com.pradarshana.app.ui.components.PauseIcon
import com.pradarshana.app.ui.components.SessionMark
import com.pradarshana.core.data.presentation.CounterEvent
import com.pradarshana.core.data.presentation.CounterUiState
import com.pradarshana.core.data.presentation.CounterViewModel
import com.pradarshana.app.ui.components.ProgressRing
import com.pradarshana.app.ui.theme.CounterNumberStyle
import com.pradarshana.core.model.MalaDrag
import com.pradarshana.core.model.MalaSide
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionKind
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
        CounterUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        CounterUiState.Missing -> LaunchedEffect(Unit) { onEnded() }
        is CounterUiState.Ready -> {
            val batterySaverEnabled = if (feedback == null) {
                true
            } else {
                val enabled by feedback.blackoutAfterTwoTaps.collectAsStateWithLifecycle()
                enabled
            }
            val autoCount = if (feedback == null || s.session.kind != SessionKind.PRADARSHANA) {
                false
            } else {
                val enabled by feedback.autoCount.collectAsStateWithLifecycle()
                enabled
            }
            val malaDrag = if (feedback == null || s.session.kind != SessionKind.CHANTING) {
                false
            } else {
                val enabled by feedback.malaDrag.collectAsStateWithLifecycle()
                enabled
            }
            val malaSide = if (feedback == null) {
                MalaSide.RIGHT
            } else {
                val side by feedback.malaSide.collectAsStateWithLifecycle()
                side
            }
            AutoCountHost(s.session)
            CounterScreen(
                session = s.session,
                onIncrement = viewModel::increment,
                onTogglePause = viewModel::togglePause,
                onEndSession = viewModel::endSession,
                onBack = onBack,
                batterySaverEnabled = batterySaverEnabled,
                autoCount = autoCount,
                malaDrag = malaDrag,
                malaSide = malaSide,
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
    batterySaverEnabled: Boolean = true,
    autoCount: Boolean = false,
    malaDrag: Boolean = false,
    malaSide: MalaSide = MalaSide.RIGHT,
) {
    val automatic = autoCount && session.kind == SessionKind.PRADARSHANA
    val mala = malaDrag && session.kind == SessionKind.CHANTING
    val autoStatus by AutoCountService.progress.collectAsStateWithLifecycle()
    KeepScreenOn(automatic || session.status == SessionStatus.ACTIVE)
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    var blackout by rememberSaveable { mutableStateOf(automatic) }
    var plusClicks by rememberSaveable { mutableIntStateOf(0) }
    val paused = session.status == SessionStatus.PAUSED

    fun wake() {
        blackout = false
        plusClicks = 0
    }

    LaunchedEffect(automatic) {
        if (automatic) blackout = true
    }
    LaunchedEffect(batterySaverEnabled) {
        if (!batterySaverEnabled && !automatic) wake()
    }

    fun onPlus() {
        if (!session.canIncrement) return
        val completesNow = session.completedCount + 1 >= session.targetCount
        onIncrement()
        if (completesNow) {
            wake()
            return
        }
        if (!blackout && batterySaverEnabled) {
            plusClicks += 1
            if (plusClicks >= 2) blackout = true
        }
    }

    if (blackout && session.status != SessionStatus.COMPLETED) {
        BlackCountingScreen(
            enabled = session.canIncrement && !automatic,
            paused = paused,
            remaining = session.remainingCount,
            completed = session.completedCount,
            target = session.targetCount,
            countLabel = if (automatic) session.completedCount.toString() else null,
            statusText = if (automatic) autoStatus else null,
            onPlus = if (automatic) ({}) else ::onPlus,
            onWake = ::wake,
            mala = mala,
            malaSide = malaSide,
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(session.kind.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = if (mala && malaSide == MalaSide.LEFT) 88.dp else 24.dp,
                    end = if (mala && malaSide == MalaSide.RIGHT) 88.dp else 24.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SessionMark(session.kind, size = 72.dp)
            Spacer(Modifier.height(4.dp))
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
                            session.kind.countHeading,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.testTag("count_heading"),
                        )
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

            if (automatic) {
                RoundCountButton(session.completedCount)
                Spacer(Modifier.height(8.dp))
                Text(
                    autoStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("auto_count_status"),
                )
            } else if (!mala) {
                Button(
                    onClick = ::onPlus,
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
            } else {
                Text(
                    "Drag down the strip to count one name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("mala_hint"),
                )
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
        if (mala) {
            MalaStrip(
                completed = session.completedCount,
                target = session.targetCount,
                enabled = session.canIncrement,
                onCount = ::onPlus,
                onBlack = false,
                modifier = Modifier.align(
                    if (malaSide == MalaSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd,
                ),
            )
        }
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
private fun BlackCountingScreen(
    enabled: Boolean,
    paused: Boolean,
    remaining: Int,
    completed: Int = 0,
    target: Int = 1,
    onPlus: () -> Unit,
    onWake: () -> Unit,
    countLabel: String? = null,
    statusText: String? = null,
    mala: Boolean = false,
    malaSide: MalaSide = MalaSide.RIGHT,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = window?.statusBarColor
        val previousNavigation = window?.navigationBarColor
        window?.statusBarColor = android.graphics.Color.BLACK
        window?.navigationBarColor = android.graphics.Color.BLACK
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            if (previousStatus != null) window.statusBarColor = previousStatus
            if (previousNavigation != null) window.navigationBarColor = previousNavigation
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onWake() }) }
            .testTag("battery_saver")
            .semantics {
                contentDescription = if (mala) {
                    "Screen darkened to save battery. Double tap outside the mala strip to show the count."
                } else {
                    "Screen darkened to save battery. Double tap outside the plus button to show the count."
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (mala) {
            MalaStrip(
                completed = completed,
                target = target,
                enabled = enabled,
                onCount = onPlus,
                onBlack = true,
                modifier = Modifier.align(
                    if (malaSide == MalaSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd,
                ),
            )
        } else if (countLabel == null) {
            Button(
                onClick = onPlus,
                enabled = enabled,
                shape = CircleShape,
                modifier = Modifier
                    .size(136.dp)
                    .testTag("increment_button")
                    .semantics {
                        contentDescription = "Count one round"
                        stateDescription = if (paused) "Paused" else "$remaining remaining"
                    },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(72.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 28.dp)) {
                RoundCountButton(countLabel.toIntOrNull() ?: 0)
                if (!statusText.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        statusText,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("auto_count_status"),
                    )
                }
            }
        }
    }
}

@Composable
private fun MalaStrip(
    completed: Int,
    target: Int,
    enabled: Boolean,
    onCount: () -> Unit,
    onBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { MalaDrag.MIN_DOWNWARD_DP.dp.toPx() }
    val insetPx = with(density) { 18.dp.toPx() }
    val home = MalaDrag.beadFraction(completed, target)
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(home) }
    val shown = if (dragging) dragFraction else home
    val saffron = if (onBlack) Color(0xFFE07B17) else MaterialTheme.colorScheme.primary
    val gold = Color(0xFFC9A227)
    val pale = if (onBlack) Color.White.copy(alpha = 0.28f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val cord = if (onBlack) Color.White.copy(alpha = 0.45f) else gold
    BoxWithConstraints(
        modifier
            .width(80.dp)
            .fillMaxHeight()
            .testTag("mala_strip")
            .semantics { contentDescription = "Drag down to count one name" }
            .pointerInput(enabled, thresholdPx, insetPx) {
                if (!enabled) return@pointerInput
                val span = (size.height - insetPx * 2f).coerceAtLeast(1f)
                var downward = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        downward = 0f
                        dragging = true
                        dragFraction = ((offset.y - insetPx) / span).coerceIn(0f, 1f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 0f) downward += dragAmount.y
                        dragFraction = (dragFraction + dragAmount.y / span).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        val countThisDrag = MalaDrag.counts(downward, thresholdPx)
                        dragging = false
                        if (countThisDrag) onCount()
                    },
                    onDragCancel = { dragging = false },
                )
            },
    ) {
        val bead = 34.dp
        val inset = 18.dp
        val span = (maxHeight - inset * 2).coerceAtLeast(0.dp)
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val top = 18.dp.toPx()
            val travel = (size.height - top * 2f).coerceAtLeast(1f)
            drawLine(cord, Offset(cx, top), Offset(cx, top + travel), strokeWidth = 3.dp.toPx())
            val midY = top + travel * 0.5f
            drawLine(cord, Offset(cx - 22.dp.toPx(), midY), Offset(cx - 12.dp.toPx(), midY), strokeWidth = 3.dp.toPx())
            drawLine(cord, Offset(cx + 12.dp.toPx(), midY), Offset(cx + 22.dp.toPx(), midY), strokeWidth = 3.dp.toPx())
            val slots = 16
            repeat(slots) { index ->
                val place = index / (slots - 1f)
                if (abs(place - shown) < 0.045f) return@repeat
                val visited = MalaDrag.beadVisited(place, completed, target)
                rudraksha(
                    center = Offset(cx, top + travel * place),
                    rx = 8.dp.toPx(),
                    ry = 11.dp.toPx(),
                    body = if (visited) saffron else pale,
                    groove = if (visited) gold else pale.copy(alpha = 0.9f),
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = inset + span * shown - bead / 2)
                .size(bead)
                .testTag("mala_bead")
                .semantics { contentDescription = "Name $completed of $target" },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                rudraksha(
                    center = Offset(size.width / 2f, size.height / 2f),
                    rx = size.width / 2f - 2.dp.toPx(),
                    ry = size.height / 2f - 2.dp.toPx(),
                    body = saffron,
                    groove = if (onBlack) Color(0xFF8C430C) else gold,
                )
                drawOval(
                    color = gold,
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.rudraksha(
    center: Offset,
    rx: Float,
    ry: Float,
    body: Color,
    groove: Color,
) {
    drawOval(body, topLeft = Offset(center.x - rx, center.y - ry), size = Size(rx * 2f, ry * 2f))
    for (t in floatArrayOf(-0.55f, -0.28f, 0f, 0.28f, 0.55f)) {
        val half = ry * sqrt((1f - t * t).coerceAtLeast(0f))
        val x = center.x + rx * t
        drawLine(
            groove,
            Offset(x, center.y - half * 0.72f),
            Offset(x, center.y + half * 0.72f),
            strokeWidth = 1.5f,
        )
    }
    val hole = (rx * 0.12f).coerceAtLeast(1.5f)
    drawCircle(groove, hole, Offset(center.x, center.y - ry + hole))
    drawCircle(groove, hole, Offset(center.x, center.y + ry - hole))
}

@Composable
private fun RoundCountButton(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(136.dp)
            .testTag("auto_count_button")
            .semantics { contentDescription = "$count rounds, counted automatically" },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                count.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 48.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
        }
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
        val window = view.context.findActivity()?.window
        view.keepScreenOn = enabled
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            view.keepScreenOn = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
