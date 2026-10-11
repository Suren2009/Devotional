package com.pradarshana.core.data

import android.content.Context
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.data.sync.DataLayerSyncTransport
import com.pradarshana.core.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID

/** Manual dependency container shared by the phone and watch applications. */
class PradarshanaContainer(context: Context, devicePrefix: String) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val deviceId: String = DeviceIdStore.getOrCreate(appContext, devicePrefix)
    val database: PradarshanaDatabase = PradarshanaDatabase.create(appContext)
    val repository = SessionRepository(database, deviceId)
    val syncManager = SyncManager(repository, DataLayerSyncTransport(appContext, deviceId), appScope)

    init {
        syncManager.start()
    }
}

interface PradarshanaContainerOwner {
    val container: PradarshanaContainer
}

/** Stable random identifier for this installation, used as the G-Counter key. */
object DeviceIdStore {
    private const val PREFS = "pradarshana_device"
    private const val KEY = "device_id"

    fun getOrCreate(context: Context, prefix: String): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        val id = "$prefix-${UUID.randomUUID()}"
        prefs.edit().putString(KEY, id).commit()
        return id
    }
}
