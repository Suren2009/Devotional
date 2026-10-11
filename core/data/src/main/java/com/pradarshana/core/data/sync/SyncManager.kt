package com.pradarshana.core.data.sync

import android.util.Log
import com.pradarshana.core.data.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pushes local session changes to the paired device and merges incoming replicas.
 *
 * Every publish carries the full replicated state of a session, so delivery can be delayed,
 * repeated or reordered without double counting. Changes that could not be handed to the
 * transport stay marked as unsynced in the database and are retried by [flushPending].
 */
class SyncManager(
    private val repository: SessionRepository,
    private val transport: SyncTransport,
    private val scope: CoroutineScope,
    private val logger: (String, Throwable?) -> Unit = { msg, t -> Log.w(TAG, msg, t) },
) {
    private val publishLock = Mutex()
    private var collector: Job? = null

    fun start() {
        if (collector != null) return
        collector = scope.launch {
            repository.localChanges.collect { publish(it) }
        }
        scope.launch { reconcile() }
    }

    fun requestReconcile() {
        scope.launch { reconcile() }
    }

    /** Pulls everything the transport holds, merges it, then pushes anything still unsent. */
    suspend fun reconcile() {
        try {
            onRemoteSnapshots(transport.fetchAll())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger("Fetching remote sessions failed", e)
        }
        flushPending()
    }

    suspend fun onRemoteSnapshots(snapshots: List<RemoteSnapshot>) {
        for (remote in snapshots) {
            if (remote.sourceDeviceId == repository.deviceId) continue
            try {
                repository.applyRemote(remote.snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger("Ignoring invalid remote session ${remote.snapshot.sessionId}", e)
            }
        }
    }

    suspend fun flushPending() {
        for (id in repository.unsyncedSessionIds()) publish(id)
    }

    /** Publishes are serialized so an older snapshot can never overwrite a newer one. */
    suspend fun publish(sessionId: String): Boolean = publishLock.withLock {
        val (snapshot, version) = repository.snapshot(sessionId) ?: return@withLock false
        try {
            transport.publish(snapshot)
            repository.markSynced(sessionId, version)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger("Publishing session $sessionId failed; will retry", e)
            false
        }
    }

    suspend fun clearHistory() {
        val removed = repository.clearHistory()
        if (removed.isEmpty()) return
        try {
            transport.delete(removed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger("Removing synced history failed", e)
        }
    }

    private companion object {
        const val TAG = "PradarshanaSync"
    }
}
