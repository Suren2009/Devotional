package com.pradarshana.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.pradarshana.wear.ui.WearNavHost
import com.pradarshana.wear.ui.theme.WearTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as PradarshanaWearApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearTheme {
                WearNavHost(repository = container.repository)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.syncManager.requestReconcile()
    }
}
