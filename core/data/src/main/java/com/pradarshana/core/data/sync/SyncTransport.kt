package com.pradarshana.core.data.sync

import com.pradarshana.core.model.SessionSnapshot

/** A snapshot as published by [sourceDeviceId]. */
data class RemoteSnapshot(
    val sourceDeviceId: String,
    val snapshot: SessionSnapshot,
)

/**
 * Store-and-forward channel to the paired device. Implementations must keep the latest published
 * snapshot per session and deliver it once the peer becomes reachable (the Wear OS Data Layer
 * `DataClient` does exactly this).
 */
interface SyncTransport {
    /** Persists [snapshot] locally for delivery to the peer. Throws if it could not be stored. */
    suspend fun publish(snapshot: SessionSnapshot)

    /** Every snapshot currently known to the transport, from all devices. */
    suspend fun fetchAll(): List<RemoteSnapshot>

    /** Removes this device's published copies of the given sessions. */
    suspend fun delete(sessionIds: List<String>)
}
