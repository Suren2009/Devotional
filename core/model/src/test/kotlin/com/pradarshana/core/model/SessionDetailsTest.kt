package com.pradarshana.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDetailsTest {

    @Test
    fun blankTitleIsShownAsUntitled() {
        assertEquals("Untitled", SessionDetails.displayTitle(""))
        assertEquals("Untitled", PradarshanaSession("s", 1, 0, 0, null, SessionStatus.ACTIVE, 0).displayTitle)
    }

    @Test
    fun titleIsTrimmedAndCapped() {
        assertEquals("Meenakshi Temple", SessionDetails.normalizeTitle("  Meenakshi   Temple \n"))
        assertEquals(60, SessionDetails.normalizeTitle("a".repeat(80)).length)
    }

    @Test
    fun locationMustBeAValidPair() {
        assertEquals(13.03 to 80.27, SessionDetails.normalizeLocation(13.03, 80.27))
        assertNull(SessionDetails.normalizeLocation(null, 80.0).first)
        assertNull(SessionDetails.normalizeLocation(91.0, 0.0).first)
        assertNull(SessionDetails.normalizeLocation(Double.NaN, 0.0).first)
    }

    @Test
    fun mergeKeepsATitleAndFixRecordedOnOnlyOneDevice() {
        val phone = snapshot(title = "Kapaleeshwarar", latitude = 13.033, longitude = 80.270)
        val watch = snapshot()
        val merged = SessionMerger.merge(watch, phone).merged
        assertEquals("Kapaleeshwarar", merged.title)
        assertEquals(13.033, merged.latitude!!, 0.0)
        assertEquals(80.270, merged.longitude!!, 0.0)
        assertEquals(merged, SessionMerger.merge(phone, watch).merged)
    }

    @Test
    fun conflictingTitlesAndFixesResolveTheSameWayOnBothDevices() {
        val a = snapshot(title = "Temple", latitude = 10.0, longitude = 20.0)
        val b = snapshot(title = "Another", latitude = 11.0, longitude = 21.0)
        val left = SessionMerger.merge(a, b).merged
        val right = SessionMerger.merge(b, a).merged
        assertEquals(left.title, right.title)
        assertEquals(left.latitude, right.latitude)
        assertEquals("Another", left.title)
        assertEquals(10.0, left.latitude!!, 0.0)
    }

    @Test
    fun sessionReportsWhetherAPlaceWasSaved() {
        val placed = PradarshanaSession("s", 1, 0, 0, null, SessionStatus.ACTIVE, 0, latitude = 1.0, longitude = 2.0)
        assertTrue(placed.hasLocation)
        assertEquals("1.00000° N, 2.00000° E", SessionDetails.formatLocation(1.0, 2.0))
        assertFalse(PradarshanaSession("s", 1, 0, 0, null, SessionStatus.ACTIVE, 0).hasLocation)
    }

    private fun snapshot(
        title: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
    ) = SessionSnapshot(
        sessionId = "s1",
        targetCount = 11,
        startTime = 0,
        endTime = null,
        status = SessionStatus.ACTIVE,
        statusUpdatedAt = 0,
        statusUpdatedBy = "phone",
        lastUpdatedTime = 0,
        originDeviceId = "phone",
        deviceCounts = mapOf("phone" to 1),
        title = title,
        latitude = latitude,
        longitude = longitude,
    )
}
