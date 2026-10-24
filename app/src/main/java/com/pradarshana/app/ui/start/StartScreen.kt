package com.pradarshana.app.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pradarshana.app.ui.appViewModel
import com.pradarshana.core.data.CountFeedbackPreferences
import com.pradarshana.core.data.PradarshanaContainerOwner
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.StartResult
import com.pradarshana.core.data.location.FusedLocationCapture
import com.pradarshana.core.data.location.LocationCapture
import com.pradarshana.core.data.location.hasLocationPermission
import com.pradarshana.core.model.CommonCounts
import com.pradarshana.core.model.CounterRules
import com.pradarshana.core.model.MalaSide
import com.pradarshana.core.model.SessionDetails
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.CounterRules.TargetValidation
import com.pradarshana.core.model.PradarshanaSession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StartViewModel(
    private val repository: SessionRepository,
    private val locationCapture: LocationCapture = LocationCapture { null },
) : ViewModel() {
    var title by mutableStateOf("")
        private set
    var input by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var counts by mutableStateOf(CommonCounts.defaults)
        private set
    var kind by mutableStateOf(SessionKind.PRADARSHANA)
        private set
    private var titleEdited = false
    private var kindEdited = false
    private var starting = false

    init {
        viewModelScope.launch {
            val latest = repository.latestSessionTitle()
            if (!titleEdited && title.isEmpty() && latest.isNotBlank()) title = latest
            val latestKind = repository.latestSessionKind()
            if (!kindEdited) kind = latestKind
            counts = repository.suggestedCounts()
        }
    }

    val ongoing: StateFlow<PradarshanaSession?> = repository.observeOngoingSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _started = Channel<String>(Channel.BUFFERED)
    val started: Flow<String> = _started.receiveAsFlow()

    fun onTitleChange(value: String) {
        titleEdited = true
        title = value.take(SessionDetails.MAX_TITLE_LENGTH)
    }

    fun onKindChange(value: SessionKind) {
        kindEdited = true
        kind = value
    }

    fun onInputChange(value: String) {
        input = value.filter { it.isDigit() }.take(6)
        error = null
    }

    fun start(captureLocation: Boolean = false) {
        if (starting) return
        when (val validation = CounterRules.validateTarget(input)) {
            is TargetValidation.Valid -> {
                starting = true
                viewModelScope.launch {
                    val fix = if (captureLocation) runCatching { locationCapture.capture() }.getOrNull() else null
                    when (val result = repository.startSession(validation.target, title, fix?.latitude, fix?.longitude, kind)) {
                        is StartResult.Started -> _started.send(result.sessionId)
                        is StartResult.Invalid -> error = messageFor(result.reason, kind)
                    }
                    starting = false
                }
            }
            else -> error = messageFor(validation, kind)
        }
    }

    companion object {
        fun messageFor(validation: TargetValidation, kind: SessionKind = SessionKind.PRADARSHANA): String? {
            val counted = if (kind == SessionKind.CHANTING) "times" else "rounds"
            return when (validation) {
                is TargetValidation.Valid -> null
                TargetValidation.Empty -> "Please enter the number of $counted."
                TargetValidation.NotPositive -> "The number of $counted must be greater than zero."
                TargetValidation.TooLarge -> "Please enter at most ${CounterRules.MAX_TARGET} $counted."
                TargetValidation.NotANumber -> "Please enter a whole number."
            }
        }
    }
}

@Composable
fun StartRoute(onBack: () -> Unit, onStarted: (String) -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { StartViewModel(it.repository, FusedLocationCapture(context.applicationContext)) }
    val ongoing by vm.ongoing.collectAsStateWithLifecycle()
    val feedback = (context.applicationContext as? PradarshanaContainerOwner)?.container?.countFeedback
    val savedAutomatic = if (feedback == null) false else SavedAutomaticCount(feedback)
    var chosenAutomatic by remember { mutableStateOf<Boolean?>(null) }
    val automatic = chosenAutomatic ?: savedAutomatic
    val savedMala = if (feedback == null) false else SavedMalaDrag(feedback)
    val savedSide = if (feedback == null) MalaSide.RIGHT else SavedMalaSide(feedback)
    var chosenMala by remember { mutableStateOf<Boolean?>(null) }
    var chosenSide by remember { mutableStateOf<MalaSide?>(null) }
    val malaDrag = chosenMala ?: savedMala
    val malaSide = chosenSide ?: savedSide
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.start(granted || context.hasLocationPermission())
    }
    LaunchedEffect(vm) { vm.started.collect(onStarted) }
    StartScreen(
        title = vm.title,
        input = vm.input,
        error = vm.error,
        ongoing = ongoing,
        counts = vm.counts,
        kind = vm.kind,
        onKindChange = vm::onKindChange,
        onTitleChange = vm::onTitleChange,
        onInputChange = vm::onInputChange,
        automatic = automatic,
        onAutomaticChange = { chosenAutomatic = it },
        malaDrag = malaDrag,
        onMalaDragChange = { chosenMala = it },
        malaSide = malaSide,
        onMalaSideChange = { chosenSide = it },
        onStart = {
            if (vm.kind == SessionKind.PRADARSHANA) feedback?.setAutoCount(automatic)
            if (vm.kind == SessionKind.CHANTING) {
                feedback?.setMalaDrag(malaDrag)
                feedback?.setMalaSide(malaSide)
            }
            if (context.hasLocationPermission()) vm.start(captureLocation = true)
            else permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        },
        onBack = onBack,
    )
}

