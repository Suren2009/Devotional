package com.pradarshana.core.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** A gentle vibration when the target number of rounds is reached. */
object CompletionVibration {
    const val DURATION_MILLIS = 3_000L
    private const val AMPLITUDE = 80

    fun start(context: Context) {
        val vibrator = vibrator(context.applicationContext) ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(DURATION_MILLIS, AMPLITUDE))
    }

    private fun vibrator(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    }.getOrNull()
}
