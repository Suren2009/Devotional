package com.pradarshana.wear.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.StartResult
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.data.presentation.CounterViewModel
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus
import com.pradarshana.wear.ui.theme.WearTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w227dp-h227dp-round-watch-xhdpi")
class WearCounterScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: PradarshanaDatabase
    private lateinit var repo: SessionRepository
    private var completions = 0
    private var ended = 0

    @Before
    fun setUp() {
        db = PradarshanaDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        repo = SessionRepository(db, "watch-test")
    }

    @After
    fun tearDown() = db.close()

    private fun ComposeTestRule.waitUntilReal(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (true) {
            waitForIdle()
            if (condition()) return
            check(System.currentTimeMillis() < deadline) { "Condition not met within $timeoutMillis ms" }
            Thread.sleep(10)
        }
    }

    private fun showCounter(target: Int): String {
        val id = runBlocking { (repo.startSession(target) as StartResult.Started).sessionId }
        val vm = CounterViewModel(repo, id)
        compose.setContent {
            WearTheme {
                WearCounterRoute(id, onCompleted = { completions++ }, onEnded = { ended++ }, viewModel = vm)
            }
        }
        compose.waitUntilReal { compose.onAllNodes(hasTestTag("wear_count"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        return id
    }

    private fun tapAndWait(expected: Int) {
        compose.onNodeWithTag("wear_increment").performClick()
        compose.waitUntilReal {
            compose.onAllNodes(hasTestTag("wear_count").and(hasText("$expected")), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun countsRoundsAndShowsRemaining() {
        val id = showCounter(108)
        tapAndWait(1)
        repeat(26) { compose.onNodeWithTag("wear_increment").performClick() }
        compose.waitUntilReal { runBlocking { repo.getSession(id)!!.completedCount } == 27 }
        compose.onNodeWithTag("battery_saver").performTouchInput { doubleClick(Offset(4f, 4f)) }
        compose.waitUntilReal {
            compose.onAllNodes(hasTestTag("wear_count"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("wear_count", useUnmergedTree = true).assertTextEquals("27")
        compose.onNodeWithText("of 108 · 81 left", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("27 of 108 rounds, 81 remaining").assertExists()
    }

    @Test
    fun sessionCountedEntirelyOnWatchCompletesOnce() {
        val id = showCounter(5)
        repeat(5) { compose.onNodeWithTag("wear_increment").performClick() }
        compose.waitUntilReal { completions == 1 }
        compose.onNodeWithTag("wear_increment").performClick()
        compose.waitForIdle()
        Thread.sleep(100)
        compose.waitForIdle()
        assertEquals(1, completions)
        val session = runBlocking { repo.getSession(id)!! }
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(5, session.completedCount)
        assertEquals(listOf(id), runBlocking { repo.observeHistory().first().map { it.id } })
    }

    @Test
    fun pauseBlocksCountingAndEndRequiresConfirmation() {
        val id = showCounter(10)
        compose.onAllNodes(hasTestTag("wear_end")).fetchSemanticsNodes().let { assertEquals(0, it.size) }
        compose.onNodeWithTag("wear_pause").performClick()
        compose.waitUntilReal { compose.onAllNodes(hasTestTag("wear_end")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("wear_increment").assertIsNotEnabled()

        compose.onNodeWithTag("wear_end").performClick()
        compose.onNodeWithText("End session?").assertExists()
        compose.onNodeWithTag("wear_confirm_end").performClick()
        compose.waitUntilReal { ended == 1 }
        assertEquals(SessionStatus.CANCELLED, runBlocking { repo.getSession(id)!!.status })
    }

    @Test
    fun homeOffersResumeForActiveSession() {
        var resumed: String? = null
        val ongoing = PradarshanaSession("w1", 21, 4, 0, null, SessionStatus.ACTIVE, 0)
        compose.setContent {
            WearTheme { WearHomeScreen(ongoing, onResume = { resumed = it }, onStart = {}) }
        }
        compose.onNodeWithText("Pradarshana").assertExists()
        compose.onNodeWithText("4 / 21").assertExists()
        compose.onNodeWithTag("wear_resume").performClick()
        assertEquals("w1", resumed)
    }

    @Test
    fun completionScreenShowsSummary() {
        val session = PradarshanaSession("w2", 108, 108, 0, 1_930_000, SessionStatus.COMPLETED, 1_930_000)
        var done = false
        compose.setContent { WearTheme { WearCompletionScreen(session, onDone = { done = true }) } }
        compose.onNodeWithTag("wear_completed_title").assertTextEquals("Pradarshana Completed!")
        compose.onNodeWithText("108 / 108 rounds").assertExists()
        compose.onNodeWithText("32:10").assertExists()
        compose.onNodeWithText("Done").performClick()
        assertEquals(true, done)
    }
}
