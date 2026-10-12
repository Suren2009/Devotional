package com.pradarshana.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pradarshana.core.data.PradarshanaContainer
import com.pradarshana.core.data.PradarshanaContainerOwner
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.StartResult
import com.pradarshana.core.data.location.LocationCapture
import com.pradarshana.core.model.CommonCounts
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionKind
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
inline fun <reified VM : ViewModel> wearViewModel(crossinline create: (PradarshanaContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as PradarshanaContainerOwner).container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

class WearHomeViewModel(
    private val repository: SessionRepository,
    private val locationCapture: LocationCapture = LocationCapture { null },
) : ViewModel() {
    val ongoing: StateFlow<PradarshanaSession?> = repository.observeOngoingSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var counts by mutableStateOf(CommonCounts.defaults)
        private set
    var kind by mutableStateOf(SessionKind.PRADARSHANA)
        private set
    private var kindEdited = false

    init {
        viewModelScope.launch {
            counts = repository.suggestedCounts()
            val latestKind = repository.latestSessionKind()
            if (!kindEdited) kind = latestKind
        }
    }

    fun onKindChange(value: SessionKind) {
        kindEdited = true
        kind = value
    }

    private val _started = Channel<String>(Channel.BUFFERED)
    val started: Flow<String> = _started.receiveAsFlow()
    private var starting = false

    fun start(target: Int, captureLocation: Boolean = false, kind: SessionKind = this.kind) {
        if (starting) return
        starting = true
        viewModelScope.launch {
            val fix = if (captureLocation) runCatching { locationCapture.capture() }.getOrNull() else null
            val result = repository.startSession(
                target,
                latitude = fix?.latitude,
                longitude = fix?.longitude,
                kind = kind,
            )
            (result as? StartResult.Started)?.let { _started.send(it.sessionId) }
            starting = false
        }
    }
}

class WearSessionViewModel(repository: SessionRepository, sessionId: String) : ViewModel() {
    val session: StateFlow<PradarshanaSession?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
