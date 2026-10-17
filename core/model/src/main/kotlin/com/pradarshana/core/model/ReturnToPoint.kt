package com.pradarshana.core.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Distances the devotee can change in Settings. The return distance stays shorter than the walk away. */
object AutoCountDistances {
    const val DEFAULT_AWAY_METERS = 30
    const val DEFAULT_RETURN_METERS = 20
    const val MIN_AWAY_METERS = 10
    const val MAX_AWAY_METERS = 500
    const val MIN_RETURN_METERS = 5

    fun isValid(awayMeters: Int, returnMeters: Int): Boolean =
        awayMeters in MIN_AWAY_METERS..MAX_AWAY_METERS &&
            returnMeters in MIN_RETURN_METERS until awayMeters
}

object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun metersBetween(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
        val startLat = Math.toRadians(latitude1)
        val endLat = Math.toRadians(latitude2)
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLon = Math.toRadians(longitude2 - longitude1)
        val a = sin(dLat / 2).pow(2) + cos(startLat) * cos(endLat) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }
}

sealed interface ReturnVisit {
    data object Ignored : ReturnVisit
    data object WaitingForStart : ReturnVisit
    data class AnchorChosen(val latitude: Double, val longitude: Double) : ReturnVisit
    data class Watching(val distanceMeters: Int, val leftStart: Boolean) : ReturnVisit
    data object Counted : ReturnVisit
}

/**
 * Counts one round only after the walker leaves the start and later comes back to it.
 * Standing at the start never counts, and a poor fix is ignored.
 */
class ReturnToPointTracker(
    awayMeters: Int,
    returnMeters: Int,
    anchorLatitude: Double? = null,
    anchorLongitude: Double? = null,
) {
    var awayMeters: Int = awayMeters
        private set
    var returnMeters: Int = returnMeters
        private set

    private var anchorLatitude: Double? = anchorLatitude
    private var anchorLongitude: Double? = anchorLongitude
    private var hasLeftStart: Boolean = false
    private var lastCountedAtMillis: Long? = null

    fun updateDistances(awayMeters: Int, returnMeters: Int) {
        this.awayMeters = awayMeters
        this.returnMeters = returnMeters
    }

    fun onFix(latitude: Double, longitude: Double, accuracyMeters: Float, timeMillis: Long): ReturnVisit {
        if (!accuracyMeters.isFinite()) return ReturnVisit.Ignored
        val anchorLat = anchorLatitude
        val anchorLng = anchorLongitude
        if (anchorLat == null || anchorLng == null) {
            if (!usable(accuracyMeters, ANCHOR_ACCURACY_METERS)) return ReturnVisit.WaitingForStart
            anchorLatitude = latitude
            anchorLongitude = longitude
            return ReturnVisit.AnchorChosen(latitude, longitude)
        }
        if (!usable(accuracyMeters, TRACK_ACCURACY_METERS)) return ReturnVisit.Ignored
        val distance = Geo.metersBetween(anchorLat, anchorLng, latitude, longitude).toInt()
        if (!hasLeftStart) {
            if (distance >= awayMeters) hasLeftStart = true
            return ReturnVisit.Watching(distance, hasLeftStart)
        }
        val previousCount = lastCountedAtMillis
        val gapElapsed = previousCount == null || timeMillis - previousCount >= MIN_GAP_MILLIS
        if (distance <= returnMeters && gapElapsed) {
            hasLeftStart = false
            lastCountedAtMillis = timeMillis
            return ReturnVisit.Counted
        }
        return ReturnVisit.Watching(distance, hasLeftStart)
    }

    private fun usable(accuracyMeters: Float, limit: Float): Boolean =
        accuracyMeters <= 0f || accuracyMeters <= limit

    companion object {
        /** A clear reading is required before the starting place is saved. */
        const val ANCHOR_ACCURACY_METERS = 30f

        /** Later readings can be a little less precise and still follow the walk. */
        const val TRACK_ACCURACY_METERS = 55f
        const val MIN_GAP_MILLIS = 20_000L
    }
}
