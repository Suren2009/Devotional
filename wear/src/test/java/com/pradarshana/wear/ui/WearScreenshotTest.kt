package com.pradarshana.wear.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.platform.LocalView
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus
import com.pradarshana.wear.ui.theme.WearTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders each watch screen to PNG. Run with `./gradlew :wear:testDebugUnitTest -Pscreenshots`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w227dp-h227dp-round-watch-xhdpi")
class WearScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val outDir = System.getProperty("screenshots.dir")?.let(::File)
    private val now = System.currentTimeMillis()
    private val active = PradarshanaSession("a", 108, 27, now - 14 * 60_000, null, SessionStatus.ACTIVE, now)

    @Before
    fun requireOutputDir() {
        assumeTrue("screenshots.dir not set", outDir != null)
        outDir!!.mkdirs()
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        var view: View? = null
        compose.setContent {
            view = LocalView.current
            WearTheme { content() }
        }
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        val root = view!!.rootView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun home() = shoot("watch_home") { WearHomeScreen(active, {}, {}) }

    @Test fun target() = shoot("watch_target") { WearTargetScreen({}, {}) }

    @Test fun custom() = shoot("watch_custom_target") { WearCustomTargetScreen {} }

    @Test fun counter() = shoot("watch_counter") { WearCounterScreen(active, {}, {}, {}) }

    @Test fun paused() = shoot("watch_counter_paused") {
        WearCounterScreen(active.copy(status = SessionStatus.PAUSED), {}, {}, {})
    }

    @Test fun completion() = shoot("watch_completion") {
        WearCompletionScreen(active.copy(completedCount = 108, status = SessionStatus.COMPLETED, endTime = now), {})
    }
}
