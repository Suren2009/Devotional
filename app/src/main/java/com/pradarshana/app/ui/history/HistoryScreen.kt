package com.pradarshana.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pradarshana.app.ui.Formatting
import com.pradarshana.app.ui.appViewModel
import com.pradarshana.app.ui.components.Mandala
import com.pradarshana.app.ui.theme.LocalTempleColors
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.sync.SyncManager
import com.pradarshana.core.model.DurationFormat
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(repository: SessionRepository, private val syncManager: SyncManager) : ViewModel() {
    val history: StateFlow<List<PradarshanaSession>?> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearHistory() {
        viewModelScope.launch { syncManager.clearHistory() }
    }
}

@Composable
fun HistoryRoute(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm = appViewModel { HistoryViewModel(it.repository, it.syncManager) }
    val history by vm.history.collectAsStateWithLifecycle()
    HistoryScreen(history = history, onBack = onBack, onOpen = onOpen, onClear = vm::clearHistory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<PradarshanaSession>?,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Pradarshana History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!history.isNullOrEmpty()) {
                        IconButton(onClick = { confirmClear = true }, modifier = Modifier.testTag("clear_history")) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear history")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            history == null -> Unit
            history.isEmpty() -> EmptyHistory(Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("history_list"),
            ) {
                items(history, key = { it.id }) { session ->
                    HistoryItem(session, onClick = { onOpen(session.id) })
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("This permanently removes every saved Pradarshana from this device. A session in progress is kept.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                    modifier = Modifier.testTag("confirm_clear"),
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Mandala(size = 120.dp)
            Spacer(Modifier.height(16.dp))
            Text("No sessions yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Completed and ended Pradarshanas will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HistoryItem(session: PradarshanaSession, onClick: () -> Unit) {
    val duration = DurationFormat.format(session.durationMillis())
    val statusLabel = statusLabel(session.status)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Open details", onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${Formatting.dateTime(session.startTime)}, " +
                    "${session.completedCount} of ${session.targetCount} rounds, $duration, $statusLabel"
            },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Formatting.dateTime(session.startTime),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(session.status)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${session.completedCount}", style = MaterialTheme.typography.headlineSmall)
                Text(
                    " / ${session.targetCount} rounds",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(duration, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { session.progressFraction }, modifier = Modifier.fillMaxWidth())
        }
    }
}

fun statusLabel(status: SessionStatus): String = when (status) {
    SessionStatus.COMPLETED -> "Completed"
    SessionStatus.CANCELLED -> "Incomplete"
    SessionStatus.ACTIVE -> "In progress"
    SessionStatus.PAUSED -> "Paused"
}

@Composable
fun StatusChip(status: SessionStatus) {
    val colors = LocalTempleColors.current
    val color = if (status == SessionStatus.COMPLETED) colors.completed else colors.incomplete
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
    ) {
        Text(
            statusLabel(status),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
