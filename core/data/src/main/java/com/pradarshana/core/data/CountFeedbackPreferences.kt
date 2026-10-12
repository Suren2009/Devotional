package com.pradarshana.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether each + gives a short vibration. On until the devotee turns it off. */
class CountFeedbackPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _vibrateOnCount = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE_ON_COUNT, true))
    val vibrateOnCount: StateFlow<Boolean> = _vibrateOnCount.asStateFlow()

    fun setVibrateOnCount(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE_ON_COUNT, enabled).commit()
        _vibrateOnCount.value = enabled
    }

    companion object {
        private const val PREFS = "pradarshana_settings"
        const val KEY_VIBRATE_ON_COUNT = "vibrate_on_count"
    }
}
