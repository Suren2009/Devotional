package com.pradarshana.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("System", "Follow the phone’s light or dark setting"),
    LIGHT("Light", "Cream and saffron"),
    DARK("Dark", "Warm dark tones"),
}

/** Phone appearance choice. The watch stays dark. */
class ThemeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).commit()
        _mode.value = mode
    }

    private fun read(): ThemeMode = when (prefs.getString(KEY, null)) {
        ThemeMode.LIGHT.name -> ThemeMode.LIGHT
        ThemeMode.DARK.name -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    companion object {
        private const val PREFS = "pradarshana_settings"
        private const val KEY = "theme_mode"
    }
}
