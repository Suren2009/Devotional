package com.pradarshana.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.pradarshana.wear.tile.QuickStart
import com.pradarshana.wear.ui.WearNavHost
import com.pradarshana.wear.ui.theme.WearTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as PradarshanaWearApp).container
    private var quickStartToken by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) consumeQuickStart(intent)
        setContent {
            WearTheme {
                WearNavHost(
                    repository = container.repository,
                    quickStartToken = quickStartToken,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeQuickStart(intent)
    }

    override fun onResume() {
        super.onResume()
        container.syncManager.requestReconcile()
    }

    private fun consumeQuickStart(intent: Intent?) {
        if (intent?.getBooleanExtra(QuickStart.EXTRA, false) == true) {
            quickStartToken += 1
            intent.removeExtra(QuickStart.EXTRA)
        }
    }
}
