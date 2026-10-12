package com.pradarshana.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class CapturedLocation(val latitude: Double, val longitude: Double)

/** Best-effort place lookup. Returns null when location is unavailable, so starting a session never waits forever. */
fun Context.hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

fun interface LocationCapture {
    suspend fun capture(): CapturedLocation?
}

class FusedLocationCapture(context: Context) : LocationCapture {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override suspend fun capture(): CapturedLocation? = withTimeoutOrNull(5_000) {
        runCatching {
            val last = client.lastLocation.await()
            val recent = last?.takeIf { System.currentTimeMillis() - it.time < RECENT_FIX_MILLIS }
            val fix = recent ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            fix?.let { CapturedLocation(it.latitude, it.longitude) }
        }.getOrNull()
    }

    private companion object {
        const val RECENT_FIX_MILLIS = 10 * 60_000L
    }
}
