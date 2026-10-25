package com.pradarshana.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pradarshana.app.R
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
    onChant: () -> Unit,
    onResume: (String) -> Unit,
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
        onChant = onChant,
        onResume = onResume,
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
    onChant: () -> Unit = {},
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

            HomeMenu(
                ongoing = ongoing,
                onStart = onStart,
                onChant = onChant,
                onResume = { id -> onResume(id) },
                onStotras = onStotras,
                onAskBaba = onAskBaba,
                onSettings = onSettings,
            )
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
private fun HomeMenu(
    ongoing: PradarshanaSession?,
    onStart: () -> Unit,
    onChant: () -> Unit,
    onResume: (String) -> Unit,
    onStotras: () -> Unit,
    onAskBaba: () -> Unit,
    onSettings: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        if (ongoing != null) {
            val paused = ongoing.status == SessionStatus.PAUSED
            val label = if (paused) "Paused" else "Resume"
            MenuTile(
                label = label,
                tag = "resume_button",
                description = "$label, ${ongoing.completedCount} of ${ongoing.targetCount} rounds, ${ongoing.remainingCount} remaining",
                onClick = { onResume(ongoing.id) },
            ) {
                if (paused) PauseIcon(tint = tint)
                else Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
            }
        }
        MenuTile(label = "Start", tag = "start_button", onClick = onStart) {
            PradakshinaIcon(tint = tint)
        }
        MenuTile(label = "Chanting", tag = "chanting_button", onClick = onChant) {
            MalaIcon(tint = tint)
        }
        MenuTile(label = "Stotras", tag = "stotras_button", onClick = onStotras) {
            BookIcon(tint = tint)
        }
        MenuTile(label = "Ask Baba", tag = "ask_baba_button", onClick = onAskBaba) {
            Image(
                painter = painterResource(R.drawable.sai_baba_1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
        }
        MenuTile(label = "Settings", tag = "settings_button", onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun RowScope.MenuTile(
    label: String,
    tag: String,
    onClick: () -> Unit,
    description: String = label,
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 3.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp)
            .semantics { contentDescription = description }
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
