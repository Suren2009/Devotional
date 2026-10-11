package com.pradarshana.app

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end phone flow on a real device or emulator: start, count, complete, view history. */
@RunWith(AndroidJUnit4::class)
class PradarshanaFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearState() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PradarshanaApp
        runBlocking {
            app.container.repository.getOngoingSession()?.let { app.container.repository.endSession(it.id) }
            app.container.syncManager.clearHistory()
        }
    }

    @Test
    fun startCountCompleteAndSeeHistory() {
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Start Pradarshana")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("start_button").performClick()
        compose.onNodeWithTag("target_input").performTextInput("3")
        compose.onNodeWithTag("confirm_start_button").performClick()

        compose.waitUntil(5_000) { compose.onAllNodes(hasText("of 3"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        repeat(3) { i ->
            compose.onNodeWithTag("increment_button").performClick()
            if (i < 2) {
                compose.waitUntil(5_000) {
                    compose.onAllNodes(hasText("${i + 1}"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
                }
            }
        }

        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Pradarshana Completed!")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("View History").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Completed")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(" / 3 rounds", useUnmergedTree = true).assertExists()
    }
}
