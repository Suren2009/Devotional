package com.pradarshana.core.data

import androidx.room.withTransaction
import com.pradarshana.core.data.db.CounterOperationEntity
import com.pradarshana.core.data.db.DeletedSessionEntity
import com.pradarshana.core.data.db.DeviceCountEntity
import com.pradarshana.core.data.db.MetadataEntity
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.data.db.SessionEntity
import com.pradarshana.core.model.CommonCounts
import com.pradarshana.core.model.CounterRules
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionDetails
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionMerger
import com.pradarshana.core.model.SessionSnapshot
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}

sealed interface StartResult {
    data class Started(val sessionId: String) : StartResult
    data class Invalid(val reason: CounterRules.TargetValidation) : StartResult
}

sealed interface IncrementResult {
    data class Counted(val session: PradarshanaSession) : IncrementResult

    /** The tap that reached the target. Returned exactly once per session. */
    data class Completed(val session: PradarshanaSession) : IncrementResult
    data class Rejected(val reason: Reason) : IncrementResult

    enum class Reason { NOT_FOUND, PAUSED, ALREADY_FINISHED, TARGET_REACHED }
}

data class RemoteApplyResult(
    val changedLocally: Boolean,
    val newlyCompleted: Boolean,
    /** The remote replica is missing information we hold, so we should publish ours. */
    val remoteIsBehind: Boolean,
    val ignored: Boolean = false,
)

