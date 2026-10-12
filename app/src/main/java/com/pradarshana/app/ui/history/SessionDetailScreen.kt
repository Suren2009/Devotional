package com.pradarshana.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pradarshana.app.ui.Formatting
import com.pradarshana.app.ui.components.SessionMark
import com.pradarshana.app.ui.appViewModel
import com.pradarshana.app.ui.components.ProgressRing
import com.pradarshana.app.ui.session.SessionViewModel
import com.pradarshana.core.model.DurationFormat
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionDetails

@Composable
fun SessionDetailRoute(sessionId: String, onBack: () -> Unit) {
    val vm = appViewModel { SessionViewModel(it.repository, sessionId) }
    val session by vm.session.collectAsStateWithLifecycle()
    SessionDetailScreen(session, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(session: PradarshanaSession?, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (session == null) return@Scaffold
        val context = LocalContext.current
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .width(180.dp)
                    .aspectRatio(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${session.completedCount} of ${session.targetCount} rounds, ${session.progressPercent} percent"
                    },
                contentAlignment = Alignment.Center,
            ) {
                ProgressRing(session.progressFraction, Modifier.fillMaxSize(), strokeWidth = 10.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${session.completedCount}", style = MaterialTheme.typography.displayMedium)
                    Text("of ${session.targetCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            SessionMark(session.kind, size = 72.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                session.displayTitle,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            StatusChip(session.status)
            if (session.hasLocation) {
                Spacer(Modifier.height(16.dp))
                SessionMapPreview(session)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { openSessionInMaps(context, session) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Show on map")
                }
            }
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    DetailRow("Type", session.kind.label)
                    DetailRow("Title", session.displayTitle)
                    DetailRow("Place", placeLabel(session))
                    DetailRow("Date", Formatting.date(session.startTime))
                    DetailRow("Started", Formatting.time(session.startTime))
                    DetailRow("Ended", session.endTime?.let(Formatting::time) ?: "—")
                    DetailRow("Duration", DurationFormat.format(session.durationMillis()))
                    DetailRow("Target ${session.kind.unit}", session.targetCount.toString())
                    DetailRow("Completed ${session.kind.unit}", session.completedCount.toString())
                    DetailRow("Remaining ${session.kind.unit}", session.remainingCount.toString())
                    DetailRow("Progress", "${session.progressPercent}%")
                    DetailRow("Status", statusLabel(session.status), divider = false)
                }
            }
        }
    }
}

@Composable
private fun placeLabel(session: PradarshanaSession): String {
    val latitude = session.latitude
    val longitude = session.longitude
    return if (latitude != null && longitude != null) {
        SessionDetails.formatLocation(latitude, longitude)
    } else {
        "Not saved"
    }
}

@Composable
private fun DetailRow(label: String, value: String, divider: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
