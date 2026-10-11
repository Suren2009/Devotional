package com.pradarshana.core.data.sync

import com.pradarshana.core.model.SessionSnapshot
import java.io.IOException

/**
 * In-memory stand-in for the Wear OS Data Layer between exactly two nodes. Like `DataClient`,
 * each node keeps its latest item per session and forwards it when the link is up; while the
 * link is down items are retained and only the newest version is delivered on reconnect.
 */
class FakeDataLayer {
    var connected = true

    /** When set, deliveries are repeated to simulate duplicate notifications. */
    var duplicateDeliveries = false

    val a = Node("A")
    val b = Node("B")

    init {
        a.peer = b
        b.peer = a
    }

    inner class Node(val name: String) : SyncTransport {
        lateinit var peer: Node
        lateinit var deviceId: String
        var receiver: (suspend (List<RemoteSnapshot>) -> Unit)? = null
        var failPublishes = false

        val ownItems = LinkedHashMap<String, RemoteSnapshot>()
        val peerItems = LinkedHashMap<String, RemoteSnapshot>()
        val undelivered = LinkedHashSet<String>()
        var publishCount = 0

        override suspend fun publish(snapshot: SessionSnapshot) {
            if (failPublishes) throw IOException("Wearable API unavailable")
            publishCount++
            ownItems[snapshot.sessionId] = RemoteSnapshot(deviceId, snapshot)
            undelivered += snapshot.sessionId
        }

        override suspend fun fetchAll(): List<RemoteSnapshot> = ownItems.values + peerItems.values

        override suspend fun delete(sessionIds: List<String>) {
            sessionIds.forEach { ownItems.remove(it); undelivered.remove(it) }
        }

        /** Returns true if anything was delivered. */
        suspend fun deliver(): Boolean {
            if (!connected || undelivered.isEmpty()) return false
            val batch = undelivered.mapNotNull { ownItems[it] }
            undelivered.clear()
            batch.forEach { peer.peerItems[it.snapshot.sessionId] = it }
            peer.receiver?.invoke(batch)
            if (duplicateDeliveries) peer.receiver?.invoke(batch)
            return true
        }
    }

    /** Delivers in both directions until no device has anything left to send. */
    suspend fun pump(vararg managers: SyncManager) {
        repeat(50) {
            managers.forEach { it.flushPending() }
            val movedA = a.deliver()
            val movedB = b.deliver()
            if (!movedA && !movedB) return
        }
        error("Sync did not converge")
    }
}
