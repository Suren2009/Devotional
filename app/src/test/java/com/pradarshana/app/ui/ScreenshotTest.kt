package com.pradarshana.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.LocalView
import com.pradarshana.app.ui.completion.CompletionScreen
import com.pradarshana.app.ui.counter.CounterScreen
import com.pradarshana.app.ui.history.HistoryScreen
import com.pradarshana.app.ui.history.SessionDetailScreen
import com.pradarshana.app.ui.home.HomeScreen
import com.pradarshana.app.ui.start.StartScreen
import com.pradarshana.app.ui.theme.PradarshanaTheme
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders each phone screen to PNG. Run with `./gradlew :app:testDebugUnitTest -Pscreenshots`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val outDir = System.getProperty("screenshots.dir")?.let(::File)
    private val now = System.currentTimeMillis()
    private val active = PradarshanaSession("a", 108, 27, now - 14 * 60_000, null, SessionStatus.ACTIVE, now)
    private val history = listOf(
        PradarshanaSession("h1", 108, 108, now - 3_600_000, now - 1_480_000, SessionStatus.COMPLETED, now - 1_480_000),
        PradarshanaSession("h2", 21, 21, now - 90_000_000, now - 89_100_000, SessionStatus.COMPLETED, now - 89_100_000),
        PradarshanaSession("h3", 51, 32, now - 180_000_000, now - 178_800_000, SessionStatus.CANCELLED, now - 178_800_000),
    )

    @Before
    fun requireOutputDir() {
        assumeTrue("screenshots.dir not set", outDir != null)
        outDir!!.mkdirs()
    }

    private fun shoot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        var view: View? = null
        compose.setContent {
            view = LocalView.current
            PradarshanaTheme(darkTheme = dark) { content() }
        }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        val root = view!!.rootView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun homeLight() = shoot("phone_home_light") { HomeScreen(active, {}, {}, {}) }

    @Test fun homeDark() = shoot("phone_home_dark", dark = true) { HomeScreen(null, {}, {}, {}) }

    @Test fun start() = shoot("phone_start") {
        StartScreen("108", null, null, {}, {}, {})
    }

    @Test fun startError() = shoot("phone_start_error") {
        StartScreen("0", "The number of rounds must be greater than zero.", null, {}, {}, {})
    }

    @Test fun counterLight() = shoot("phone_counter_light") { CounterScreen(active, {}, {}, {}, {}) }

    @Test fun counterDark() = shoot("phone_counter_dark", dark = true) { CounterScreen(active, {}, {}, {}, {}) }

    @Test fun counterPaused() = shoot("phone_counter_paused") {
        CounterScreen(active.copy(status = SessionStatus.PAUSED), {}, {}, {}, {})
    }

    @Test fun completion() = shoot("phone_completion") { CompletionScreen(history[0], {}, {}, {}) }

    @Test fun historyList() = shoot("phone_history") { HistoryScreen(history, {}, {}, {}) }

    @Test fun historyDark() = shoot("phone_history_dark", dark = true) { HistoryScreen(history, {}, {}, {}) }

    @Test fun detail() = shoot("phone_detail") { SessionDetailScreen(history[2], {}) }
}
