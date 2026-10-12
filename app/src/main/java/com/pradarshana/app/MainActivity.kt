package com.pradarshana.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pradarshana.app.ui.PradarshanaNavHost
import com.pradarshana.app.ui.theme.PradarshanaTheme
import com.pradarshana.app.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    private val app get() = application as PradarshanaApp
    private val container get() = app.container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by app.themeStore.mode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            PradarshanaTheme(darkTheme = dark) {
                PradarshanaNavHost(repository = container.repository)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pick up anything the watch counted while the phone app was in the background.
        container.syncManager.requestReconcile()
    }
}
