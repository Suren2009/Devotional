package com.pradarshana.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pradarshana.app.ui.PradarshanaNavHost
import com.pradarshana.app.ui.theme.PradarshanaTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as PradarshanaApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PradarshanaTheme {
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
