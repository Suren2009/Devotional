package com.pradarshana.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.pradarshana.app.ui.history.HistoryScreen
import com.pradarshana.app.ui.home.HomeScreen
import com.pradarshana.app.ui.start.StartScreen
import com.pradarshana.app.ui.start.StartViewModel
import com.pradarshana.app.ui.theme.PradarshanaTheme
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StartAndHistoryScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: PradarshanaDatabase
    private lateinit var repo: SessionRepository

    @Before
    fun setUp() {
        db = PradarshanaDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        repo = SessionRepository(db, "phone-test")
    }

    @After
    fun tearDown() = db.close()

    private fun showStart(vm: StartViewModel) {
        compose.setContent {
            PradarshanaTheme {
                StartScreen(
                    input = vm.input,
                    error = vm.error,
                    ongoing = null,
                    onInputChange = vm::onInputChange,
                    onStart = vm::start,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun zeroTargetShowsValidationErrorAndDoesNotStart() {
        val vm = StartViewModel(repo)
        showStart(vm)
        compose.onNodeWithTag("target_input").performTextInput("0")
        compose.onNodeWithTag("confirm_start_button").performClick()
        compose.onNodeWithText("The number of rounds must be greater than zero.").assertIsDisplayed()
        compose.onNodeWithTag("target_input").performTextReplacement("")
        compose.onNodeWithTag("confirm_start_button").performClick()
        compose.onNodeWithText("Please enter the number of rounds.").assertIsDisplayed()
        assertEquals(null, runBlocking { repo.getOngoingSession() })
    }

    @Test
    fun nonDigitInputIsIgnored() {
        val vm = StartViewModel(repo)
        showStart(vm)
        compose.onNodeWithTag("target_input").performTextInput("-1a2")
        assertEquals("12", vm.input)
    }

    @Test
    fun validTargetStartsSession() {
        val vm = StartViewModel(repo)
        var startedId: String? = null
        compose.setContent {
            PradarshanaTheme {
                LaunchedEffect(vm) { vm.started.collect { startedId = it } }
                StartScreen(vm.input, vm.error, null, vm::onInputChange, vm::start, onBack = {})
            }
        }
        compose.onNodeWithContentDescription("108 rounds").performClick()
        compose.onNodeWithTag("confirm_start_button").performClick()
        compose.waitUntilReal { startedId != null }
        val session = runBlocking { repo.getSession(startedId!!) }
        assertNotNull(session)
        assertEquals(108, session!!.targetCount)
        assertEquals(0, session.completedCount)
        assertEquals(SessionStatus.ACTIVE, session.status)
    }

    @Test
    fun homeShowsResumeCardForOngoingSession() {
        var resumed: String? = null
        val ongoing = PradarshanaSession("s1", 108, 27, 0, null, SessionStatus.ACTIVE, 0)
        compose.setContent {
            PradarshanaTheme { HomeScreen(ongoing, onStart = {}, onResume = { resumed = it }, onHistory = {}) }
        }
        compose.onNodeWithText("Pradarshana").assertIsDisplayed()
        compose.onNodeWithText("27 of 108 rounds · 81 remaining").assertExists()
        compose.onNodeWithTag("resume_button").performClick()
        assertEquals("s1", resumed)
    }

    @Test
    fun historyListsSessionsWithStatusAndClearsAfterConfirmation() {
        var cleared = false
        var opened: String? = null
        val sessions = listOf(
            PradarshanaSession("new", 108, 108, 2_000_000, 2_600_000, SessionStatus.COMPLETED, 2_600_000),
            PradarshanaSession("old", 21, 9, 1_000_000, 1_300_000, SessionStatus.CANCELLED, 1_300_000),
        )
        compose.setContent {
            PradarshanaTheme {
                HistoryScreen(sessions, onBack = {}, onOpen = { opened = it }, onClear = { cleared = true })
            }
        }
        compose.onNodeWithText("Completed").assertExists()
        compose.onNodeWithText("Incomplete").assertExists()
        compose.onNodeWithText(" / 108 rounds").assertExists()
        compose.onNodeWithText("10m 00s").assertExists()

        compose.onNode(hasText(" / 21 rounds"), useUnmergedTree = true).performClick()
        assertEquals("old", opened)

        compose.onNodeWithTag("clear_history").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(!cleared)
        compose.onNodeWithTag("clear_history").performClick()
        compose.onNodeWithTag("confirm_clear").performClick()
        assertTrue(cleared)
    }
}
