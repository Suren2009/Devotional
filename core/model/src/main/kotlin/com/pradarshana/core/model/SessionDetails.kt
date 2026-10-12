package com.pradarshana.core.model

/** Optional name and place recorded when a session starts. */
object SessionDetails {
    const val MAX_TITLE_LENGTH = 60

    /** Shown in history when the devotee did not type a title. */
    const val UNTITLED = "Untitled"

    fun normalizeTitle(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ").take(MAX_TITLE_LENGTH)

    fun displayTitle(title: String): String = title.ifEmpty { UNTITLED }

    /**
     * A fix is kept only when both coordinates are present and inside the valid range.
     * Returns nulls otherwise, including when only one of the two was supplied.
     */
    fun normalizeLocation(latitude: Double?, longitude: Double?): Pair<Double?, Double?> {
        if (latitude == null || longitude == null) return null to null
        if (!latitude.isFinite() || !longitude.isFinite()) return null to null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null to null
        return latitude to longitude
    }

    /** Blank loses to a real title. Two different titles resolve to the same value on every device. */
    fun mergeTitle(a: String, b: String): String = when {
        a.isEmpty() -> b
        b.isEmpty() -> a
        else -> minOf(a, b)
    }

    /** A missing fix loses to a recorded one. Two different fixes resolve deterministically. */
    fun mergeLocation(
        firstLatitude: Double?,
        firstLongitude: Double?,
        secondLatitude: Double?,
        secondLongitude: Double?,
    ): Pair<Double?, Double?> {
        val first = normalizeLocation(firstLatitude, firstLongitude)
        val second = normalizeLocation(secondLatitude, secondLongitude)
        return when {
            first.first == null -> second
            second.first == null -> first
            first == second -> first
            else -> minOf(
                first,
                second,
                compareBy<Pair<Double?, Double?>>({ it.first!! }, { it.second!! }),
            )
        }
    }

    fun formatLocation(latitude: Double, longitude: Double): String {
        fun hemisphere(value: Double, positive: String, negative: String): String {
            val side = if (value >= 0) positive else negative
            return "%.5f° %s".format(kotlin.math.abs(value), side)
        }
        return "${hemisphere(latitude, "N", "S")}, ${hemisphere(longitude, "E", "W")}"
    }
}
