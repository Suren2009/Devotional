package com.pradarshana.app.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import com.pradarshana.app.ui.counter.CounterRoute
import com.pradarshana.app.ui.counter.CounterScreen
import com.pradarshana.core.model.MalaSide
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.data.presentation.CounterViewModel
import com.pradarshana.app.ui.theme.PradarshanaTheme
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.StartResult
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CounterScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: PradarshanaDatabase
    private lateinit var repo: SessionRepository
    private var completedCalls = 0
    private var endedCalls = 0

    @Before
    fun setUp() {
        db = PradarshanaDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        repo = SessionRepository(db, "phone-test")
    }

    @After
    fun tearDown() = db.close()

    private fun showCounter(target: Int): String {
        val id = runBlocking { (repo.startSession(target) as StartResult.Started).sessionId }
        val vm = CounterViewModel(repo, id)
        compose.setContent {
            PradarshanaTheme {
                CounterRoute(
                    sessionId = id,
                    onBack = {},
                    onCompleted = { completedCalls++ },
                    onEnded = { endedCalls++ },
                    viewModel = vm,
                )
            }
        }
        compose.waitUntilReal { compose.onAllNodes(hasTestTag("completed_count"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        return id
    }

    private fun waitForText(tag: String, text: String) {
        compose.waitUntilReal {
            compose.onAllNodes(hasTestTag(tag).and(hasText(text)), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun showsInitialCountsForTarget() {
        showCounter(108)
        compose.onNodeWithTag("completed_count", useUnmergedTree = true).assertTextEquals("0")
        compose.onNodeWithTag("target_count").assertContentDescriptionEquals("Target 108")
        compose.onNodeWithTag("remaining_count").assertContentDescriptionEquals("Remaining 108")
        compose.onNodeWithTag("progress_percent").assertContentDescriptionEquals("Progress 0%")
    }

    @Test
    fun tappingPlusIncrementsAndUpdatesRemainingAndProgress() {
        val id = showCounter(108)
        compose.onNodeWithTag("increment_button").performClick()
        waitForText("completed_count", "1")
        repeat(26) { compose.onNodeWithTag("increment_button").performClick() }
        compose.waitUntilReal { runBlocking { repo.getSession(id)!!.completedCount } == 27 }
        compose.onNodeWithTag("battery_saver").performTouchInput { doubleClick(Offset(8f, 8f)) }
        compose.waitUntilReal {
            compose.onAllNodes(hasTestTag("completed_count"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("completed_count", useUnmergedTree = true).assertTextEquals("27")
        compose.onNodeWithTag("remaining_count").assertContentDescriptionEquals("Remaining 81")
        compose.onNodeWithTag("progress_percent").assertContentDescriptionEquals("Progress 25%")
        compose.onNodeWithContentDescription("27 of 108 rounds completed, 25 percent").assertExists()
        assertEquals(27, runBlocking { repo.getSession(id)!!.completedCount })
    }

    @Test
    fun twoPlusTapsBlackOutTheScreenUntilADoubleTapOutsideTheButton() {
        showCounter(108)
        compose.onNodeWithTag("increment_button").performClick()
        waitForText("completed_count", "1")
        compose.onNodeWithTag("battery_saver").assertDoesNotExist()
        compose.onNodeWithTag("increment_button").performClick()
        compose.waitUntilReal { compose.onAllNodes(hasTestTag("battery_saver")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("pause_button").assertDoesNotExist()
        compose.onNodeWithTag("completed_count", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("increment_button").assertExists()

        compose.onNodeWithTag("battery_saver").performTouchInput { doubleClick(Offset(8f, 8f)) }
        compose.waitUntilReal {
            compose.onAllNodes(hasTestTag("completed_count"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("pause_button").assertExists()
        compose.onNodeWithTag("battery_saver").assertDoesNotExist()
    }

    @Test
    fun blackoutStaysOffWhenTheSettingIsDisabled() {
        val session = PradarshanaSession("s", 108, 0, 0, null, SessionStatus.ACTIVE, 0)
        compose.setContent {
            PradarshanaTheme {
                CounterScreen(
                    session = session,
                    onIncrement = {},
                    onTogglePause = {},
                    onEndSession = {},
                    onBack = {},
                    batterySaverEnabled = false,
                )
            }
        }
        compose.onNodeWithTag("increment_button").performClick()
        compose.onNodeWithTag("increment_button").performClick()
        compose.onNodeWithTag("battery_saver").assertDoesNotExist()
        compose.onNodeWithTag("pause_button").assertExists()
    }

    @Test
    fun chantingCountHeadingReplacesTotalRounds() {
        val session = PradarshanaSession(
            "s",
            108,
            2,
            0,
            null,
            SessionStatus.ACTIVE,
            0,
            kind = com.pradarshana.core.model.SessionKind.CHANTING,
        )
        compose.setContent {
            PradarshanaTheme {
                CounterScreen(session, {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("count_heading", useUnmergedTree = true).assertTextEquals("No. of times chanting")
        compose.onNodeWithText("Total rounds").assertDoesNotExist()
    }

    @Test
    fun pauseDisablesCountingAndResumeEnablesIt() {
        val id = showCounter(10)
        compose.onNodeWithTag("pause_button").performClick()
        compose.waitUntilReal { compose.onAllNodes(hasText("Resume")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("increment_button").assertIsNotEnabled()
        assertEquals(SessionStatus.PAUSED, runBlocking { repo.getSession(id)!!.status })

        compose.onNodeWithTag("pause_button").performClick()
        compose.waitUntilReal { compose.onAllNodes(hasText("Pause")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("increment_button").assertIsEnabled()
    }

    @Test
    fun reachingTargetNavigatesToCompletionExactlyOnce() {
        val id = showCounter(3)
        repeat(3) {
            compose.onNodeWithTag("increment_button").performClick()
            compose.waitForIdle()
        }
        compose.waitUntilReal { completedCalls > 0 }
        repeat(2) { compose.onNodeWithTag("increment_button").performClick() }
        compose.waitForIdle()
        Thread.sleep(200)
        compose.waitForIdle()
        assertEquals(1, completedCalls)
        val session = runBlocking { repo.getSession(id)!! }
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(3, session.completedCount)
    }

    @Test
    fun endSessionAsksForConfirmation() {
        val id = showCounter(108)
        compose.onNodeWithTag("end_button").performClick()
        compose.onNodeWithText("End this Pradarshana?").assertExists()
        compose.onNodeWithText("Keep Counting").performClick()
        compose.onNodeWithText("End this Pradarshana?").assertDoesNotExist()
        assertEquals(SessionStatus.ACTIVE, runBlocking { repo.getSession(id)!!.status })

        compose.onNodeWithTag("end_button").performClick()
        compose.onNodeWithTag("confirm_end_button").performClick()
        compose.waitUntilReal { endedCalls == 1 }
        assertEquals(SessionStatus.CANCELLED, runBlocking { repo.getSession(id)!!.status })
    }

    @Test
    fun automaticCountingShowsTheRoundAndDoesNotAddOne() {
        var taps = 0
        val session = PradarshanaSession("s", 108, 4, 0, null, SessionStatus.PAUSED, 0)
        var keepOn = false
        compose.setContent {
            PradarshanaTheme {
                CounterScreen(
                    session = session,
                    onIncrement = { taps++ },
                    onTogglePause = {},
                    onEndSession = {},
                    onBack = {},
                    autoCount = true,
                )
                val view = LocalView.current
                SideEffect { keepOn = view.keepScreenOn }
            }
        }
        compose.onNodeWithTag("battery_saver").assertExists()
        compose.onNodeWithTag("auto_count_button").assertHasNoClickAction()
        compose.onNodeWithContentDescription("4 rounds, counted automatically").assertExists()
        compose.onNodeWithText("4").assertExists()
        assertEquals(0, taps)
        assertTrue(keepOn)
        compose.onNodeWithTag("battery_saver").performTouchInput { doubleClick(Offset(8f, 8f)) }
        compose.waitUntilReal { compose.onAllNodes(hasTestTag("pause_button")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("battery_saver").assertDoesNotExist()
        compose.onNodeWithTag("increment_button").assertDoesNotExist()
        compose.onNodeWithTag("auto_count_button").assertHasNoClickAction()
        assertEquals(0, taps)
        assertTrue(keepOn)
    }

    @Test
    fun chantingMalaDragCountsOneNamePerDownwardDrag() {
        var clicks = 0
        compose.setContent {
            PradarshanaTheme {
                CounterScreen(
                    session = chanting(),
                    onIncrement = { clicks++ },
                    onTogglePause = {},
                    onEndSession = {},
                    onBack = {},
                    batterySaverEnabled = false,
                    malaDrag = true,
                )
            }
        }
        compose.onNodeWithTag("increment_button").assertDoesNotExist()
        compose.onNodeWithTag("mala_strip").assertExists()
        compose.onNodeWithTag("mala_bead").assertExists()
        compose.onNodeWithContentDescription("Drag down to count one name").assertExists()
        assertTrue(compose.onNodeWithTag("mala_strip").fetchSemanticsNode().positionInRoot.x > 400f)

        compose.onNodeWithTag("mala_strip").performTouchInput {
            swipe(Offset(width / 2f, 24f), Offset(width / 2f, 48f), durationMillis = 80)
        }
        compose.waitForIdle()
        assertEquals(0, clicks)

        compose.onNodeWithTag("mala_strip").performTouchInput {
            swipe(Offset(width / 2f, height - 16f), Offset(width / 2f, 16f), durationMillis = 300)
        }
        compose.waitForIdle()
        assertEquals(0, clicks)

        compose.onNodeWithTag("mala_strip").performTouchInput {
            swipe(Offset(width / 2f, 12f), Offset(width / 2f, height - 12f), durationMillis = 400)
        }
        compose.waitForIdle()
        assertEquals(1, clicks)

        compose.onNodeWithTag("mala_strip").performTouchInput {
            swipe(Offset(width / 2f, 12f), Offset(width / 2f, height - 12f), durationMillis = 400)
        }
        compose.waitForIdle()
        assertEquals(2, clicks)
    }

    @Test
    fun malaDragOnTheLeftDoesNotCountWhilePaused() {
        var clicks = 0
        compose.setContent {
            PradarshanaTheme {
                CounterScreen(
                    session = chanting(status = SessionStatus.PAUSED),
                    onIncrement = { clicks++ },
                    onTogglePause = {},
                    onEndSession = {},
                    onBack = {},
                    batterySaverEnabled = false,
                    malaDrag = true,
                    malaSide = MalaSide.LEFT,
                )
            }
        }
        assertTrue(compose.onNodeWithTag("mala_strip").fetchSemanticsNode().positionInRoot.x < 20f)
        compose.onNodeWithTag("mala_strip").performTouchInput {
            swipe(Offset(width / 2f, 12f), Offset(width / 2f, height - 12f), durationMillis = 400)
        }
        compose.waitForIdle()
        assertEquals(0, clicks)
    }

    @Test
    fun pradarshanaKeepsThePlusButtonWhenMalaDragIsOn() {
        compose.setContent {
            PradarshanaTheme {
                CounterScreen(
                    session = PradarshanaSession("s", 108, 0, 0, null, SessionStatus.ACTIVE, 0),
                    onIncrement = {},
                    onTogglePause = {},
                    onEndSession = {},
                    onBack = {},
                    malaDrag = true,
                )
            }
        }
        compose.onNodeWithTag("increment_button").assertExists()
        compose.onNodeWithTag("mala_strip").assertDoesNotExist()
    }

    @Test
    fun darkenedChantingScreenStillCountsFromTheStrip() {
        var clicks = 0
        compose.setContent {
            PradarshanaTheme {
                CounterScreen(
                    session = chanting(),
                    onIncrement = { clicks++ },
                    onTogglePause = {},
                    onEndSession = {},
                    onBack = {},
                    malaDrag = true,
                )
            }
        }
        repeat(2) {
            compose.onNodeWithTag("mala_strip").performTouchInput {
                swipe(Offset(width / 2f, 12f), Offset(width / 2f, height - 12f), durationMillis = 400)
            }
            compose.waitForIdle()
        }
        assertEquals(2, clicks)
        compose.onNodeWithTag("battery_saver").assertExists()
        compose.onNodeWithTag("increment_button").assertDoesNotExist()
        compose.onNodeWithTag("mala_strip").performTouchInput {
            swipe(Offset(width / 2f, 12f), Offset(width / 2f, height - 12f), durationMillis = 400)
        }
        compose.waitForIdle()
        assertEquals(3, clicks)
        compose.onNodeWithTag("battery_saver").performTouchInput { doubleClick(Offset(8f, 8f)) }
        compose.waitUntilReal { compose.onAllNodes(hasTestTag("pause_button")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("battery_saver").assertDoesNotExist()
        compose.onNodeWithTag("increment_button").assertDoesNotExist()
        compose.onNodeWithTag("mala_strip").assertExists()
    }

    private fun chanting(count: Int = 0, status: SessionStatus = SessionStatus.ACTIVE) = PradarshanaSession(
        "s",
        108,
        count,
        0,
        null,
        status,
        0,
        kind = SessionKind.CHANTING,
    )
}
