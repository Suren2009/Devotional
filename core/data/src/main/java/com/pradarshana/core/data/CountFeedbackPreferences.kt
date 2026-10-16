package com.pradarshana.core.data

import android.content.Context
import com.pradarshana.core.model.AutoCountDistances
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Counting-screen options. Vibration and darkening stay on until turned off. Automatic counting stays off. */
class CountFeedbackPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _vibrateOnCount = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE_ON_COUNT, true))
    val vibrateOnCount: StateFlow<Boolean> = _vibrateOnCount.asStateFlow()
    private val _blackoutAfterTwoTaps = MutableStateFlow(prefs.getBoolean(KEY_BLACKOUT_AFTER_TWO_TAPS, true))
    val blackoutAfterTwoTaps: StateFlow<Boolean> = _blackoutAfterTwoTaps.asStateFlow()
    private val _autoCount = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COUNT, false))
    val autoCount: StateFlow<Boolean> = _autoCount.asStateFlow()
    private val _awayMeters = MutableStateFlow(prefs.getInt(KEY_AWAY_METERS, AutoCountDistances.DEFAULT_AWAY_METERS))
    val awayMeters: StateFlow<Int> = _awayMeters.asStateFlow()
    private val _returnMeters = MutableStateFlow(prefs.getInt(KEY_RETURN_METERS, AutoCountDistances.DEFAULT_RETURN_METERS))
    val returnMeters: StateFlow<Int> = _returnMeters.asStateFlow()

    fun setVibrateOnCount(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE_ON_COUNT, enabled).commit()
        _vibrateOnCount.value = enabled
    }

    fun setBlackoutAfterTwoTaps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLACKOUT_AFTER_TWO_TAPS, enabled).commit()
        _blackoutAfterTwoTaps.value = enabled
    }

    fun setAutoCount(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COUNT, enabled).commit()
        _autoCount.value = enabled
    }

    /** Saves a valid pair. A return distance that is not shorter than the walk-away distance is rejected. */
    fun setDistances(awayMeters: Int, returnMeters: Int) {
        if (!AutoCountDistances.isValid(awayMeters, returnMeters)) return
        prefs.edit().putInt(KEY_AWAY_METERS, awayMeters).putInt(KEY_RETURN_METERS, returnMeters).commit()
        _awayMeters.value = awayMeters
        _returnMeters.value = returnMeters
    }

    companion object {
        private const val PREFS = "pradarshana_settings"
        const val KEY_VIBRATE_ON_COUNT = "vibrate_on_count"
        const val KEY_BLACKOUT_AFTER_TWO_TAPS = "blackout_after_two_taps"
        const val KEY_AUTO_COUNT = "auto_count"
        const val KEY_AWAY_METERS = "auto_count_away_meters"
        const val KEY_RETURN_METERS = "auto_count_return_meters"
    }
}
