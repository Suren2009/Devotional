package com.pradarshana.app.ui.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pradarshana.core.data.IncrementResult
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CounterUiState {
    data object Loading : CounterUiState
    data object Missing : CounterUiState
    data class Ready(val session: PradarshanaSession) : CounterUiState
}

sealed interface CounterEvent {
    data object RoundCounted : CounterEvent

    /** Emitted at most once per ViewModel, whether completion happened here or on the watch. */
    data object Completed : CounterEvent
    data object Ended : CounterEvent
}

class CounterViewModel(
    private val repository: SessionRepository,
    private val sessionId: String,
) : ViewModel() {

    private val events = Channel<CounterEvent>(Channel.BUFFERED)
    val eventFlow: Flow<CounterEvent> = events.receiveAsFlow()
    private var completionSignalled = false
    private var endSignalled = false

    val state: StateFlow<CounterUiState> = repository.observeSession(sessionId)
        .onEach { session -> session?.let(::signalTerminalState) }
        .map { if (it == null) CounterUiState.Missing else CounterUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CounterUiState.Loading)

    fun increment() {
        viewModelScope.launch {
            when (val result = repository.increment(sessionId)) {
                is IncrementResult.Counted -> events.send(CounterEvent.RoundCounted)
                is IncrementResult.Completed -> signalTerminalState(result.session)
                is IncrementResult.Rejected -> Unit
            }
        }
    }

    fun togglePause() {
        val session = (state.value as? CounterUiState.Ready)?.session ?: return
        viewModelScope.launch {
            when (session.status) {
                SessionStatus.ACTIVE -> repository.pause(sessionId)
                SessionStatus.PAUSED -> repository.resume(sessionId)
                else -> Unit
            }
        }
    }

    fun endSession() {
        viewModelScope.launch { repository.endSession(sessionId) }
    }

    private fun signalTerminalState(session: PradarshanaSession) {
        when (session.status) {
            SessionStatus.COMPLETED -> if (!completionSignalled) {
                completionSignalled = true
                events.trySend(CounterEvent.Completed)
            }
            SessionStatus.CANCELLED -> if (!endSignalled) {
                endSignalled = true
                events.trySend(CounterEvent.Ended)
            }
            else -> Unit
        }
    }
}
