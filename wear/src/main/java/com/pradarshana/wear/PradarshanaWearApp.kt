package com.pradarshana.wear

import android.app.Application
import com.pradarshana.core.data.PradarshanaContainer
import com.pradarshana.core.data.PradarshanaContainerOwner

class PradarshanaWearApp : Application(), PradarshanaContainerOwner {
    override val container: PradarshanaContainer by lazy { PradarshanaContainer(this, devicePrefix = "watch") }
}
