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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.StartResult
import com.pradarshana.core.model.CounterRules
import com.pradarshana.core.model.CounterRules.TargetValidation
import com.pradarshana.core.model.PradarshanaSession
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StartViewModel(private val repository: SessionRepository) : ViewModel() {
    var input by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var starting = false

    val ongoing: StateFlow<PradarshanaSession?> = repository.observeOngoingSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _started = Channel<String>(Channel.BUFFERED)
    val started: Flow<String> = _started.receiveAsFlow()

    fun onInputChange(value: String) {
        input = value.filter { it.isDigit() }.take(6)
        error = null
    }

    fun start() {
        if (starting) return
        when (val validation = CounterRules.validateTarget(input)) {
            is TargetValidation.Valid -> {
                starting = true
                viewModelScope.launch {
                    when (val result = repository.startSession(validation.target)) {
                        is StartResult.Started -> _started.send(result.sessionId)
                        is StartResult.Invalid -> error = messageFor(result.reason)
                    }
                    starting = false
                }
            }
            else -> error = messageFor(validation)
        }
    }

    companion object {
        fun messageFor(validation: TargetValidation): String? = when (validation) {
            is TargetValidation.Valid -> null
            TargetValidation.Empty -> "Please enter the number of rounds."
            TargetValidation.NotPositive -> "The number of rounds must be greater than zero."
            TargetValidation.TooLarge -> "Please enter at most ${CounterRules.MAX_TARGET} rounds."
            TargetValidation.NotANumber -> "Please enter a whole number."
        }
    }
}

val PresetTargets = listOf(3, 7, 11, 21, 51, 108)

@Composable
fun StartRoute(onBack: () -> Unit, onStarted: (String) -> Unit) {
    val vm = appViewModel { StartViewModel(it.repository) }
    val ongoing by vm.ongoing.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.started.collect(onStarted) }
    StartScreen(
        input = vm.input,
        error = vm.error,
        ongoing = ongoing,
        onInputChange = vm::onInputChange,
        onStart = vm::start,
        onBack = onBack,
    )
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
            Text("How many rounds will you walk?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text("Target rounds") },
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
                PresetTargets.forEach { preset ->
                    SuggestionChip(
                        onClick = { onInputChange(preset.toString()) },
                        label = { Text(preset.toString()) },
                        modifier = Modifier.semantics { contentDescription = "$preset rounds" },
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
            Spacer(Modifier.height(28.dp))
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
