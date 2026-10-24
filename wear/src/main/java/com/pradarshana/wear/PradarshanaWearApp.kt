package com.pradarshana.wear

import android.app.Application
import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.pradarshana.core.data.PradarshanaContainer
import com.pradarshana.core.data.PradarshanaContainerOwner
import com.pradarshana.wear.complication.TodayComplicationService
import com.pradarshana.wear.tile.PradarshanaTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PradarshanaWearApp : Application(), PradarshanaContainerOwner {
    override val container: PradarshanaContainer by lazy { PradarshanaContainer(this, devicePrefix = "watch") }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            container.repository.observeOngoingSession().collect {
                runCatching {
                    TileService.getUpdater(this@PradarshanaWearApp)
                        .requestUpdate(PradarshanaTileService::class.java)
                }
                runCatching {
                    ComplicationDataSourceUpdateRequester.create(
                        this@PradarshanaWearApp,
                        ComponentName(this@PradarshanaWearApp, TodayComplicationService::class.java),
                    ).requestUpdateAll()
                }
            }
        }
    }
}
