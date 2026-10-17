package com.pradarshana.app.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pradarshana.app.PradarshanaApp
import com.pradarshana.app.ui.theme.ThemeMode
import com.pradarshana.core.model.AutoCountDistances

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as PradarshanaApp
    val mode by app.themeStore.mode.collectAsStateWithLifecycle()
    val vibrateOnCount by app.container.countFeedback.vibrateOnCount.collectAsStateWithLifecycle()
    val blackoutAfterTwoTaps by app.container.countFeedback.blackoutAfterTwoTaps.collectAsStateWithLifecycle()
    val autoCount by app.container.countFeedback.autoCount.collectAsStateWithLifecycle()
    val awayMeters by app.container.countFeedback.awayMeters.collectAsStateWithLifecycle()
    val returnMeters by app.container.countFeedback.returnMeters.collectAsStateWithLifecycle()
    SettingsScreen(
        mode = mode,
        onMode = app.themeStore::set,
        vibrateOnCount = vibrateOnCount,
        onVibrateOnCount = app.container.countFeedback::setVibrateOnCount,
        blackoutAfterTwoTaps = blackoutAfterTwoTaps,
        onBlackoutAfterTwoTaps = app.container.countFeedback::setBlackoutAfterTwoTaps,
        autoCount = autoCount,
        onAutoCount = app.container.countFeedback::setAutoCount,
        awayMeters = awayMeters,
        returnMeters = returnMeters,
        onDistances = app.container.countFeedback::setDistances,
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
    autoCount: Boolean = false,
    onAutoCount: (Boolean) -> Unit = {},
    awayMeters: Int = AutoCountDistances.DEFAULT_AWAY_METERS,
    returnMeters: Int = AutoCountDistances.DEFAULT_RETURN_METERS,
    onDistances: (Int, Int) -> Unit = { _, _ -> },
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
                .consumeWindowInsets(padding)
                .imePadding()
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clickable(role = Role.Switch) { onAutoCount(!autoCount) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("auto_count"),
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Automatic Pradarshana counting", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (autoCount) {
                            "Automatic. Choose Manual or Automatic on the start screen too."
                        } else {
                            "Manual. Tap + on the usual counting screen. The start screen can switch this."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = autoCount, onCheckedChange = onAutoCount)
            }
            if (autoCount) {
                DistanceFields(awayMeters, returnMeters, onDistances)
            }
        }
    }
}

@Composable
private fun DistanceFields(
    awayMeters: Int,
    returnMeters: Int,
    onDistances: (Int, Int) -> Unit,
) {
    var awayText by remember(awayMeters) { mutableStateOf(awayMeters.toString()) }
    var returnText by remember(returnMeters) { mutableStateOf(returnMeters.toString()) }
    val away = awayText.toIntOrNull()
    val back = returnText.toIntOrNull()
    val valid = away != null && back != null && AutoCountDistances.isValid(away, back)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = awayText,
        onValueChange = { raw ->
            awayText = raw.filter { it.isDigit() }.take(3)
            publishDistances(awayText, returnText, onDistances)
        },
        label = { Text("Walk away from the start (meters)") },
        supportingText = { Text("How far the walk must go before a return can count. Default 30.") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .keepVisibleAboveKeyboard()
            .testTag("auto_count_away"),
    )
    OutlinedTextField(
        value = returnText,
        onValueChange = { raw ->
            returnText = raw.filter { it.isDigit() }.take(3)
            publishDistances(awayText, returnText, onDistances)
        },
        label = { Text("Back within (meters)") },
        supportingText = {
            Text(
                if (valid || away == null || back == null) {
                    "A round counts when the walk comes back inside this distance. Default 20."
                } else {
                    "This must be shorter than the walk-away distance."
                },
            )
        },
        isError = away != null && back != null && !valid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .keepVisibleAboveKeyboard()
            .testTag("auto_count_return"),
    )
    Spacer(Modifier.height(24.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.keepVisibleAboveKeyboard(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom, focused) {
        if (focused && imeBottom > 0) requester.bringIntoView()
    }
    return this
        .bringIntoViewRequester(requester)
        .onFocusEvent { focused = it.isFocused }
}

private fun publishDistances(awayText: String, returnText: String, onDistances: (Int, Int) -> Unit) {
    val away = awayText.toIntOrNull() ?: return
    val back = returnText.toIntOrNull() ?: return
    if (AutoCountDistances.isValid(away, back)) onDistances(away, back)
}