class SessionRepository(
    private val db: PradarshanaDatabase,
    val deviceId: String,
    private val clock: Clock = Clock.System,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = db.sessionDao()
    private val writeLock = Mutex()

    private val _localChanges = MutableSharedFlow<String>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Session ids whose state changed on this device. Emissions may be dropped under extreme load;
     * the persistent `local_version > synced_version` marker guarantees they are retried.
     */
    val localChanges: Flow<String> = _localChanges.asSharedFlow()

    fun observeOngoingSession(): Flow<PradarshanaSession?> =
        dao.observeOngoingSession().map { it?.toSession() }.distinctUntilChanged()

    fun observeSession(id: String): Flow<PradarshanaSession?> =
        dao.observeSession(id).map { it?.toSession() }.distinctUntilChanged()

    fun observeHistory(): Flow<List<PradarshanaSession>> =
        dao.observeHistory().map { list -> list.map { it.toSession() } }.distinctUntilChanged()

    suspend fun getSession(id: String): PradarshanaSession? = dao.getSession(id)?.toSession()

    suspend fun getOngoingSession(): PradarshanaSession? = dao.getOngoingSessions().firstOrNull()?.toSession()

    /** Most recent session title, or blank when there is no history or it was left unnamed. */
    suspend fun latestSessionTitle(): String = SessionDetails.normalizeTitle(dao.latestTitle().orEmpty())

    suspend fun latestSessionKind(): SessionKind = SessionKind.fromStored(dao.latestKind())

    /** Most recent target for this kind, or null when that kind has never been started. */
    suspend fun latestTargetFor(kind: SessionKind): Int? = dao.latestTargetForKind(kind.name)

    /** Most recent title for this kind. Blank when there is no such session or it was unnamed. */
    suspend fun latestTitleFor(kind: SessionKind): String =
        SessionDetails.normalizeTitle(dao.latestTitleForKind(kind.name).orEmpty())

    suspend fun suggestedCounts(): List<Int> = CommonCounts.suggest(dao.targetCountsNewestFirst())

    suspend fun startSession(
        targetCount: Int,
        title: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        kind: SessionKind = SessionKind.PRADARSHANA,
    ): StartResult {
        val validation = CounterRules.validateTarget(targetCount.toLong())
        if (validation !is CounterRules.TargetValidation.Valid) return StartResult.Invalid(validation)
        val cleanTitle = SessionDetails.normalizeTitle(title)
        val (lat, lng) = SessionDetails.normalizeLocation(latitude, longitude)

        val now = clock.now()
        val id = idGenerator()
        val changed = mutableListOf<String>()
        write {
            // Only one session can be in progress; starting a new one ends the previous as incomplete.
            for (ongoing in dao.getOngoingSessions()) {
                dao.upsertSession(ongoing.cancelled(now))
                changed += ongoing.id
            }
            dao.insertSession(
                SessionEntity(
                    id = id,
                    targetCount = validation.target,
                    completedCount = 0,
                    startTime = now,
                    endTime = null,
                    status = SessionStatus.ACTIVE,
                    statusUpdatedAt = now,
                    statusUpdatedBy = deviceId,
                    lastUpdatedTime = now,
                    originDeviceId = deviceId,
                    localVersion = 1,
                    syncedVersion = 0,
                    title = cleanTitle,
                    latitude = lat,
                    longitude = lng,
                    kind = kind,
                ),
            )
            dao.upsertDeviceCounts(listOf(DeviceCountEntity(id, deviceId, 0)))
            changed += id
        }
        changed.forEach(::notifyChanged)
        return StartResult.Started(id)
    }

    suspend fun increment(sessionId: String): IncrementResult {
        val result = write {
            val session = dao.getSession(sessionId)
                ?: return@write IncrementResult.Rejected(IncrementResult.Reason.NOT_FOUND)
            when {
                session.status.isTerminal -> return@write IncrementResult.Rejected(IncrementResult.Reason.ALREADY_FINISHED)
                session.status == SessionStatus.PAUSED -> return@write IncrementResult.Rejected(IncrementResult.Reason.PAUSED)
                session.completedCount >= session.targetCount ->
                    return@write IncrementResult.Rejected(IncrementResult.Reason.TARGET_REACHED)
            }
            val now = clock.now()
            val counts = dao.getDeviceCounts(sessionId).associate { it.deviceId to it.count }.toMutableMap()
            val sequence = (counts[deviceId] ?: 0) + 1
            dao.insertOperation(CounterOperationEntity("$sessionId:$deviceId:$sequence", sessionId, deviceId, sequence, now))
            counts[deviceId] = sequence
            dao.upsertDeviceCounts(listOf(DeviceCountEntity(sessionId, deviceId, sequence)))

            val total = CounterRules.mergedCount(session.targetCount, counts)
            val reachedTarget = total >= session.targetCount
            val updated = session.copy(
                completedCount = total,
                lastUpdatedTime = now,
                localVersion = session.localVersion + 1,
            ).let {
                if (reachedTarget) {
                    it.copy(status = SessionStatus.COMPLETED, endTime = now, statusUpdatedAt = now, statusUpdatedBy = deviceId)
                } else {
                    it
                }
            }
            dao.upsertSession(updated)
            if (reachedTarget) IncrementResult.Completed(updated.toSession()) else IncrementResult.Counted(updated.toSession())
        }
        if (result !is IncrementResult.Rejected) notifyChanged(sessionId)
        return result
    }

    suspend fun pause(sessionId: String): Boolean = changeOngoingStatus(sessionId, SessionStatus.PAUSED)

    suspend fun resume(sessionId: String): Boolean = changeOngoingStatus(sessionId, SessionStatus.ACTIVE)

    /** Ends an unfinished session; it is kept in history as incomplete. */
    suspend fun endSession(sessionId: String): Boolean {
        val changed = write {
            val session = dao.getSession(sessionId) ?: return@write false
            if (session.status.isTerminal) return@write false
            dao.upsertSession(session.cancelled(clock.now()))
            true
        }
        if (changed) notifyChanged(sessionId)
        return changed
    }

    /**
     * Deletes finished sessions. Synced copies on the other device that started before this moment
     * will not be re-imported.
     */
    suspend fun clearHistory(): List<String> = write {
        val ids = dao.getTerminalSessionIds()
        val now = clock.now()
        dao.deleteTerminalSessions()
        ids.forEach { dao.insertDeleted(DeletedSessionEntity(it, now)) }
        dao.putMetadata(MetadataEntity(KEY_HISTORY_CLEARED_AT, now))
        ids
    }

    /** Removes one finished session. An in-progress session is left alone. */
    suspend fun deleteSession(sessionId: String): Boolean = write {
        if (dao.deleteTerminalSession(sessionId) == 0) return@write false
        dao.insertDeleted(DeletedSessionEntity(sessionId, clock.now()))
        true
    }

    suspend fun snapshot(sessionId: String): Pair<SessionSnapshot, Long>? = db.withTransaction {
        val entity = dao.getSession(sessionId) ?: return@withTransaction null
        entity.toSnapshot(dao.getDeviceCounts(sessionId)) to entity.localVersion
    }

    suspend fun unsyncedSessionIds(): List<String> = dao.getUnsyncedSessions().map { it.id }

    suspend fun sessionIdsForResync(since: Long): List<String> = dao.getSessionsForResync(since).map { it.id }

    suspend fun markSynced(sessionId: String, version: Long) = dao.markSynced(sessionId, version)

    /** Merges a replica received from the other device. Never counts a round by itself. */
    suspend fun applyRemote(remote: SessionSnapshot): RemoteApplyResult {
        val superseded = mutableListOf<String>()
        val result = write {
            val local = dao.getSession(remote.sessionId)
            if (local == null && dao.isDeleted(remote.sessionId)) {
                return@write RemoteApplyResult(false, false, false, ignored = true)
            }
            if (local == null && remote.status.isTerminal) {
                val clearedAt = dao.getMetadata(KEY_HISTORY_CLEARED_AT)
                if (clearedAt != null && remote.startTime < clearedAt) {
                    return@write RemoteApplyResult(false, false, false, ignored = true)
                }
            }
            val localSnapshot = local?.toSnapshot(dao.getDeviceCounts(local.id))
            val merge = SessionMerger.merge(localSnapshot, remote)
            val merged = merge.merged
            if (merge.changed) {
                dao.upsertSession(
                    SessionEntity(
                        id = merged.sessionId,
                        targetCount = merged.targetCount,
                        completedCount = merged.completedCount,
                        startTime = merged.startTime,
                        endTime = merged.endTime,
                        status = merged.status,
                        statusUpdatedAt = merged.statusUpdatedAt,
                        statusUpdatedBy = merged.statusUpdatedBy,
                        lastUpdatedTime = merged.lastUpdatedTime,
                        originDeviceId = merged.originDeviceId,
                        localVersion = (local?.localVersion ?: 0) + 1,
                        syncedVersion = local?.syncedVersion ?: 0,
                        title = merged.title,
                        latitude = merged.latitude,
                        longitude = merged.longitude,
                        kind = merged.kind,
                        pausedMillis = merged.pausedMillis,
                    ),
                )
                dao.upsertDeviceCounts(merged.deviceCounts.map { (device, count) -> DeviceCountEntity(merged.sessionId, device, count) })
            }
            superseded += cancelSupersededSessions()
            val stored = dao.getSession(merged.sessionId)
            val finalSnapshot = stored?.toSnapshot(dao.getDeviceCounts(stored.id)) ?: merged
            val remoteBehind = SessionMerger.merge(remote, finalSnapshot).changed
            if (!remoteBehind && stored != null && stored.localVersion > stored.syncedVersion) {
                // The peer already holds everything we have, so there is nothing left to send.
                dao.markSynced(stored.id, stored.localVersion)
            }
            RemoteApplyResult(merge.changed, merge.newlyCompleted, remoteBehind)
        }
        if (result.remoteIsBehind) notifyChanged(remote.sessionId)
        superseded.forEach(::notifyChanged)
        return result
    }

    /** Only the most recently started session may stay in progress, on every device. */
    private suspend fun cancelSupersededSessions(): List<String> {
        val ongoing = dao.getOngoingSessions()
        if (ongoing.size <= 1) return emptyList()
        val now = clock.now()
        return ongoing.drop(1).map { stale ->
            dao.upsertSession(stale.cancelled(now))
            stale.id
        }
    }

    private suspend fun changeOngoingStatus(sessionId: String, status: SessionStatus): Boolean {
        val changed = write {
            val session = dao.getSession(sessionId) ?: return@write false
            if (session.status.isTerminal || session.status == status) return@write false
            val now = clock.now()
            val closedPause = if (session.status == SessionStatus.PAUSED) {
                (now - session.statusUpdatedAt).coerceAtLeast(0)
            } else {
                0
            }
            dao.upsertSession(
                session.copy(
                    status = status,
                    statusUpdatedAt = maxOf(now, session.statusUpdatedAt + 1),
                    statusUpdatedBy = deviceId,
                    lastUpdatedTime = now,
                    localVersion = session.localVersion + 1,
                    pausedMillis = session.pausedMillis + closedPause,
                ),
            )
            true
        }
        if (changed) notifyChanged(sessionId)
        return changed
    }

    private fun SessionEntity.cancelled(now: Long): SessionEntity {
        val closedPause = if (status == SessionStatus.PAUSED) (now - statusUpdatedAt).coerceAtLeast(0) else 0
        return copy(
            status = SessionStatus.CANCELLED,
            endTime = now,
            statusUpdatedAt = now,
            statusUpdatedBy = deviceId,
            lastUpdatedTime = now,
            localVersion = localVersion + 1,
            pausedMillis = pausedMillis + closedPause,
        )
    }

    private suspend fun <T> write(block: suspend () -> T): T = writeLock.withLock { db.withTransaction { block() } }

    private fun notifyChanged(sessionId: String) {
        _localChanges.tryEmit(sessionId)
    }

    companion object {
        const val KEY_HISTORY_CLEARED_AT = "history_cleared_at"
    }
}

internal fun SessionEntity.toSession() = PradarshanaSession(
    id = id,
    targetCount = targetCount,
    completedCount = completedCount.coerceIn(0, targetCount),
    startTime = startTime,
    endTime = endTime,
    status = status,
    lastUpdatedTime = lastUpdatedTime,
    title = title,
    latitude = latitude,
    longitude = longitude,
    kind = kind,
    pausedMillis = pausedMillis,
    statusUpdatedAt = statusUpdatedAt,
)

internal fun SessionEntity.toSnapshot(counts: List<DeviceCountEntity>) = SessionSnapshot(
    sessionId = id,
    targetCount = targetCount,
    startTime = startTime,
    endTime = endTime,
    status = status,
    statusUpdatedAt = statusUpdatedAt,
    statusUpdatedBy = statusUpdatedBy,
    lastUpdatedTime = lastUpdatedTime,
    originDeviceId = originDeviceId,
    deviceCounts = counts.associate { it.deviceId to it.count }.toSortedMap(),
    title = title,
    latitude = latitude,
    longitude = longitude,
    kind = kind,
    pausedMillis = pausedMillis,
)
