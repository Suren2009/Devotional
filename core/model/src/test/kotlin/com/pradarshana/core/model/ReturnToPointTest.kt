package com.pradarshana.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnToPointTest {

    @Test
    fun samePointIsZeroMeters() {
        assertEquals(0.0, Geo.metersBetween(13.03, 80.27, 13.03, 80.27), 0.001)
    }

    @Test
    fun standingAtTheStartDoesNotCount() {
        val tracker = ReturnToPointTracker(30, 20, anchorLatitude = 13.0, anchorLongitude = 80.0)
        val visit = tracker.onFix(13.0, 80.0, accuracyMeters = 5f, timeMillis = 1_000)
        assertEquals(ReturnVisit.Watching(0, leftStart = false), visit)
    }

    @Test
    fun leavingAndReturningCountsOnceUntilTheWalkerLeavesAgain() {
        val tracker = ReturnToPointTracker(30, 20, anchorLatitude = 13.0, anchorLongitude = 80.0)
        val away = northOf(13.0, 80.0, 40.0)
        val back = northOf(13.0, 80.0, 10.0)
        val left = tracker.onFix(away.first, away.second, 5f, 1_000)
        assertTrue(left is ReturnVisit.Watching && left.leftStart)
        assertEquals(ReturnVisit.Counted, tracker.onFix(back.first, back.second, 5f, 30_000))
        val stillThere = tracker.onFix(back.first, back.second, 5f, 60_000)
        assertTrue(stillThere is ReturnVisit.Watching && !stillThere.leftStart)
        val leftAgain = tracker.onFix(away.first, away.second, 5f, 70_000)
        assertTrue(leftAgain is ReturnVisit.Watching && leftAgain.leftStart)
        assertEquals(ReturnVisit.Counted, tracker.onFix(13.0, 80.0, 5f, 100_000))
    }

    @Test
    fun aReturnInsideTheGapDoesNotCount() {
        val tracker = ReturnToPointTracker(30, 20, anchorLatitude = 13.0, anchorLongitude = 80.0)
        val away = northOf(13.0, 80.0, 40.0)
        tracker.onFix(away.first, away.second, 5f, 1_000)
        assertEquals(ReturnVisit.Counted, tracker.onFix(13.0, 80.0, 5f, 30_000))
        tracker.onFix(away.first, away.second, 5f, 35_000)
        val tooSoon = tracker.onFix(13.0, 80.0, 5f, 40_000)
        assertTrue(tooSoon is ReturnVisit.Watching && tooSoon.leftStart)
    }

    @Test
    fun aPoorFixIsIgnored() {
        val tracker = ReturnToPointTracker(30, 20, anchorLatitude = 13.0, anchorLongitude = 80.0)
        val away = northOf(13.0, 80.0, 40.0)
        assertEquals(
            ReturnVisit.Ignored,
            tracker.onFix(away.first, away.second, accuracyMeters = 80f, timeMillis = 1_000),
        )
        assertEquals(ReturnVisit.Watching(0, leftStart = false), tracker.onFix(13.0, 80.0, 5f, 2_000))
    }

    @Test
    fun theFirstGoodFixBecomesTheStartWhenNoneWasSaved() {
        val tracker = ReturnToPointTracker(30, 20)
        assertEquals(ReturnVisit.WaitingForStart, tracker.onFix(13.0, 80.0, 45f, 500))
        val visit = tracker.onFix(13.0, 80.0, 8f, 1_000)
        assertTrue(visit is ReturnVisit.AnchorChosen)
        assertEquals(ReturnVisit.Watching(0, leftStart = false), tracker.onFix(13.0, 80.0, 8f, 2_000))
    }

    @Test
    fun betweenTheTwoDistancesDoesNotCountAsAReturn() {
        val tracker = ReturnToPointTracker(30, 20, anchorLatitude = 13.0, anchorLongitude = 80.0)
        val away = northOf(13.0, 80.0, 40.0)
        val middle = northOf(13.0, 80.0, 25.0)
        tracker.onFix(away.first, away.second, 5f, 1_000)
        val middleVisit = tracker.onFix(middle.first, middle.second, 5f, 40_000)
        assertTrue(middleVisit is ReturnVisit.Watching && middleVisit.leftStart)
    }

    private fun northOf(latitude: Double, longitude: Double, meters: Double): Pair<Double, Double> {
        val moved = latitude + meters / 111_320.0
        val distance = Geo.metersBetween(latitude, longitude, moved, longitude)
        assertTrue(distance in meters * 0.98..meters * 1.02)
        return moved to longitude
    }
}
