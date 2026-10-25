package com.pradarshana.app.ui.stotra

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal const val STOTRA_IMAGE_MAX_EDGE = 2048
internal const val STOTRA_IMAGE_MAX_PIXELS = 2048 * 4096
internal const val STOTRA_IMAGE_MAX_SOURCE_BYTES = 32L * 1024 * 1024

/** Longest-edge sample size so a camera photo can be decoded without keeping every pixel. */
internal fun stotraImageSampleSize(width: Int, height: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (sample < 64) {
        val scaledWidth = width / sample
        val scaledHeight = height / sample
        if (scaledWidth <= STOTRA_IMAGE_MAX_EDGE &&
            scaledHeight <= STOTRA_IMAGE_MAX_EDGE &&
            scaledWidth.toLong() * scaledHeight <= STOTRA_IMAGE_MAX_PIXELS
        ) {
            break
        }
        sample *= 2
    }
    return sample
}

/** Turns a camera photo upright. The original bitmap is recycled when a new one is made. */
internal fun applyStotraOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.preScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.preScale(-1f, 1f)
        }
        else -> return bitmap
    }
    val turned = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (turned != bitmap) bitmap.recycle()
    return turned
}

/** JPEG, PNG, WebP, or HEIF. Other files are not stored as a stotra photo. */
internal fun isStotraImageFile(file: File): Boolean {
    if (!file.isFile || file.length() < 12L) return false
    val header = ByteArray(12)
    val read = file.inputStream().use { it.read(header) }
    if (read < 12) return false
    val jpeg = header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()
    val png = header[0] == 0x89.toByte() && header[1] == 'P'.code.toByte() &&
        header[2] == 'N'.code.toByte() && header[3] == 'G'.code.toByte()
    val webp = header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
        header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
        header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
        header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()
    val ftyp = header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
        header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()
    val brand = if (ftyp) String(header, 8, 4, Charsets.ISO_8859_1) else ""
    val heif = brand == "heic" || brand == "heif" || brand == "mif1" || brand == "msf1"
    return jpeg || png || webp || heif
}

/** Copies [source] into a smaller JPEG at [dest]. An unreadable or oversized photo is refused. */
internal fun writeStotraJpeg(source: File, dest: File): Boolean {
    if (!source.isFile || source.length() <= 0L || source.length() > STOTRA_IMAGE_MAX_SOURCE_BYTES) return false
    if (!isStotraImageFile(source)) return false
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
    val decoded = BitmapFactory.Options().apply {
        inSampleSize = stotraImageSampleSize(bounds.outWidth, bounds.outHeight)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    var bitmap = BitmapFactory.decodeFile(source.absolutePath, decoded) ?: return false
    val orientation = runCatching {
        ExifInterface(source.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    bitmap = applyStotraOrientation(bitmap, orientation)
    bitmap = fitStotraBitmap(bitmap)
    bitmap = flattenStotraBitmap(bitmap)
    val saved = try {
        dest.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    } finally {
        bitmap.recycle()
    }
    return saved && dest.isFile && dest.length() > 0L
}

internal fun readStotraImage(file: File): Bitmap? {
    if (!file.isFile || file.length() <= 0L) return null
    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    return BitmapFactory.decodeFile(file.absolutePath, options)
}

private fun fitStotraBitmap(bitmap: Bitmap): Bitmap {
    val edge = max(bitmap.width, bitmap.height).toFloat()
    var scale = if (edge > STOTRA_IMAGE_MAX_EDGE) STOTRA_IMAGE_MAX_EDGE / edge else 1f
    val pixels = bitmap.width.toDouble() * bitmap.height * scale * scale
    if (pixels > STOTRA_IMAGE_MAX_PIXELS) {
        val pixelScale = sqrt(STOTRA_IMAGE_MAX_PIXELS / (bitmap.width.toDouble() * bitmap.height)).toFloat()
        if (pixelScale < scale) scale = pixelScale
    }
    if (scale >= 0.999f) return bitmap
    val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
    if (scaled != bitmap) bitmap.recycle()
    return scaled
}

private fun flattenStotraBitmap(bitmap: Bitmap): Bitmap {
    if (!bitmap.hasAlpha()) return bitmap
    val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(flat).apply {
        drawColor(android.graphics.Color.WHITE)
        drawBitmap(bitmap, 0f, 0f, null)
    }
    bitmap.recycle()
    return flat
}
