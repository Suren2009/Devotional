package com.pradarshana.core.model

/**
 * Deterministic, commutative, associative and idempotent merge of two replicas of a session.
 * Applying the same remote snapshot any number of times, in any order, yields the same state.
 */
object SessionMerger {

    data class Result(
        val merged: SessionSnapshot,
        /** True if the merged state differs from the local state. */
        val changed: Boolean,
        /** True if this merge is what moved the local replica into [SessionStatus.COMPLETED]. */
        val newlyCompleted: Boolean,
    )

    fun merge(local: SessionSnapshot?, remote: SessionSnapshot): Result {
        if (local == null) {
            val normalized = normalize(remote)
            return Result(normalized, changed = true, newlyCompleted = normalized.status == SessionStatus.COMPLETED)
        }
        require(local.sessionId == remote.sessionId) { "Cannot merge different sessions" }

        val counts = mergeCounts(local.deviceCounts, remote.deviceCounts)
        val undos = mergeCounts(local.deviceUndos, remote.deviceUndos)
        // Session identity fields are immutable after creation; the local copy is authoritative.
        val (latitude, longitude) = SessionDetails.mergeLocation(
            local.latitude,
            local.longitude,
            remote.latitude,
            remote.longitude,
        )
        val base = local.copy(
            deviceCounts = counts,
            deviceUndos = undos,
            lastUpdatedTime = maxOf(local.lastUpdatedTime, remote.lastUpdatedTime),
            title = SessionDetails.mergeTitle(local.title, remote.title),
            latitude = latitude,
            longitude = longitude,
            pausedMillis = maxOf(local.pausedMillis, remote.pausedMillis),
            mantra = SessionDetails.mergeMantra(local.mantra, remote.mantra),
        )
        val merged = resolveStatus(base, local, remote)
        return Result(
            merged = merged,
            changed = merged != local,
            newlyCompleted = local.status != SessionStatus.COMPLETED && merged.status == SessionStatus.COMPLETED,
        )
    }

    fun mergeCounts(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> {
        val result = HashMap<String, Int>(a)
        for ((device, count) in b) {
            result[device] = maxOf(result[device] ?: 0, count)
        }
        return result.toSortedMap()
    }

    private fun normalize(snapshot: SessionSnapshot): SessionSnapshot =
        resolveStatus(
            snapshot.copy(
                deviceCounts = snapshot.deviceCounts.toSortedMap(),
                deviceUndos = snapshot.deviceUndos.toSortedMap(),
            ),
            snapshot,
            snapshot,
        )

    private fun resolveStatus(base: SessionSnapshot, a: SessionSnapshot, b: SessionSnapshot): SessionSnapshot {
        val net = CounterRules.mergedCount(base.targetCount, base.deviceCounts, base.deviceUndos)
        val gross = CounterRules.mergedCount(base.targetCount, base.deviceCounts)
        val alreadyFinished = gross >= base.targetCount &&
            (a.status == SessionStatus.COMPLETED || b.status == SessionStatus.COMPLETED)
        if (net >= base.targetCount || alreadyFinished) {
            val completedEnds = listOfNotNull(
                a.endTime.takeIf { a.status == SessionStatus.COMPLETED },
                b.endTime.takeIf { b.status == SessionStatus.COMPLETED },
            )
            val endTime = completedEnds.minOrNull() ?: base.lastUpdatedTime
            val winner = listOf(a, b)
                .filter { it.status == SessionStatus.COMPLETED }
                .minWithOrNull(compareBy<SessionSnapshot>({ it.statusUpdatedAt }, { it.statusUpdatedBy }))
            return base.copy(
                status = SessionStatus.COMPLETED,
                endTime = endTime,
                statusUpdatedAt = winner?.statusUpdatedAt ?: endTime,
                statusUpdatedBy = winner?.statusUpdatedBy ?: MERGE_AUTHOR,
            )
        }

        val cancelled = listOf(a, b)
            .filter { it.status == SessionStatus.CANCELLED }
            .minWithOrNull(compareBy<SessionSnapshot>({ it.statusUpdatedAt }, { it.statusUpdatedBy }))
        if (cancelled != null) {
            return base.copy(
                status = SessionStatus.CANCELLED,
                endTime = cancelled.endTime ?: cancelled.statusUpdatedAt,
                statusUpdatedAt = cancelled.statusUpdatedAt,
                statusUpdatedBy = cancelled.statusUpdatedBy,
            )
        }

        val winner = maxOf(a, b, compareBy<SessionSnapshot>({ it.statusUpdatedAt }, { it.statusUpdatedBy }))
        return base.copy(
            // A replica can only claim COMPLETED once the target is reached; otherwise it is malformed.
            status = if (winner.status == SessionStatus.COMPLETED) SessionStatus.ACTIVE else winner.status,
            endTime = null,
            statusUpdatedAt = winner.statusUpdatedAt,
            statusUpdatedBy = winner.statusUpdatedBy,
        )
    }

    const val MERGE_AUTHOR = "merge"
}
