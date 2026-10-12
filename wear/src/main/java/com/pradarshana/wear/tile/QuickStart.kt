package com.pradarshana.wear.tile

import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.StartResult
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the watch tile shows, and the intent extra that opens the counter from a tap. */
object QuickStart {
    const val EXTRA = "com.pradarshana.wear.extra.QUICK_START"
    const val DEFAULT_TARGET = 108

    private val lock = Mutex()

    fun content(ongoing: PradarshanaSession?, latestTarget: Int): TileContent {
        if (ongoing == null) {
            return TileContent(
                title = SessionKind.PRADARSHANA.label,
                countText = latestTarget.toString(),
                detail = SessionKind.PRADARSHANA.unit,
                action = "Start",
            )
        }
        val detail = if (ongoing.status == SessionStatus.PAUSED) {
            "Paused"
        } else {
            "of ${ongoing.targetCount}"
        }
        return TileContent(
            title = ongoing.kind.label,
            countText = ongoing.completedCount.toString(),
            detail = detail,
            action = "Resume",
        )
    }

    /**
     * Opens the session already in progress, or starts a Pradarshana using the latest walk's
     * target and title. The first walk uses [DEFAULT_TARGET]. A place is not requested here.
     */
    suspend fun startOrResume(repository: SessionRepository): String = lock.withLock {
        repository.getOngoingSession()?.let { return it.id }
        val kind = SessionKind.PRADARSHANA
        val target = repository.latestTargetFor(kind) ?: DEFAULT_TARGET
        val title = repository.latestTitleFor(kind)
        when (val result = repository.startSession(target, title = title, kind = kind)) {
            is StartResult.Started -> result.sessionId
            is StartResult.Invalid -> {
                val fallback = repository.startSession(DEFAULT_TARGET, kind = kind)
                (fallback as StartResult.Started).sessionId
            }
        }
    }
}

data class TileContent(
    val title: String,
    val countText: String,
    val detail: String,
    val action: String,
)
