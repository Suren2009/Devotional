package com.pradarshana.app.ui.stotra

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomStotraStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun clear() {
        context.getSharedPreferences("pradarshana_settings", Context.MODE_PRIVATE).edit().clear().commit()
        File(context.filesDir, "custom-stotras").deleteRecursively()
    }

    @Test
    fun aTypedStotraIsSavedReadAndDeletedAndAnIncludedStotraCanComeBack() {
        val store = CustomStotraStore(context)
        assertEquals(CustomStotraAdd.Blank, store.add("  ", "Om"))
        assertEquals(CustomStotraAdd.Blank, store.add("Name", "   "))
        val saved = store.add("  Evening   prayer  ", "Om namah") as CustomStotraAdd.Saved
        assertEquals("Evening prayer", store.entry(saved.id)!!.title)
        assertEquals("Om namah", store.body(saved.id))
        assertEquals(listOf(saved.id), CustomStotraStore.decode(CustomStotraStore.encode(store.entries.value)).map { it.id })

        store.delete(StotraId.VISHNU.file)
        assertTrue(store.hiddenBuiltIns.value.contains(StotraId.VISHNU.file))
        store.restoreBuiltIns()
        assertTrue(store.hiddenBuiltIns.value.isEmpty())

        store.delete(saved.id)
        assertTrue(store.entries.value.isEmpty())
        assertNull(store.body(saved.id))
    }
}
