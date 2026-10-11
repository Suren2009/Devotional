package com.pradarshana.core.data.sync

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.pradarshana.core.model.SessionSnapshot
import kotlinx.coroutines.tasks.await

/**
 * Wear OS Data Layer transport. Each device writes one DataItem per session at
 * `/pradarshana/session/<id>`; DataItem URIs are scoped by node, so the phone's and the watch's
 * copies never overwrite each other. The Data Layer persists items and delivers them once the
 * Bluetooth/cloud connection to the peer returns, which is what makes offline counting safe.
 */
class DataLayerSyncTransport(
    context: Context,
    private val deviceId: String,
) : SyncTransport {

    private val appContext = context.applicationContext
    private val dataClient: DataClient by lazy { Wearable.getDataClient(appContext) }

    override suspend fun publish(snapshot: SessionSnapshot) {
        val request = PutDataMapRequest.create(sessionPath(snapshot.sessionId)).apply {
            SnapshotCodec.write(dataMap, snapshot, deviceId)
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }

    override suspend fun fetchAll(): List<RemoteSnapshot> {
        val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(SESSION_PREFIX).build()
        val buffer = dataClient.getDataItems(uri, DataClient.FILTER_PREFIX).await()
        return try {
            buffer.mapNotNull { item -> SnapshotCodec.read(DataMapItem.fromDataItem(item).dataMap) }
        } finally {
            buffer.release()
        }
    }

    override suspend fun delete(sessionIds: List<String>) {
        val localNode = Wearable.getNodeClient(appContext).localNode.await()
        for (id in sessionIds) {
            val uri = Uri.Builder()
                .scheme(PutDataRequest.WEAR_URI_SCHEME)
                .authority(localNode.id)
                .path(sessionPath(id))
                .build()
            dataClient.deleteDataItems(uri).await()
        }
    }

    companion object {
        const val PATH_ROOT = "/pradarshana"
        const val SESSION_PREFIX = "$PATH_ROOT/session"

        fun sessionPath(sessionId: String) = "$SESSION_PREFIX/$sessionId"
    }
}
