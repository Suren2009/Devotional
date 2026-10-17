package com.pradarshana.app.ui.stotra

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StotraLanguage { TELUGU, ENGLISH }

/** The language chosen for stotras. Telugu is used until English is chosen. */
class StotraLanguageStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _language = MutableStateFlow(read())
    val language: StateFlow<StotraLanguage> = _language.asStateFlow()

    fun set(language: StotraLanguage) {
        prefs.edit().putString(KEY, if (language == StotraLanguage.ENGLISH) ENGLISH else TELUGU).commit()
        _language.value = language
    }

    private fun read(): StotraLanguage =
        if (prefs.getString(KEY, TELUGU) == ENGLISH) StotraLanguage.ENGLISH else StotraLanguage.TELUGU

    companion object {
        private const val PREFS = "pradarshana_settings"
        const val KEY = "stotra_language"
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
