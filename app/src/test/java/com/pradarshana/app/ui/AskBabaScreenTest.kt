package com.pradarshana.app.ui

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pradarshana.app.ui.ask.AskBabaScreen
import com.pradarshana.app.ui.theme.PradarshanaTheme
import com.pradarshana.core.model.AskBaba
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AskBabaScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aNumberShowsTheEnglishAndTeluguAnswer() {
        compose.setContent { PradarshanaTheme { AskBabaScreen(onBack = {}) } }
        compose.onNodeWithTag("banner_faith").assertExists()
        compose.onNodeWithTag("banner_preserve").assertExists()
        compose.onNodeWithTag("ask_number").performTextInput("720")
        compose.onNodeWithTag("ask_button").performClick()
        val answer = AskBaba.answer(720)!!
        compose.onNodeWithTag("ask_english").assertTextContains(answer.english, substring = true)
        compose.onNodeWithTag("ask_telugu").assertTextContains(answer.telugu, substring = true)
    }

    @Test
    fun aNumberOutsideTheRangeIsRefused() {
        compose.setContent { PradarshanaTheme { AskBabaScreen(onBack = {}) } }
        compose.onNodeWithTag("ask_number").performTextInput("0")
        compose.onNodeWithTag("ask_button").performClick()
        compose.onNodeWithText("Enter a number from 1 to 720.").assertExists()
        compose.onNodeWithTag("ask_answer").assertDoesNotExist()
    }
}
