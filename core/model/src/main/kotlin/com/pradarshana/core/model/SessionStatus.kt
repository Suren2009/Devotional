package com.pradarshana.core.model

enum class SessionStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED;

    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELLED

    val isOngoing: Boolean get() = !isTerminal
}
