package com.pradarshana.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.pradarshana.app.ui.stotra.StotraLanguageStore
import com.pradarshana.app.ui.stotra.StotraTextSize
import com.pradarshana.app.ui.theme.ThemeMode
import com.pradarshana.core.model.AutoCountDistances
import com.pradarshana.core.model.MalaSide
import com.pradarshana.core.model.SavedTemple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as PradarshanaApp
    val mode by app.themeStore.mode.collectAsStateWithLifecycle()
    val vibrateOnCount by app.container.countFeedback.vibrateOnCount.collectAsStateWithLifecycle()
    val blackoutAfterTwoTaps by app.container.countFeedback.blackoutAfterTwoTaps.collectAsStateWithLifecycle()
    val autoCount by app.container.countFeedback.autoCount.collectAsStateWithLifecycle()
    val awayMeters by app.container.countFeedback.awayMeters.collectAsStateWithLifecycle()
    val returnMeters by app.container.countFeedback.returnMeters.collectAsStateWithLifecycle()
    val malaDrag by app.container.countFeedback.malaDrag.collectAsStateWithLifecycle()
    val malaSide by app.container.countFeedback.malaSide.collectAsStateWithLifecycle()
    val undoLast by app.container.countFeedback.undoLast.collectAsStateWithLifecycle()
    val volumeCount by app.container.countFeedback.volumeCount.collectAsStateWithLifecycle()
    val guruBell by app.container.countFeedback.guruBell.collectAsStateWithLifecycle()
    val showTotals by app.container.countFeedback.showTotals.collectAsStateWithLifecycle()
    val temples by app.container.countFeedback.temples.collectAsStateWithLifecycle()
    val stotraStore = remember { StotraLanguageStore(app) }
    val textSize by stotraStore.textSize.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = app.container.repository.exportHistory()
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.openOutputStream(uri)?.use { stream -> stream.write(json.toByteArray(Charsets.UTF_8)) }
                    true
                }.getOrDefault(false)
            }
            backupMessage = if (written) "History saved." else "Could not save the history file."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching { app.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
            }
            val kept = if (json == null) -1 else app.container.repository.importHistory(json)
            backupMessage = if (kept < 0) "That file could not be read." else "Brought in $kept sessions."
        }
    }
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
        malaDrag = malaDrag,
        onMalaDrag = app.container.countFeedback::setMalaDrag,
        malaSide = malaSide,
        onMalaSide = app.container.countFeedback::setMalaSide,
        undoLast = undoLast,
        onUndoLast = app.container.countFeedback::setUndoLast,
        volumeCount = volumeCount,
        onVolumeCount = app.container.countFeedback::setVolumeCount,
        guruBell = guruBell,
        onGuruBell = app.container.countFeedback::setGuruBell,
        showTotals = showTotals,
        onShowTotals = app.container.countFeedback::setShowTotals,
        temples = temples,
        onSaveTemple = { name, latitude, longitude ->
            val lat = latitude.toDoubleOrNull()
            val lng = longitude.toDoubleOrNull()
            if (lat == null || lng == null || !app.container.countFeedback.saveTemple(name, lat, lng)) {
                backupMessage = "Enter a name and a valid place."
            } else {
                backupMessage = "Temple saved."
            }
        },
        onDeleteTemple = app.container.countFeedback::deleteTemple,
        textSize = textSize,
        onTextSize = stotraStore::setTextSize,
        backupMessage = backupMessage,
        onExport = { exportLauncher.launch("pradarshana-history.json") },
        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
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
    malaDrag: Boolean = false,
    onMalaDrag: (Boolean) -> Unit = {},
    malaSide: MalaSide = MalaSide.RIGHT,
    onMalaSide: (MalaSide) -> Unit = {},
    undoLast: Boolean = false,
    onUndoLast: (Boolean) -> Unit = {},
    volumeCount: Boolean = false,
    onVolumeCount: (Boolean) -> Unit = {},
    guruBell: Boolean = false,
    onGuruBell: (Boolean) -> Unit = {},
    showTotals: Boolean = false,
    onShowTotals: (Boolean) -> Unit = {},
    temples: List<SavedTemple> = emptyList(),
    onSaveTemple: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteTemple: (String) -> Unit = {},
    textSize: StotraTextSize = StotraTextSize.MEDIUM,
    onTextSize: (StotraTextSize) -> Unit = {},
    backupMessage: String? = null,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
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
                        "Turns the screen black except the plus button, or the mala strip when drag counting is on. Double-tap elsewhere to bring the count back.",
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clickable(role = Role.Switch) { onMalaDrag(!malaDrag) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("mala_drag"),
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Count chanting by dragging down", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (malaDrag) {
                            "On. Drag down the strip to count one name. The start screen can also choose this."
                        } else {
                            "Off. Tap + while chanting. The start screen can also choose this."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = malaDrag, onCheckedChange = onMalaDrag)
            }
            if (malaDrag) {
                Text(
                    "Mala strip",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    SegmentedButton(
                        selected = malaSide == MalaSide.LEFT,
                        onClick = { onMalaSide(MalaSide.LEFT) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("mala_side_left"),
                    ) {
                        Text("Left")
                    }
                    SegmentedButton(
                        selected = malaSide == MalaSide.RIGHT,
                        onClick = { onMalaSide(MalaSide.RIGHT) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("mala_side_right"),
                    ) {
                        Text("Right")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OptionSwitch(
                title = "Undo last count",
                body = "Shows Undo on the counting screen. It takes back one count made on this phone, and a finished session stays finished.",
                checked = undoLast,
                onChecked = onUndoLast,
                tag = "undo_last_count",
            )
            OptionSwitch(
                title = "Volume buttons count",
                body = "While a session is open, the volume keys add one count and leave the volume alone.",
                checked = volumeCount,
                onChecked = onVolumeCount,
                tag = "volume_count",
            )
            OptionSwitch(
                title = "Guru bead vibration",
                body = "A stronger pulse at halfway and when the session is finished.",
                checked = guruBell,
                onChecked = onGuruBell,
                tag = "guru_bell",
            )
            OptionSwitch(
                title = "Show counted totals",
                body = "Home shows how many counts were made today, this week and this month.",
                checked = showTotals,
                onChecked = onShowTotals,
                tag = "show_totals",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "Stotra text",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                StotraTextSize.entries.forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = textSize == size,
                        onClick = { onTextSize(size) },
                        shape = SegmentedButtonDefaults.itemShape(index, StotraTextSize.entries.size),
                        modifier = Modifier.testTag("stotra_text_${size.stored}"),
                    ) {
                        Text(
                            when (size) {
                                StotraTextSize.SMALL -> "Small"
                                StotraTextSize.MEDIUM -> "Medium"
                                StotraTextSize.LARGE -> "Large"
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "Saved temples",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                "A saved place can be the start of an automatic Pradarshana. The walk still uses location only while automatic counting is on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            TempleEditor(onSaveTemple)
            temples.forEach { temple ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(temple.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${temple.latitude}, ${temple.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onDeleteTemple(temple.id) }, modifier = Modifier.testTag("delete_temple_${temple.id}")) {
                        Text("Remove")
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "History file",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                "Save a copy of history, or bring a copy back. Sessions you deleted stay deleted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Button(onClick = onExport, modifier = Modifier.testTag("export_history")) { Text("Save history") }
                Button(onClick = onImport, modifier = Modifier.testTag("import_history")) { Text("Bring history in") }
            }
            if (backupMessage != null) {
                Text(
                    backupMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("backup_message"),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OptionSwitch(
    title: String,
    body: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    tag: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Switch) { onChecked(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(tag),
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun TempleEditor(onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(60) },
        label = { Text("Temple name") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .keepVisibleAboveKeyboard()
            .testTag("temple_name"),
    )
    OutlinedTextField(
        value = latitude,
        onValueChange = { latitude = it.take(12) },
        label = { Text("Latitude") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .keepVisibleAboveKeyboard()
            .testTag("temple_latitude"),
    )
    OutlinedTextField(
        value = longitude,
        onValueChange = { longitude = it.take(12) },
        label = { Text("Longitude") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .keepVisibleAboveKeyboard()
            .testTag("temple_longitude"),
    )
    Button(
        onClick = { onSave(name, latitude, longitude) },
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("save_temple"),
    ) {
        Text("Save temple")
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
