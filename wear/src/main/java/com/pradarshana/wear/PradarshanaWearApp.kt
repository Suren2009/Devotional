package com.pradarshana.wear

import android.app.Application
import androidx.wear.tiles.TileService
import com.pradarshana.core.data.PradarshanaContainer
import com.pradarshana.core.data.PradarshanaContainerOwner
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
                TileService.getUpdater(this@PradarshanaWearApp)
                    .requestUpdate(PradarshanaTileService::class.java)
            }
        }
    }
}
