package com.pradarshana.app.ui.stotra

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pradarshana.app.ui.theme.PradarshanaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StotraPhotoScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aPhotoHasNoSearchAndCanBeMadeLarger() {
        var removed by mutableStateOf(false)
        compose.setContent {
            PradarshanaTheme {
                StotraPhotoScreen(
                    title = "Evening prayer",
                    image = Bitmap.createBitmap(8, 16, Bitmap.Config.ARGB_8888).asImageBitmap(),
                    onBack = {},
                    onDelete = { removed = true },
                )
            }
        }
        compose.onNodeWithTag("stotra_image").assertExists()
        compose.onNodeWithText("Evening prayer").assertExists()
        compose.onNodeWithTag("stotra_tools").performClick()
        compose.onNodeWithTag("stotra_search_toggle").assertDoesNotExist()
        compose.onNodeWithTag("stotra_zoom_in").performClick()
        compose.onNodeWithTag("stotra_image").assertExists()
        compose.onNodeWithTag("stotra_tools").performClick()
        compose.onNodeWithTag("stotra_delete").performClick()
        compose.onNodeWithText("This deletes the photo you saved.").assertExists()
        compose.onNodeWithTag("confirm_delete_stotra").performClick()
        assertEquals(true, removed)
    }
}
