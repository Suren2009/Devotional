package com.pradarshana.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.model.PradarshanaSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionViewModel(repository: SessionRepository, sessionId: String) : ViewModel() {
    val session: StateFlow<PradarshanaSession?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
