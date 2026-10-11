package com.pradarshana.app.ui

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pradarshana.app.ui.counter.CounterRoute
import com.pradarshana.app.ui.counter.CounterViewModel
import com.pradarshana.app.ui.theme.PradarshanaTheme
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.StartResult
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.model.SessionStatus
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
        repeat(27) {
            compose.onNodeWithTag("increment_button").performClick()
            waitForText("completed_count", "${it + 1}")
        }
        compose.onNodeWithTag("remaining_count").assertContentDescriptionEquals("Remaining 81")
        compose.onNodeWithTag("progress_percent").assertContentDescriptionEquals("Progress 25%")
        compose.onNodeWithContentDescription("27 of 108 rounds completed, 25 percent").assertExists()
        assertEquals(27, runBlocking { repo.getSession(id)!!.completedCount })
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
}
