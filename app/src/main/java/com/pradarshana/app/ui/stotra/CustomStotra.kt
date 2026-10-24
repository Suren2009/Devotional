package com.pradarshana.app.ui.stotra

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.random.Random

/** A stotra the devotee typed and saved on this phone. */
data class CustomStotra(val id: String, val title: String)

sealed class CustomStotraAdd {
    data class Saved(val id: String) : CustomStotraAdd()
    data object Blank : CustomStotraAdd()
    data object Full : CustomStotraAdd()
}

/**
 * Included stotras stay in the app. Hiding one removes it from the list until the included
 * stotras are shown again. A stotra typed here is stored on this phone and can be deleted.
 */
class CustomStotraStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val directory = File(context.applicationContext.filesDir, DIRECTORY).apply { mkdirs() }
    private val _entries = MutableStateFlow(readEntries())
    val entries: StateFlow<List<CustomStotra>> = _entries.asStateFlow()
    private val _hidden = MutableStateFlow(readHidden())
    val hiddenBuiltIns: StateFlow<Set<String>> = _hidden.asStateFlow()

    fun entry(id: String): CustomStotra? = _entries.value.firstOrNull { it.id == id }

    fun body(id: String): String? {
        val file = File(directory, "$id.txt")
        if (!file.isFile || !isSafeId(id)) return null
        return file.readText(Charsets.UTF_8)
    }

    fun add(title: String, body: String): CustomStotraAdd {
        val cleanTitle = normalizeTitle(title)
        val cleanBody = body.trim().take(MAX_BODY)
        if (cleanTitle.isEmpty() || cleanBody.isEmpty()) return CustomStotraAdd.Blank
        if (_entries.value.size >= MAX_COUNT) return CustomStotraAdd.Full
        val id = newId()
        File(directory, "$id.txt").writeText(cleanBody, Charsets.UTF_8)
        val next = _entries.value + CustomStotra(id, cleanTitle)
        writeEntries(next)
        return CustomStotraAdd.Saved(id)
    }

    fun delete(id: String) {
        if (StotraId.fromFile(id) != null) {
            val next = _hidden.value + id
            prefs.edit().putString(KEY_HIDDEN, next.joinToString("\n")).commit()
            _hidden.value = next
            return
        }
        if (!isSafeId(id)) return
        File(directory, "$id.txt").delete()
        writeEntries(_entries.value.filter { it.id != id })
    }

    fun restoreBuiltIns() {
        prefs.edit().remove(KEY_HIDDEN).commit()
        _hidden.value = emptySet()
    }

    private fun writeEntries(entries: List<CustomStotra>) {
        prefs.edit().putString(KEY_ENTRIES, encode(entries)).commit()
        _entries.value = entries
    }

    private fun readEntries(): List<CustomStotra> = decode(prefs.getString(KEY_ENTRIES, null))

    private fun readHidden(): Set<String> {
        val raw = prefs.getString(KEY_HIDDEN, null).orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.lineSequence().map { it.trim() }.filter { StotraId.fromFile(it) != null }.toSet()
    }

    private fun newId(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        repeat(6) {
            val suffix = buildString(6) { repeat(6) { append(alphabet[Random.nextInt(alphabet.length)]) } }
            val id = "c${System.currentTimeMillis().toString(36)}$suffix"
            if (isSafeId(id) && _entries.value.none { it.id == id } && StotraId.fromFile(id) == null) return id
        }
        return "c${System.currentTimeMillis().toString(36)}"
    }

    companion object {
        const val MAX_COUNT = 40
        const val MAX_TITLE = 80
        const val MAX_BODY = 100_000
        private const val PREFS = "pradarshana_settings"
        private const val KEY_ENTRIES = "custom_stotras"
        private const val KEY_HIDDEN = "hidden_stotras"
        private const val DIRECTORY = "custom-stotras"

        fun normalizeTitle(value: String): String =
            value.trim().replace(Regex("\\s+"), " ").take(MAX_TITLE)

        fun encode(entries: List<CustomStotra>): String =
            entries.joinToString("\n") { "${it.id}\t${it.title}" }

        fun decode(raw: String?): List<CustomStotra> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.lineSequence().mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 2 || !isSafeId(parts[0])) return@mapNotNull null
                val title = normalizeTitle(parts[1])
                if (title.isEmpty() || StotraId.fromFile(parts[0]) != null) return@mapNotNull null
                CustomStotra(parts[0], title)
            }.take(MAX_COUNT).toList()
        }

        fun isSafeId(id: String): Boolean = id.length in 2..40 && id.all { it in 'a'..'z' || it in '0'..'9' }
    }
}
