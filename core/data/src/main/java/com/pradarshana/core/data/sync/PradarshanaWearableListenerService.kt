package com.pradarshana.core.data.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.pradarshana.core.data.PradarshanaContainerOwner
import kotlinx.coroutines.runBlocking

/** Wakes the app when the paired device publishes a session update. */
class PradarshanaWearableListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Items must be decoded before this callback returns; the buffer is released afterwards.
        val snapshots = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .filter { it.dataItem.uri.path?.startsWith(DataLayerSyncTransport.SESSION_PREFIX) == true }
            .mapNotNull { SnapshotCodec.read(DataMapItem.fromDataItem(it.dataItem).dataMap) }
        if (snapshots.isEmpty()) return

        val container = (application as? PradarshanaContainerOwner)?.container ?: return
        // Runs on a binder background thread; blocking keeps the service alive until data is stored.
        runBlocking { container.syncManager.onRemoteSnapshots(snapshots) }
    }
}
