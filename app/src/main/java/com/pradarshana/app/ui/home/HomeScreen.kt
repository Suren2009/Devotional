package com.pradarshana.app.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pradarshana.app.count.AutoCountHost
import com.pradarshana.app.ui.appViewModel
import com.pradarshana.app.ui.components.Mandala
import com.pradarshana.core.data.CountFeedbackPreferences
import com.pradarshana.core.data.PradarshanaContainerOwner
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.model.CountPeriods
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

class HomeViewModel(repository: SessionRepository) : ViewModel() {
    val ongoing: StateFlow<PradarshanaSession?> = repository.observeOngoingSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _totals = MutableStateFlow(CountPeriods.Totals(0, 0, 0))
    val totals: StateFlow<CountPeriods.Totals> = _totals

    init {
        viewModelScope.launch {
            combine(repository.observeHistory(), repository.observeOngoingSession()) { _, _ -> }
                .collect {
                    _totals.value = repository.periodCounts(System.currentTimeMillis(), ZoneId.systemDefault())
                }
        }
    }
}

@Composable
fun HomeRoute(
    onStart: () -> Unit,
    onResume: (String) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onAskBaba: () -> Unit,
    onStotras: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it.repository) }
    val ongoing by vm.ongoing.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val feedback = (context.applicationContext as? PradarshanaContainerOwner)?.container?.countFeedback
    val showTotals = if (feedback == null) false else CollectedTotals(feedback)
    AutoCountHost(ongoing)
    HomeScreen(
        ongoing = ongoing,
        onStart = onStart,
        onResume = onResume,
        onHistory = onHistory,
        onSettings = onSettings,
        onAskBaba = onAskBaba,
        onStotras = onStotras,
        totals = if (showTotals) totals else null,
    )
}

@Composable
private fun CollectedTotals(feedback: CountFeedbackPreferences): Boolean {
    val enabled by feedback.showTotals.collectAsStateWithLifecycle()
    return enabled
}

@Composable
fun HomeScreen(
    ongoing: PradarshanaSession?,
    onStart: () -> Unit,
    onResume: (String) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit = {},
    onAskBaba: () -> Unit = {},
    onStotras: () -> Unit = {},
    totals: CountPeriods.Totals? = null,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Mandala(size = 176.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                "Pradarshana",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Count your rounds around the temple with a single tap, and walk in peace.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            if (totals != null) {
                TotalsCard(totals)
                Spacer(Modifier.height(20.dp))
            }

            if (ongoing != null) {
                ResumeCard(ongoing, onResume = { onResume(ongoing.id) })
                Spacer(Modifier.height(20.dp))
            }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag("start_button"),
            ) {
                Text("Start Pradarshana", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("history_button"),
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Pradarshana History", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onStotras,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("stotras_button"),
            ) {
                Text("Stotras", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAskBaba,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("ask_baba_button"),
            ) {
                Text("Ask Baba", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onSettings,
                modifier = Modifier.testTag("settings_button"),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Settings")
            }
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun TotalsCard(totals: CountPeriods.Totals) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("period_totals"),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Counted", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Today ${totals.today} · This week ${totals.week} · This month ${totals.month}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ResumeCard(session: PradarshanaSession, onResume: () -> Unit) {
    val label = if (session.status == SessionStatus.PAUSED) "Paused session" else "Session in progress"
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, ${session.completedCount} of ${session.targetCount} rounds"
            },
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(4.dp))
            Text(
                "${session.completedCount} of ${session.targetCount} rounds · ${session.remainingCount} remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { session.progressFraction },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(onClick = onResume, modifier = Modifier.testTag("resume_button")) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Resume")
                }
            }
        }
    }
}
