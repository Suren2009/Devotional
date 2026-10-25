package com.pradarshana.app.ui.stotra

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
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
import java.io.RandomAccessFile

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
        assertEquals(StotraKind.TEXT, store.entry(saved.id)!!.kind)
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

    @Test
    fun aPhotoIsStoredAndAnOldTextLineStaysText() {
        val store = CustomStotraStore(context)
        assertEquals(CustomStotraAdd.Blank, store.addImage("  ", jpegFile()))
        val notes = File(context.cacheDir, "note.jpg")
        notes.writeText("not a photo")
        assertEquals(CustomStotraAdd.Unreadable, store.addImage("Page", notes))
        val huge = File(context.cacheDir, "huge.jpg")
        RandomAccessFile(huge, "rw").use { it.setLength(STOTRA_IMAGE_MAX_SOURCE_BYTES + 1) }
        assertEquals(CustomStotraAdd.Unreadable, store.addImage("Page", huge))
        assertTrue(store.entries.value.isEmpty())

        val saved = store.addImage("  Lamp  ", jpegFile()) as CustomStotraAdd.Saved
        val entry = store.entry(saved.id)!!
        assertEquals("Lamp", entry.title)
        assertEquals(StotraKind.IMAGE, entry.kind)
        assertNull(store.body(saved.id))
        val image = store.loadImage(saved.id)
        assertTrue(image != null && image.width > 0 && image.height > 0)
        image!!.recycle()
        assertEquals(
            listOf(saved.id to StotraKind.IMAGE),
            CustomStotraStore.decode(CustomStotraStore.encode(store.entries.value)).map { it.id to it.kind },
        )
        assertEquals(StotraKind.TEXT, CustomStotraStore.decode("cabc\tEvening prayer").single().kind)
        assertEquals(StotraKind.TEXT, CustomStotraStore.decode("cabc\tEvening prayer\ttext").single().kind)
        assertEquals(StotraKind.IMAGE, CustomStotraStore.decode("cabc\tPage\timage").single().kind)
        assertTrue(CustomStotraStore.decode("cabc\tPage\tnope").isEmpty())

        store.delete(saved.id)
        assertTrue(store.entries.value.isEmpty())
        assertNull(store.loadImage(saved.id))

        repeat(CustomStotraStore.MAX_COUNT) { index ->
            assertTrue(store.add("Name $index", "Om") is CustomStotraAdd.Saved)
        }
        assertEquals(CustomStotraAdd.Full, store.addImage("Extra", jpegFile()))
    }

    @Test
    fun aLargePhotoIsSampledAndATurnedPhotoSwapsSides() {
        assertEquals(1, stotraImageSampleSize(100, 80))
        assertTrue(stotraImageSampleSize(4000, 3000) >= 2)
        val bitmap = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888)
        val turned = applyStotraOrientation(bitmap, ExifInterface.ORIENTATION_ROTATE_90)
        assertEquals(2, turned.width)
        assertEquals(4, turned.height)
        turned.recycle()
    }

    private fun jpegFile(): File {
        val bitmap = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val file = File(context.cacheDir, "sample-stotra.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }
}
