package com.pradarshana.app.ui.stotra

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StotraLanguage { TELUGU, ENGLISH }

enum class StotraTextSize(val stored: String, val scale: Float) {
    SMALL("small", 0.85f),
    MEDIUM("medium", 1f),
    LARGE("large", 1.25f),
    ;

    companion object {
        fun fromStored(value: String?): StotraTextSize = entries.firstOrNull { it.stored == value } ?: MEDIUM
    }
}

/** Character indexes of [query] inside [body]. Blank queries match nothing. */
fun stotraMatchStarts(body: String, query: String): List<Int> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    val haystack = body.lowercase()
    val look = needle.lowercase()
    val hits = mutableListOf<Int>()
    var from = 0
    while (from <= haystack.length - look.length) {
        val found = haystack.indexOf(look, from)
        if (found < 0) break
        hits += found
        from = found + look.length
    }
    return hits
}

/** The language chosen for stotras. Telugu is used until English is chosen. */
class StotraLanguageStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _language = MutableStateFlow(read())
    val language: StateFlow<StotraLanguage> = _language.asStateFlow()
    private val _textSize = MutableStateFlow(StotraTextSize.fromStored(prefs.getString(KEY_TEXT_SIZE, null)))
    val textSize: StateFlow<StotraTextSize> = _textSize.asStateFlow()

    fun set(language: StotraLanguage) {
        prefs.edit().putString(KEY, if (language == StotraLanguage.ENGLISH) ENGLISH else TELUGU).commit()
        _language.value = language
    }

    fun setTextSize(size: StotraTextSize) {
        prefs.edit().putString(KEY_TEXT_SIZE, size.stored).commit()
        _textSize.value = size
    }

    fun bookmark(stotra: StotraId, language: StotraLanguage): Int = bookmark(stotra.file, language)

    fun bookmark(file: String, language: StotraLanguage): Int =
        prefs.getInt(bookmarkKey(file, language), NO_BOOKMARK)

    fun saveBookmark(stotra: StotraId, language: StotraLanguage, offset: Int) {
        saveBookmark(stotra.file, language, offset)
    }

    fun saveBookmark(file: String, language: StotraLanguage, offset: Int) {
        prefs.edit().putInt(bookmarkKey(file, language), offset.coerceAtLeast(0)).commit()
    }

    fun scrollOffset(stotra: StotraId, language: StotraLanguage): Int = scrollOffset(stotra.file, language)

    fun scrollOffset(file: String, language: StotraLanguage): Int =
        prefs.getInt(scrollKey(file, language), 0).coerceAtLeast(0)

    fun saveScroll(stotra: StotraId, language: StotraLanguage, offset: Int) {
        saveScroll(stotra.file, language, offset)
    }

    fun saveScroll(file: String, language: StotraLanguage, offset: Int) {
        prefs.edit().putInt(scrollKey(file, language), offset.coerceAtLeast(0)).apply()
    }

    fun listScroll(): Int = prefs.getInt(LIST_SCROLL, 0).coerceAtLeast(0)

    fun saveListScroll(offset: Int) {
        prefs.edit().putInt(LIST_SCROLL, offset.coerceAtLeast(0)).apply()
    }

    private fun read(): StotraLanguage =
        if (prefs.getString(KEY, TELUGU) == ENGLISH) StotraLanguage.ENGLISH else StotraLanguage.TELUGU

    private fun scrollKey(file: String, language: StotraLanguage): String {
        val suffix = if (language == StotraLanguage.ENGLISH) ENGLISH else TELUGU
        return "stotra_scroll_${file}_$suffix"
    }

    private fun bookmarkKey(file: String, language: StotraLanguage): String {
        val suffix = if (language == StotraLanguage.ENGLISH) ENGLISH else TELUGU
        return "stotra_bookmark_${file}_$suffix"
    }

    companion object {
        private const val PREFS = "pradarshana_settings"
        const val KEY = "stotra_language"
        const val KEY_TEXT_SIZE = "stotra_text_size"
        const val NO_BOOKMARK = -1
        private const val LIST_SCROLL = "stotras_list_scroll"
        private const val TELUGU = "te"
        private const val ENGLISH = "en"
    }
}

enum class StotraId(
    val file: String,
    val titleTe: String,
    val titleEn: String,
) {
    VISHNU("vishnu", "విష్ణు సహస్రనామం", "Vishnu Sahasranama"),
    LALITHA("lalitha", "లలితా సహస్రనామం", "Lalitha Sahasranama"),
    GOVINDA("govinda", "గోవింద నామాలు", "Govinda Namalu"),
    ANJANEYA("anjaneya", "ఆంజనేయ దండకం", "Anjaneya Dandakam"),
    HANUMAN("hanuman", "హనుమాన్ చాలీసా", "Hanuman Chalisa"),
    LAKSHMI("lakshmi", "లక్ష్మీ అష్టోత్తరం", "Lakshmi Ashtottaram"),
    MAHALAKSHMI("mahalakshmi", "మహా లక్ష్మీ అష్టకం", "Maha Lakshmi Ashtakam"),
    ;

    fun title(language: StotraLanguage): String = if (language == StotraLanguage.TELUGU) titleTe else titleEn

    companion object {
        fun fromFile(file: String): StotraId? = entries.firstOrNull { it.file == file }
    }
}

fun loadStotra(context: Context, stotra: StotraId, language: StotraLanguage): String {
    val suffix = if (language == StotraLanguage.TELUGU) "te" else "en"
    return context.assets.open("stotras/${stotra.file}_$suffix.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
}
