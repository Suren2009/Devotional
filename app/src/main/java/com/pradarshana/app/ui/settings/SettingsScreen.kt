package com.pradarshana.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pradarshana.app.PradarshanaApp
import com.pradarshana.app.ui.theme.ThemeMode

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as PradarshanaApp
    val mode by app.themeStore.mode.collectAsStateWithLifecycle()
    val vibrateOnCount by app.container.countFeedback.vibrateOnCount.collectAsStateWithLifecycle()
    val blackoutAfterTwoTaps by app.container.countFeedback.blackoutAfterTwoTaps.collectAsStateWithLifecycle()
    SettingsScreen(
        mode = mode,
        onMode = app.themeStore::set,
        vibrateOnCount = vibrateOnCount,
        onVibrateOnCount = app.container.countFeedback::setVibrateOnCount,
        blackoutAfterTwoTaps = blackoutAfterTwoTaps,
        onBlackoutAfterTwoTaps = app.container.countFeedback::setBlackoutAfterTwoTaps,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mode: ThemeMode,
    onMode: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    vibrateOnCount: Boolean = true,
    onVibrateOnCount: (Boolean) -> Unit = {},
    blackoutAfterTwoTaps: Boolean = true,
    onBlackoutAfterTwoTaps: (Boolean) -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                "Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
            )
            Text(
                "Light, dark, or the same as the phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            ThemeMode.entries.forEach { option ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clickable(role = Role.RadioButton) { onMode(option) }
                        .padding(horizontal = 8.dp)
                        .testTag("theme_${option.name}"),
                ) {
                    RadioButton(selected = mode == option, onClick = { onMode(option) })
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(option.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            option.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Counting",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
            )
            Text(
                "A short vibration each time you tap +.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable(role = Role.Switch) { onVibrateOnCount(!vibrateOnCount) }
                    .padding(horizontal = 16.dp)
                    .testTag("vibrate_on_count"),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Vibrate on each count", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (vibrateOnCount) "On" else "Off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = vibrateOnCount, onCheckedChange = onVibrateOnCount)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clickable(role = Role.Switch) { onBlackoutAfterTwoTaps(!blackoutAfterTwoTaps) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("blackout_after_two_taps"),
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Darken after two counts", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Turns the screen black except the plus button. Double-tap elsewhere to bring the count back.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = blackoutAfterTwoTaps, onCheckedChange = onBlackoutAfterTwoTaps)
            }
        }
    }
}
