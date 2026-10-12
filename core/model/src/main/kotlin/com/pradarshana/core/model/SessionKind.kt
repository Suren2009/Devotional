package com.pradarshana.core.model

/** What the devotee is counting: a walk around the temple, or a chant of names. */
enum class SessionKind(val label: String, val unit: String) {
    PRADARSHANA("Pradarshana", "rounds"),
    CHANTING("Chanting", "names"),
    ;

    companion object {
        fun fromStored(value: String?): SessionKind =
            entries.firstOrNull { it.name == value } ?: PRADARSHANA
    }
}
