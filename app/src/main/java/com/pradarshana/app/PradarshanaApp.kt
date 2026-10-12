package com.pradarshana.app

import android.app.Application
import com.pradarshana.app.ui.theme.ThemeStore
import com.pradarshana.core.data.PradarshanaContainer
import com.pradarshana.core.data.PradarshanaContainerOwner

class PradarshanaApp : Application(), PradarshanaContainerOwner {
    override val container: PradarshanaContainer by lazy { PradarshanaContainer(this, devicePrefix = "phone") }
    val themeStore: ThemeStore by lazy { ThemeStore(this) }
}
