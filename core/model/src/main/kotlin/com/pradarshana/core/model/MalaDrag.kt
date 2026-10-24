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

    /**
     * Where the current rudraksha rests on the cord.
     * 0 is the top, 1 is the bottom, and 0.5 is Start/Goal.
     * Before any name, the bead sits on that middle mark.
     * The first half of the target steps down below it, ending at the bottom.
     * The second half starts at the top and steps down to just above the mark.
     */
    fun beadFraction(completed: Int, target: Int): Float {
        if (target <= 0) return 0.5f
        val count = completed.coerceIn(0, target)
        if (count == 0) return 0.5f
        val lower = target / 2
        val upper = target - lower
        if (lower > 0 && count <= lower) {
            return 0.5f + (count.toFloat() / lower) * 0.5f
        }
        val slots = upper.coerceAtLeast(1)
        val step = (count - lower).coerceIn(1, slots)
        return ((step - 1).toFloat() / slots) * 0.5f
    }

    /** A cord bead already passed on the way from Start/Goal toward the current name. */
    fun beadVisited(place: Float, completed: Int, target: Int): Boolean {
        if (target <= 0 || completed <= 0) return false
        val here = beadFraction(completed, target)
        val lower = target / 2
        val count = completed.coerceIn(0, target)
        return if (lower > 0 && count <= lower) {
            place >= 0.5f && place <= here + 0.0001f
        } else {
            place <= here + 0.0001f || place >= 0.5f
        }
    }
}
