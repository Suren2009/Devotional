package com.pradarshana.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.pradarshana.app.ui.settings.SettingsScreen
import com.pradarshana.core.data.CountFeedbackPreferences
import com.pradarshana.app.ui.theme.PradarshanaTheme
import com.pradarshana.app.ui.theme.ThemeMode
import com.pradarshana.app.ui.theme.ThemeStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun themeChoiceIsRemembered() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("pradarshana_settings", Context.MODE_PRIVATE).edit().clear().commit()
        val store = ThemeStore(context)
        assertEquals(ThemeMode.SYSTEM, store.mode.value)
        store.set(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ThemeStore(context).mode.value)
        store.set(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, store.mode.value)
    }

    @Test
    fun choosingDarkReportsDark() {
        var chosen = ThemeMode.SYSTEM
        compose.setContent {
            PradarshanaTheme {
                SettingsScreen(mode = chosen, onMode = { chosen = it }, onBack = {})
            }
        }
        compose.onNodeWithText("Light").assertExists()
        compose.onNodeWithText("Dark").assertExists()
        compose.onNodeWithText("System").assertExists()
        compose.onNodeWithTag("theme_DARK").performClick()
        assertEquals(ThemeMode.DARK, chosen)
    }

    @Test
    fun countVibrationIsOnUntilTurnedOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("pradarshana_settings", Context.MODE_PRIVATE)
            .edit()
            .remove(CountFeedbackPreferences.KEY_VIBRATE_ON_COUNT)
            .commit()
        val prefs = CountFeedbackPreferences(context)
        assertTrue(prefs.vibrateOnCount.value)
        var enabled = true
        compose.setContent {
            PradarshanaTheme {
                SettingsScreen(
                    mode = ThemeMode.SYSTEM,
                    onMode = {},
                    onBack = {},
                    vibrateOnCount = enabled,
                    onVibrateOnCount = { enabled = it },
                )
            }
        }
        compose.onNodeWithText("Vibrate on each count").assertExists()
        compose.onNodeWithTag("vibrate_on_count").performClick()
        assertFalse(enabled)
        prefs.setVibrateOnCount(false)
        assertFalse(CountFeedbackPreferences(context).vibrateOnCount.value)
    }

    @Test
    fun darkenAfterTwoCountsIsOnUntilTurnedOff() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("pradarshana_settings", Context.MODE_PRIVATE)
            .edit()
            .remove(CountFeedbackPreferences.KEY_BLACKOUT_AFTER_TWO_TAPS)
            .commit()
        val prefs = CountFeedbackPreferences(context)
        assertTrue(prefs.blackoutAfterTwoTaps.value)
        var enabled = true
        compose.setContent {
            PradarshanaTheme {
                SettingsScreen(
                    mode = ThemeMode.SYSTEM,
                    onMode = {},
                    onBack = {},
                    blackoutAfterTwoTaps = enabled,
                    onBlackoutAfterTwoTaps = { enabled = it },
                )
            }
        }
        compose.onNodeWithText("Darken after two counts").assertExists()
        compose.onNodeWithTag("blackout_after_two_taps").performClick()
        assertFalse(enabled)
        prefs.setBlackoutAfterTwoTaps(false)
        assertFalse(CountFeedbackPreferences(context).blackoutAfterTwoTaps.value)
    }

    @Test
    fun automaticCountingIsOffUntilTurnedOnAndDistancesAreSaved() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("pradarshana_settings", Context.MODE_PRIVATE)
            .edit()
            .remove(CountFeedbackPreferences.KEY_AUTO_COUNT)
            .remove(CountFeedbackPreferences.KEY_AWAY_METERS)
            .remove(CountFeedbackPreferences.KEY_RETURN_METERS)
            .commit()
        val prefs = CountFeedbackPreferences(context)
        assertFalse(prefs.autoCount.value)
        assertEquals(30, prefs.awayMeters.value)
        assertEquals(20, prefs.returnMeters.value)

        var enabled by mutableStateOf(false)
        var away = 30
        var back = 20
        compose.setContent {
            PradarshanaTheme {
                SettingsScreen(
                    mode = ThemeMode.SYSTEM,
                    onMode = {},
                    onBack = {},
                    autoCount = enabled,
                    onAutoCount = { enabled = it },
                    awayMeters = away,
                    returnMeters = back,
                    onDistances = { nextAway, nextBack ->
                        away = nextAway
                        back = nextBack
                    },
                )
            }
        }
        compose.onNodeWithTag("auto_count_away").assertDoesNotExist()
        compose.onNodeWithTag("auto_count").performScrollTo().performClick()
        assertTrue(enabled)
        compose.onNodeWithTag("auto_count_away").performScrollTo().assertExists()
        compose.onNodeWithTag("auto_count_return").performScrollTo().performTextReplacement("15")
        assertEquals(30, away)
        assertEquals(15, back)
        prefs.setAutoCount(true)
        prefs.setDistances(40, 15)
        prefs.setDistances(10, 10)
        val stored = CountFeedbackPreferences(context)
        assertTrue(stored.autoCount.value)
        assertEquals(40, stored.awayMeters.value)
        assertEquals(15, stored.returnMeters.value)
    }
}
