package com.pradarshana.core.model

/** Which finished sessions the history screen shows. All is used until another choice is saved. */
enum class HistoryFilter(val stored: String) {
    ALL("all"),
    WALKS("walks"),
    CHANTING("chanting"),
    ;

    fun matches(kind: SessionKind): Boolean = when (this) {
        ALL -> true
        WALKS -> kind == SessionKind.PRADARSHANA
        CHANTING -> kind == SessionKind.CHANTING
    }

    companion object {
        fun fromStored(value: String?): HistoryFilter = entries.firstOrNull { it.stored == value } ?: ALL
    }
}
