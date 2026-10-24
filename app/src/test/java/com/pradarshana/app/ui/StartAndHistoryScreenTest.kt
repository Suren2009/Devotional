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
import com.pradarshana.core.model.MalaSide
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionStatus
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
                    onStart = { vm.start() },
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
                StartScreen(
                    input = vm.input,
                    error = vm.error,
                    ongoing = null,
                    onInputChange = vm::onInputChange,
                    onStart = { vm.start() },
                    onBack = {},
                    title = vm.title,
                    onTitleChange = vm::onTitleChange,
                )
            }
        }
        compose.onNodeWithTag("title_input").performTextInput("Kapaleeshwarar")
        compose.onNodeWithContentDescription("108 rounds").performClick()
        compose.onNodeWithTag("confirm_start_button").performClick()
        compose.waitUntilReal { startedId != null }
        val session = runBlocking { repo.getSession(startedId!!) }
        assertNotNull(session)
        assertEquals(108, session!!.targetCount)
        assertEquals(0, session.completedCount)
        assertEquals(SessionStatus.ACTIVE, session.status)
        assertEquals("Kapaleeshwarar", session.title)
        assertEquals(false, session.hasLocation)
    }

    @Test
    fun latestTitleIsPrefilledAndLatestCountIsTheFirstChip() {
        runBlocking { repo.startSession(54, "Meenakshi", kind = SessionKind.CHANTING) }
        val vm = StartViewModel(repo)
        compose.setContent {
            PradarshanaTheme {
                StartScreen(
                    input = vm.input,
                    error = vm.error,
                    ongoing = null,
                    onInputChange = vm::onInputChange,
                    onStart = {},
                    onBack = {},
                    title = vm.title,
                    onTitleChange = vm::onTitleChange,
                    counts = vm.counts,
                    kind = vm.kind,
                    onKindChange = vm::onKindChange,
                )
            }
        }
        compose.waitUntilReal { vm.title == "Meenakshi" && vm.counts.first() == 54 && vm.kind == SessionKind.CHANTING }
        assertEquals(listOf(54, 3, 9, 18, 108), vm.counts)
        compose.onNodeWithText("Meenakshi").assertExists()
        compose.onNodeWithTag("count_54").assertExists()
        compose.onNodeWithTag("mantra_input").assertExists()
        compose.onNodeWithTag("mantra_vishnu").assertDoesNotExist()
    }

    @Test
    fun editingTheTitleIsNotOverwrittenByTheLatestSession() {
        runBlocking { repo.startSession(9, "Old temple") }
        val vm = StartViewModel(repo)
        vm.onTitleChange("New temple")
        compose.waitUntilReal { vm.counts.first() == 9 }
        assertEquals("New temple", vm.title)
    }

    @Test
    fun homeShowsResumeCardForOngoingSession() {
        var resumed: String? = null
        var openedSettings = false
        val ongoing = PradarshanaSession("s1", 108, 27, 0, null, SessionStatus.ACTIVE, 0)
        compose.setContent {
            PradarshanaTheme {
                HomeScreen(
                    ongoing,
                    onStart = {},
                    onResume = { resumed = it },
                    onHistory = {},
                    onSettings = { openedSettings = true },
                )
            }
        }
        compose.onNodeWithText("Pradarshana").assertIsDisplayed()
        compose.onNodeWithText("27 of 108 rounds · 81 remaining").assertExists()
        compose.onNodeWithTag("resume_button").performClick()
        assertEquals("s1", resumed)
        compose.onNodeWithTag("settings_button").performClick()
        assertTrue(openedSettings)
    }

    @Test
    fun historyListsSessionsWithStatusAndClearsAfterConfirmation() {
        var cleared = false
        var opened: String? = null
        var mapped: String? = null
        var deleted: String? = null
        val sessions = listOf(
            PradarshanaSession(
                "new", 108, 108, 2_000_000, 2_600_000, SessionStatus.COMPLETED, 2_600_000,
                title = "Kapaleeshwarar", latitude = 13.033, longitude = 80.270,
            ),
            PradarshanaSession(
                "old", 21, 9, 1_000_000, 1_300_000, SessionStatus.CANCELLED, 1_300_000,
                kind = SessionKind.CHANTING,
            ),
        )
        compose.setContent {
            PradarshanaTheme {
                HistoryScreen(
                    sessions,
                    onBack = {},
                    onOpen = { opened = it },
                    onClear = { cleared = true },
                    onShowMap = { mapped = it.id },
                    onDelete = { deleted = it.id },
                )
            }
        }
        compose.onNodeWithText("Kapaleeshwarar").assertExists()
        compose.onNodeWithText("Untitled").assertExists()
        compose.onNodeWithTag("session_kind_new", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("mandala", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("japa_mala", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Chanting", useUnmergedTree = true).assertExists()
        compose.onNodeWithText(" / 21 names", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Completed").assertExists()
        compose.onNodeWithText("Incomplete").assertExists()
        compose.onNodeWithText(" / 108 rounds").assertExists()
        compose.onNodeWithText("10m 00s").assertExists()

        compose.onNodeWithTag("show_on_map_new").performClick()
        assertEquals("new", mapped)
        compose.onNode(hasText(" / 21 names"), useUnmergedTree = true).performClick()
        assertEquals("old", opened)

        compose.onNodeWithTag("delete_session_old").performClick()
        compose.onNodeWithText("Delete this Pradarshana?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(null, deleted)
        compose.onNodeWithTag("delete_session_old").performClick()
        compose.onNodeWithTag("confirm_delete").performClick()
        assertEquals("old", deleted)

        compose.onNodeWithTag("clear_history").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(!cleared)
        compose.onNodeWithTag("clear_history").performClick()
        compose.onNodeWithTag("confirm_clear").performClick()
        assertTrue(cleared)
    }

    @Test
    fun pradakshanaChoosesManualOrAutomaticAndChantingDoesNot() {
        var automatic by mutableStateOf(false)
        var kind by mutableStateOf(SessionKind.PRADARSHANA)
        compose.setContent {
            PradarshanaTheme {
                StartScreen(
                    input = "",
                    error = null,
                    ongoing = null,
                    onInputChange = {},
                    onStart = {},
                    onBack = {},
                    kind = kind,
                    onKindChange = { kind = it },
                    automatic = automatic,
                    onAutomaticChange = { automatic = it },
                )
            }
        }
        compose.onNodeWithTag("count_mode_manual").assertExists()
        compose.onNodeWithText("Target rounds").assertExists()
        compose.onNodeWithText("Tap + for each round. This is the usual counting screen.").assertExists()
        compose.onNodeWithTag("count_mode_auto").performClick()
        assertEquals(true, automatic)
        compose.onNodeWithTag("kind_CHANTING").performClick()
        compose.onNodeWithText("No. of times chanting").assertExists()
        compose.onNodeWithText("Target rounds").assertDoesNotExist()
        compose.onNodeWithTag("count_mode_manual").assertDoesNotExist()
        compose.onNodeWithTag("count_mode_auto").assertDoesNotExist()
    }

    @Test
    fun chantingChoosesPlusOrAMalaDragAndPradarshanaDoesNot() {
        var mala by mutableStateOf(false)
        var side by mutableStateOf(MalaSide.RIGHT)
        var kind by mutableStateOf(SessionKind.PRADARSHANA)
        compose.setContent {
            PradarshanaTheme {
                StartScreen(
                    input = "",
                    error = null,
                    ongoing = null,
                    onInputChange = {},
                    onStart = {},
                    onBack = {},
                    kind = kind,
                    onKindChange = { kind = it },
                    malaDrag = mala,
                    onMalaDragChange = { mala = it },
                    malaSide = side,
                    onMalaSideChange = { side = it },
                )
            }
        }
        compose.onNodeWithTag("mala_mode_plus").assertDoesNotExist()
        compose.onNodeWithTag("mala_mode_drag").assertDoesNotExist()
        compose.onNodeWithTag("kind_CHANTING").performClick()
        compose.onNodeWithTag("mala_mode_plus").assertExists()
        compose.onNodeWithText("Tap + for each name. This is the usual counting screen.").assertExists()
        compose.onNodeWithTag("mala_side_left").assertDoesNotExist()
        compose.onNodeWithTag("mala_mode_drag").performClick()
        assertEquals(true, mala)
        compose.onNodeWithTag("mala_side_right").assertExists()
        compose.onNodeWithText("Drag down the strip on the right to count one name, then lift.").assertExists()
        compose.onNodeWithTag("mala_side_left").performClick()
        assertEquals(MalaSide.LEFT, side)
        compose.onNodeWithTag("kind_PRADARSHANA").performClick()
        compose.onNodeWithTag("mala_mode_drag").assertDoesNotExist()
        compose.onNodeWithTag("count_mode_manual").assertExists()
    }
}
