package com.pradarshana.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Counting-screen options. Both stay on until the devotee turns them off. */
class CountFeedbackPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _vibrateOnCount = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE_ON_COUNT, true))
    val vibrateOnCount: StateFlow<Boolean> = _vibrateOnCount.asStateFlow()
    private val _blackoutAfterTwoTaps = MutableStateFlow(prefs.getBoolean(KEY_BLACKOUT_AFTER_TWO_TAPS, true))
    val blackoutAfterTwoTaps: StateFlow<Boolean> = _blackoutAfterTwoTaps.asStateFlow()

    fun setVibrateOnCount(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE_ON_COUNT, enabled).commit()
        _vibrateOnCount.value = enabled
    }

    fun setBlackoutAfterTwoTaps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLACKOUT_AFTER_TWO_TAPS, enabled).commit()
        _blackoutAfterTwoTaps.value = enabled
    }

    companion object {
        private const val PREFS = "pradarshana_settings"
        const val KEY_VIBRATE_ON_COUNT = "vibrate_on_count"
        const val KEY_BLACKOUT_AFTER_TWO_TAPS = "blackout_after_two_taps"
    }
}
