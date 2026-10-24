package com.pradarshana.core.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibrations for a finished session and for each counted tap. */
object CompletionVibration {
    const val DURATION_MILLIS = 500L
    const val TAP_DURATION_MILLIS = 70L
    private const val COMPLETION_AMPLITUDE = 80

    fun start(context: Context) = pulse(context, DURATION_MILLIS, COMPLETION_AMPLITUDE)

    fun tap(context: Context) = pulse(context, TAP_DURATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)

    /** A stronger two-beat pulse for the halfway guru bead and for the final name when that option is on. */
    fun guru(context: Context) {
        val vibrator = vibrator(context.applicationContext) ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 220, 90, 320),
                intArrayOf(0, 255, 0, 255),
                -1,
            ),
        )
    }

    private fun pulse(context: Context, durationMillis: Long, amplitude: Int) {
        val vibrator = vibrator(context.applicationContext) ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, amplitude))
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
