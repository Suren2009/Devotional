package com.pradarshana.wear.ui

import androidx.compose.runtime.Composable
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
import com.pradarshana.core.model.PradarshanaSession
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

class WearHomeViewModel(private val repository: SessionRepository) : ViewModel() {
    val ongoing: StateFlow<PradarshanaSession?> = repository.observeOngoingSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _started = Channel<String>(Channel.BUFFERED)
    val started: Flow<String> = _started.receiveAsFlow()
    private var starting = false

    fun start(target: Int) {
        if (starting) return
        starting = true
        viewModelScope.launch {
            (repository.startSession(target) as? StartResult.Started)?.let { _started.send(it.sessionId) }
            starting = false
        }
    }
}

class WearSessionViewModel(repository: SessionRepository, sessionId: String) : ViewModel() {
    val session: StateFlow<PradarshanaSession?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