@Composable
private fun SavedAutomaticCount(feedback: CountFeedbackPreferences): Boolean {
    val enabled by feedback.autoCount.collectAsStateWithLifecycle()
    return enabled
}

@Composable
private fun SavedMalaDrag(feedback: CountFeedbackPreferences): Boolean {
    val enabled by feedback.malaDrag.collectAsStateWithLifecycle()
    return enabled
}

@Composable
private fun SavedMalaSide(feedback: CountFeedbackPreferences): MalaSide {
    val side by feedback.malaSide.collectAsStateWithLifecycle()
    return side
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartScreen(
    input: String,
    error: String?,
    ongoing: PradarshanaSession?,
    onInputChange: (String) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    title: String = "",
    onTitleChange: (String) -> Unit = {},
    counts: List<Int> = CommonCounts.defaults,
    kind: SessionKind = SessionKind.PRADARSHANA,
    onKindChange: (SessionKind) -> Unit = {},
    automatic: Boolean = false,
    onAutomaticChange: (Boolean) -> Unit = {},
    malaDrag: Boolean = false,
    onMalaDragChange: (Boolean) -> Unit = {},
    malaSide: MalaSide = MalaSide.RIGHT,
    onMalaSideChange: (MalaSide) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Start Pradarshana") },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                if (kind == SessionKind.CHANTING) "How many names will you chant?" else "How many rounds will you walk?",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SessionKind.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = kind == option,
                        onClick = { onKindChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, SessionKind.entries.size),
                        modifier = Modifier.testTag("kind_${option.name}"),
                    ) {
                        Text(option.label)
                    }
                }
            }
            if (kind == SessionKind.PRADARSHANA) {
                Spacer(Modifier.height(16.dp))
                Text("Count each round", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !automatic,
                        onClick = { onAutomaticChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("count_mode_manual"),
                    ) {
                        Text("Manual")
                    }
                    SegmentedButton(
                        selected = automatic,
                        onClick = { onAutomaticChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("count_mode_auto"),
                    ) {
                        Text("Automatic")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (automatic) {
                        "A round is counted when you return to the start. The screen stays on and shows the count."
                    } else {
                        "Tap + for each round. This is the usual counting screen."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (kind == SessionKind.CHANTING) {
                Spacer(Modifier.height(16.dp))
                Text("Count each name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !malaDrag,
                        onClick = { onMalaDragChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("mala_mode_plus"),
                    ) {
                        Text("Plus")
                    }
                    SegmentedButton(
                        selected = malaDrag,
                        onClick = { onMalaDragChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("mala_mode_drag"),
                    ) {
                        Text("Drag down")
                    }
                }
                if (malaDrag) {
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = malaSide == MalaSide.LEFT,
                            onClick = { onMalaSideChange(MalaSide.LEFT) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("mala_side_left"),
                        ) {
                            Text("Left")
                        }
                        SegmentedButton(
                            selected = malaSide == MalaSide.RIGHT,
                            onClick = { onMalaSideChange(MalaSide.RIGHT) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("mala_side_right"),
                        ) {
                            Text("Right")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (!malaDrag) {
                        "Tap + for each name. This is the usual counting screen."
                    } else if (malaSide == MalaSide.LEFT) {
                        "Drag down the strip on the left to count one name, then lift."
                    } else {
                        "Drag down the strip on the right to count one name, then lift."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                placeholder = { Text(SessionDetails.UNTITLED) },
                supportingText = { Text("Optional. Leave blank to save as ${SessionDetails.UNTITLED}.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input"),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = {
                    Text(if (kind == SessionKind.CHANTING) "No. of times chanting" else "Target rounds")
                },
                placeholder = { Text("e.g. 108") },
                singleLine = true,
                isError = error != null,
                supportingText = { if (error != null) Text(error, modifier = Modifier.testTag("target_error")) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onStart() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("target_input"),
            )
            Spacer(Modifier.height(8.dp))
            Text("Common counts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                counts.forEach { preset ->
                    SuggestionChip(
                        onClick = { onInputChange(preset.toString()) },
                        label = { Text(preset.toString()) },
                        modifier = Modifier
                            .semantics { contentDescription = "$preset ${kind.unit}" }
                            .testTag("count_$preset"),
                    )
                }
            }
            if (ongoing != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your current session (${ongoing.completedCount} of ${ongoing.targetCount}) will be saved as incomplete.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "If you allow location, this place is saved so you can open it on a map from history. You can decline and still count.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag("confirm_start_button"),
            ) {
                Text("Start", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
