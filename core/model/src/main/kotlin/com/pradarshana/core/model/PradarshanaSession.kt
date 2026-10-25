package com.pradarshana.core.model

/**
 * Read model of a session as shown in the UI. The counter itself is stored as per-device
 * contributions (see [SessionSnapshot]); [completedCount] is the merged, capped total.
 */
data class PradarshanaSession(
    val id: String,
    val targetCount: Int,
    val completedCount: Int,
    val startTime: Long,
    val endTime: Long?,
    val status: SessionStatus,
    val lastUpdatedTime: Long,
    /** Empty when the devotee did not name the session. Display [displayTitle] instead. */
    val title: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val kind: SessionKind = SessionKind.PRADARSHANA,
    /** Time spent paused, already closed. An open pause is measured from [statusUpdatedAt]. */
    val pausedMillis: Long = 0,
    val statusUpdatedAt: Long = lastUpdatedTime,
    /** Chosen for a chant. Blank when the session has no mantra. */
    val mantra: String = "",
) {
    init {
        require(targetCount > 0) { "targetCount must be greater than zero" }
        require(completedCount in 0..targetCount) { "completedCount must be within 0..targetCount" }
        require(status != SessionStatus.COMPLETED || completedCount == targetCount) {
            "A completed session must have completedCount == targetCount"
        }
        require(pausedMillis >= 0) { "pausedMillis must not be negative" }
        require(title == SessionDetails.normalizeTitle(title)) { "title must be normalized" }
        require(mantra == SessionDetails.normalizeMantra(mantra)) { "mantra must be normalized" }
        val (lat, lng) = SessionDetails.normalizeLocation(latitude, longitude)
        require(lat == latitude && lng == longitude) { "location must be a valid pair or absent" }
    }

    val displayTitle: String get() = SessionDetails.displayTitle(title)

    val hasLocation: Boolean get() = latitude != null && longitude != null

    val remainingCount: Int get() = CounterRules.remaining(targetCount, completedCount)

    val progressPercent: Int get() = CounterRules.progressPercent(targetCount, completedCount)

    val progressFraction: Float get() = CounterRules.progressFraction(targetCount, completedCount)

    val canIncrement: Boolean get() = CounterRules.canIncrement(status, targetCount, completedCount)

    /**
     * Time from the start to the end, leaving out every pause.
     * Ongoing sessions are measured up to [now]. A pause still in progress is not counted.
     */
    fun durationMillis(now: Long = System.currentTimeMillis()): Long {
        val end = endTime ?: if (status.isTerminal) lastUpdatedTime else now
        val openPause = if (status == SessionStatus.PAUSED && endTime == null) {
            (now - statusUpdatedAt).coerceAtLeast(0)
        } else {
            0
        }
        return (end - startTime - pausedMillis - openPause).coerceAtLeast(0)
    }
}
