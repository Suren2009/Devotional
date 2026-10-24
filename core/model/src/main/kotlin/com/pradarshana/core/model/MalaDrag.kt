package com.pradarshana.core.model

/** Which edge of the chanting screen holds the mala strip. */
enum class MalaSide(val stored: String) {
    LEFT("left"),
    RIGHT("right"),
    ;

    companion object {
        fun fromStored(value: String?): MalaSide = entries.firstOrNull { it.stored == value } ?: RIGHT
    }
}

/**
 * One downward drag, then a lift, counts one name.
 * A long slide still counts once. A short slip, or a drag that never travels down, does not.
 */
object MalaDrag {
    const val MIN_DOWNWARD_DP = 64f

    fun counts(downwardPx: Float, thresholdPx: Float): Boolean =
        thresholdPx > 0f && downwardPx >= thresholdPx
}
