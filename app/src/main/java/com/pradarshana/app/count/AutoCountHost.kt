package com.pradarshana.app.count

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pradarshana.core.data.PradarshanaContainerOwner
import com.pradarshana.core.data.location.hasLocationPermission
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionKind

/** Starts automatic counting while a Pradarshana is in progress and the setting is on. */
@Composable
fun AutoCountHost(session: PradarshanaSession?) {
    val context = LocalContext.current
    val feedback = (context.applicationContext as? PradarshanaContainerOwner)?.container?.countFeedback
    val enabled = if (feedback == null) {
        false
    } else {
        val value by feedback.autoCount.collectAsStateWithLifecycle()
        value
    }
    val shouldRun = enabled && session != null && session.kind == SessionKind.PRADARSHANA && session.status.isOngoing
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) AutoCountService.start(context)
    }
    LaunchedEffect(shouldRun, session?.id) {
        if (!shouldRun) return@LaunchedEffect
        if (context.hasLocationPermission()) {
            AutoCountService.start(context)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
