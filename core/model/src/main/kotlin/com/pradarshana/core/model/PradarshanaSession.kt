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
) {
    init {
        require(targetCount > 0) { "targetCount must be greater than zero" }
        require(completedCount in 0..targetCount) { "completedCount must be within 0..targetCount" }
        require(status != SessionStatus.COMPLETED || completedCount == targetCount) {
            "A completed session must have completedCount == targetCount"
        }
    }

    val remainingCount: Int get() = CounterRules.remaining(targetCount, completedCount)

    val progressPercent: Int get() = CounterRules.progressPercent(targetCount, completedCount)

    val progressFraction: Float get() = CounterRules.progressFraction(targetCount, completedCount)

    val canIncrement: Boolean get() = CounterRules.canIncrement(status, targetCount, completedCount)

    /** Wall-clock duration; ongoing sessions are measured up to [now]. */
    fun durationMillis(now: Long = System.currentTimeMillis()): Long {
        val end = endTime ?: if (status.isTerminal) lastUpdatedTime else now
        return (end - startTime).coerceAtLeast(0)
    }
}
