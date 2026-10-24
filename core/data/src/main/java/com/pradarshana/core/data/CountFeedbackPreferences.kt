package com.pradarshana.core.data

import android.content.Context
import com.pradarshana.core.model.AutoCountDistances
import com.pradarshana.core.model.HistoryFilter
import com.pradarshana.core.model.MalaSide
import com.pradarshana.core.model.SavedTemple
import com.pradarshana.core.model.SavedTemples
import com.pradarshana.core.model.SessionDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** Counting-screen options. Vibration and darkening stay on until turned off. Automatic counting and mala drag stay off. */
class CountFeedbackPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _vibrateOnCount = MutableStateFlow(prefs.getBoolean(KEY_VIBRATE_ON_COUNT, true))
    val vibrateOnCount: StateFlow<Boolean> = _vibrateOnCount.asStateFlow()
    private val _blackoutAfterTwoTaps = MutableStateFlow(prefs.getBoolean(KEY_BLACKOUT_AFTER_TWO_TAPS, true))
    val blackoutAfterTwoTaps: StateFlow<Boolean> = _blackoutAfterTwoTaps.asStateFlow()
    private val _autoCount = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COUNT, false))
    val autoCount: StateFlow<Boolean> = _autoCount.asStateFlow()
    private val _awayMeters = MutableStateFlow(prefs.getInt(KEY_AWAY_METERS, AutoCountDistances.DEFAULT_AWAY_METERS))
    val awayMeters: StateFlow<Int> = _awayMeters.asStateFlow()
    private val _returnMeters = MutableStateFlow(prefs.getInt(KEY_RETURN_METERS, AutoCountDistances.DEFAULT_RETURN_METERS))
    val returnMeters: StateFlow<Int> = _returnMeters.asStateFlow()
    private val _malaDrag = MutableStateFlow(prefs.getBoolean(KEY_MALA_DRAG, false))
    val malaDrag: StateFlow<Boolean> = _malaDrag.asStateFlow()
    private val _malaSide = MutableStateFlow(MalaSide.fromStored(prefs.getString(KEY_MALA_SIDE, null)))
    val malaSide: StateFlow<MalaSide> = _malaSide.asStateFlow()
    private val _undoLast = MutableStateFlow(prefs.getBoolean(KEY_UNDO_LAST, false))
    val undoLast: StateFlow<Boolean> = _undoLast.asStateFlow()
    private val _volumeCount = MutableStateFlow(prefs.getBoolean(KEY_VOLUME_COUNT, false))
    val volumeCount: StateFlow<Boolean> = _volumeCount.asStateFlow()
    private val _guruBell = MutableStateFlow(prefs.getBoolean(KEY_GURU_BELL, false))
    val guruBell: StateFlow<Boolean> = _guruBell.asStateFlow()
    private val _showTotals = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TOTALS, false))
    val showTotals: StateFlow<Boolean> = _showTotals.asStateFlow()
    private val _crownCount = MutableStateFlow(prefs.getBoolean(KEY_CROWN_COUNT, false))
    val crownCount: StateFlow<Boolean> = _crownCount.asStateFlow()
    private val _historyFilter = MutableStateFlow(HistoryFilter.fromStored(prefs.getString(KEY_HISTORY_FILTER, null)))
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()
    private val _historyQuery = MutableStateFlow(prefs.getString(KEY_HISTORY_QUERY, "").orEmpty())
    val historyQuery: StateFlow<String> = _historyQuery.asStateFlow()
    private val _temples = MutableStateFlow(SavedTemples.decode(prefs.getString(KEY_TEMPLES, null)))
    val temples: StateFlow<List<SavedTemple>> = _temples.asStateFlow()

    fun setVibrateOnCount(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE_ON_COUNT, enabled).commit()
        _vibrateOnCount.value = enabled
    }

    fun setBlackoutAfterTwoTaps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLACKOUT_AFTER_TWO_TAPS, enabled).commit()
        _blackoutAfterTwoTaps.value = enabled
    }

    fun setAutoCount(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COUNT, enabled).commit()
        _autoCount.value = enabled
    }

    /** Saves a valid pair. A return distance that is not shorter than the walk-away distance is rejected. */
    fun setDistances(awayMeters: Int, returnMeters: Int) {
        if (!AutoCountDistances.isValid(awayMeters, returnMeters)) return
        prefs.edit().putInt(KEY_AWAY_METERS, awayMeters).putInt(KEY_RETURN_METERS, returnMeters).commit()
        _awayMeters.value = awayMeters
        _returnMeters.value = returnMeters
    }

    fun setMalaDrag(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MALA_DRAG, enabled).commit()
        _malaDrag.value = enabled
    }

    fun setMalaSide(side: MalaSide) {
        prefs.edit().putString(KEY_MALA_SIDE, side.stored).commit()
        _malaSide.value = side
    }

    fun setUndoLast(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_UNDO_LAST, enabled).commit()
        _undoLast.value = enabled
    }

    fun setVolumeCount(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOLUME_COUNT, enabled).commit()
        _volumeCount.value = enabled
    }

    fun setGuruBell(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GURU_BELL, enabled).commit()
        _guruBell.value = enabled
    }

    fun setShowTotals(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TOTALS, enabled).commit()
        _showTotals.value = enabled
    }

    fun setCrownCount(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CROWN_COUNT, enabled).commit()
        _crownCount.value = enabled
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        prefs.edit().putString(KEY_HISTORY_FILTER, filter.stored).commit()
        _historyFilter.value = filter
    }

    fun setHistoryQuery(query: String) {
        val clean = query.take(SessionDetails.MAX_TITLE_LENGTH)
        prefs.edit().putString(KEY_HISTORY_QUERY, clean).commit()
        _historyQuery.value = clean
    }

    fun matchingTemple(latitude: Double?, longitude: Double?): SavedTemple? {
        if (latitude == null || longitude == null) return null
        return _temples.value.firstOrNull { it.latitude == latitude && it.longitude == longitude }
    }

    /** Saves a named place. A blank name, a bad place, or a full list is left unchanged. */
    fun saveTemple(name: String, latitude: Double, longitude: Double): Boolean {
        val clean = SessionDetails.normalizeTitle(name)
        val (lat, lng) = SessionDetails.normalizeLocation(latitude, longitude)
        if (clean.isEmpty() || lat == null || lng == null) return false
        val current = _temples.value
        if (current.size >= SavedTemples.MAX) return false
        val next = current + SavedTemple(UUID.randomUUID().toString(), clean, lat, lng)
        return storeTemples(next)
    }

    fun deleteTemple(id: String) {
        storeTemples(_temples.value.filterNot { it.id == id })
    }

    private fun storeTemples(temples: List<SavedTemple>): Boolean {
        prefs.edit().putString(KEY_TEMPLES, SavedTemples.encode(temples)).commit()
        _temples.value = temples
        return true
    }

    companion object {
        private const val PREFS = "pradarshana_settings"
        const val KEY_VIBRATE_ON_COUNT = "vibrate_on_count"
        const val KEY_BLACKOUT_AFTER_TWO_TAPS = "blackout_after_two_taps"
        const val KEY_AUTO_COUNT = "auto_count"
        const val KEY_AWAY_METERS = "auto_count_away_meters"
        const val KEY_RETURN_METERS = "auto_count_return_meters"
        const val KEY_MALA_DRAG = "mala_drag"
        const val KEY_MALA_SIDE = "mala_drag_side"
        const val KEY_UNDO_LAST = "undo_last_count"
        const val KEY_VOLUME_COUNT = "volume_count"
        const val KEY_GURU_BELL = "guru_bell"
        const val KEY_SHOW_TOTALS = "show_totals"
        const val KEY_CROWN_COUNT = "crown_count"
        const val KEY_HISTORY_FILTER = "history_filter"
        const val KEY_HISTORY_QUERY = "history_title_query"
        const val KEY_TEMPLES = "saved_temples"
    }
}
