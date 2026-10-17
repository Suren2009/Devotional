package com.pradarshana.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pradarshana.app.ui.home.HomeScreen
import com.pradarshana.app.ui.stotra.StotraId
import com.pradarshana.app.ui.stotra.StotraLanguage
import com.pradarshana.app.ui.stotra.StotraLanguageStore
import com.pradarshana.app.ui.stotra.StotraListScreen
import com.pradarshana.app.ui.stotra.StotraReaderScreen
import com.pradarshana.app.ui.stotra.loadStotra
import com.pradarshana.app.ui.theme.PradarshanaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StotraScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun languageStartsInTeluguAndIsRemembered() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("pradarshana_settings", Context.MODE_PRIVATE)
            .edit()
            .remove(StotraLanguageStore.KEY)
            .commit()
        val store = StotraLanguageStore(context)
        assertEquals(StotraLanguage.TELUGU, store.language.value)
        store.set(StotraLanguage.ENGLISH)
        assertEquals(StotraLanguage.ENGLISH, StotraLanguageStore(context).language.value)
        store.set(StotraLanguage.TELUGU)
        store.saveScroll(StotraId.MAHALAKSHMI, StotraLanguage.TELUGU, 240)
        store.saveScroll(StotraId.MAHALAKSHMI, StotraLanguage.ENGLISH, 80)
        val again = StotraLanguageStore(context)
        assertEquals(240, again.scrollOffset(StotraId.MAHALAKSHMI, StotraLanguage.TELUGU))
        assertEquals(80, again.scrollOffset(StotraId.MAHALAKSHMI, StotraLanguage.ENGLISH))
        assertEquals(0, again.scrollOffset(StotraId.VISHNU, StotraLanguage.TELUGU))
        assertTrue(loadStotra(context, StotraId.MAHALAKSHMI, StotraLanguage.TELUGU).contains("నమస్తేస్తు మహామాయే"))
        assertTrue(loadStotra(context, StotraId.MAHALAKSHMI, StotraLanguage.ENGLISH).contains("namastestu mahaamaaye"))
    }

    @Test
    fun homeOpensTheListAndAStotraKeepsTheChosenLanguage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("pradarshana_settings", Context.MODE_PRIVATE)
            .edit()
            .remove(StotraLanguageStore.KEY)
            .commit()
        var showList by mutableStateOf(false)
        var opened by mutableStateOf<StotraId?>(null)
        var language by mutableStateOf(StotraLanguage.TELUGU)
        compose.setContent {
            PradarshanaTheme {
                when {
                    opened != null -> StotraReaderScreen(
                        stotra = opened!!,
                        language = language,
                        body = loadStotra(context, opened!!, language),
                        onLanguage = { language = it },
                        onBack = {},
                    )
                    showList -> StotraListScreen(onBack = {}, onOpen = { opened = it })
                    else -> HomeScreen(null, {}, {}, {}, onStotras = { showList = true })
                }
            }
        }
        compose.onNodeWithTag("stotras_button").performClick()
        compose.onNodeWithText("విష్ణు సహస్రనామం").assertExists()
        compose.onNodeWithText("లలితా సహస్రనామం").assertExists()
        compose.onNodeWithText("గోవింద నామాలు").assertExists()
        compose.onNodeWithText("ఆంజనేయ దండకం").assertExists()
        compose.onNodeWithText("హనుమాన్ చాలీసా").assertExists()
        compose.onNodeWithText("లక్ష్మీ అష్టోత్తరం").assertExists()
        compose.onNodeWithText("మహా లక్ష్మీ అష్టకం").assertExists()
        compose.onNodeWithTag("stotra_vishnu").performClick()
        assertEquals(StotraId.VISHNU, opened)
        compose.onNodeWithTag("stotra_lang_te").assertExists()
        compose.onNodeWithText("విశ్వం విష్ణుర్వషట్కారో", substring = true).assertExists()
        compose.onNodeWithTag("stotra_lang_en").performClick()
        assertEquals(StotraLanguage.ENGLISH, language)
        compose.onNodeWithText("vishvam vishnurvashatkaaro", substring = true).assertExists()
    }

    @Test
    fun readerReturnsToTheStartOfThePage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var saved by mutableStateOf(-1)
        compose.setContent {
            PradarshanaTheme {
                StotraReaderScreen(
                    stotra = StotraId.VISHNU,
                    language = StotraLanguage.TELUGU,
                    body = loadStotra(context, StotraId.VISHNU, StotraLanguage.TELUGU),
                    onLanguage = {},
                    onBack = {},
                    initialScroll = 2_000,
                    onScroll = { _, _, offset -> saved = offset },
                )
            }
        }
        compose.onNodeWithTag("stotra_to_start").assertExists()
        compose.onNodeWithTag("stotra_body").assertExists()
        compose.onNodeWithTag("stotra_to_start").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            saved == 0 && compose.onAllNodesWithTag("stotra_to_start").fetchSemanticsNodes().isEmpty()
        }
    }
}
