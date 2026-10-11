package com.pradarshana.core.model

/**
 * Full replicated state of one session, exchanged between phone and watch.
 *
 * The counter is a grow-only counter (G-Counter CRDT): every device only ever increases its own
 * entry in [deviceCounts], and replicas merge by taking the per-device maximum. Each local tap is
 * operation `sessionId:deviceId:sequence`, so re-delivered or duplicated snapshots can never add rounds twice
 * and concurrent taps on both devices are never lost.
 *
 * The status is a last-writer-wins register ordered by ([statusUpdatedAt], [statusUpdatedBy]),
 * except that terminal states are sticky and reaching the target always means [SessionStatus.COMPLETED].
 */
data class SessionSnapshot(
    val sessionId: String,
    val targetCount: Int,
    val startTime: Long,
    val endTime: Long?,
    val status: SessionStatus,
    val statusUpdatedAt: Long,
    val statusUpdatedBy: String,
    val lastUpdatedTime: Long,
    val originDeviceId: String,
    val deviceCounts: Map<String, Int>,
) {
    init {
        require(targetCount > 0) { "targetCount must be greater than zero" }
        require(deviceCounts.values.all { it >= 0 }) { "device counts must not be negative" }
    }

    val completedCount: Int get() = CounterRules.mergedCount(targetCount, deviceCounts)

    val remainingCount: Int get() = CounterRules.remaining(targetCount, completedCount)

    fun toSession(): PradarshanaSession = PradarshanaSession(
        id = sessionId,
        targetCount = targetCount,
        completedCount = completedCount,
        startTime = startTime,
        endTime = endTime,
        status = status,
        lastUpdatedTime = lastUpdatedTime,
    )
}
