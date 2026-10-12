package com.pradarshana.app.ui.history

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pradarshana.core.model.PradarshanaSession

/** Opens the device's maps app on this session's saved place. */
fun openSessionInMaps(context: Context, session: PradarshanaSession) {
    val latitude = session.latitude ?: return
    val longitude = session.longitude ?: return
    val label = Uri.encode(session.displayTitle)
    val geo = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
    val mapApp = Intent(Intent.ACTION_VIEW, geo)
    val fallback = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=17/$latitude/$longitude"),
    )
    val chosen = if (mapApp.resolveActivity(context.packageManager) != null) mapApp else fallback
    context.startActivity(chosen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Small OpenStreetMap preview. Hidden when the tiles cannot be loaded. */
@Composable
fun SessionMapPreview(session: PradarshanaSession, modifier: Modifier = Modifier) {
    val latitude = session.latitude ?: return
    val longitude = session.longitude ?: return
    var failed by remember(latitude, longitude) { mutableStateOf(false) }
    if (failed) return
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag("session_map"),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame) failed = true
                    }
                }
                loadUrl(embedUrl(latitude, longitude))
            }
        },
    )
}

private fun embedUrl(latitude: Double, longitude: Double): String {
    val span = 0.008
    val left = longitude - span
    val right = longitude + span
    val bottom = latitude - span
    val top = latitude + span
    return "https://www.openstreetmap.org/export/embed.html" +
        "?bbox=$left,$bottom,$right,$top&layer=mapnik&marker=$latitude,$longitude"
}
