package com.pradarshana.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMergerTest {

    private fun snapshot(
        counts: Map<String, Int>,
        status: SessionStatus = SessionStatus.ACTIVE,
        statusAt: Long = 0,
        statusBy: String = PHONE,
        endTime: Long? = null,
        updated: Long = 0,
        target: Int = 108,
    ) = SessionSnapshot(
        sessionId = "s1",
        targetCount = target,
        startTime = 0,
        endTime = endTime,
        status = status,
        statusUpdatedAt = statusAt,
        statusUpdatedBy = statusBy,
        lastUpdatedTime = updated,
        originDeviceId = PHONE,
        deviceCounts = counts,
    )

    @Test
    fun `receiving the same snapshot repeatedly never double counts`() {
        val local = snapshot(mapOf(PHONE to 10))
        val remote = snapshot(mapOf(PHONE to 10, WATCH to 5))
        var state = SessionMerger.merge(local, remote).merged
        repeat(5) { state = SessionMerger.merge(state, remote).merged }
        assertEquals(15, state.completedCount)
        assertFalse(SessionMerger.merge(state, remote).changed)
    }

    @Test
    fun `concurrent increments on both devices are all kept`() {
        val base = snapshot(mapOf(PHONE to 10, WATCH to 4))
        val phone = base.copy(deviceCounts = mapOf(PHONE to 13, WATCH to 4))
        val watch = base.copy(deviceCounts = mapOf(PHONE to 10, WATCH to 9))
        val onPhone = SessionMerger.merge(phone, watch).merged
        val onWatch = SessionMerger.merge(watch, phone).merged
        assertEquals(22, onPhone.completedCount)
        assertEquals(onPhone, onWatch)
    }

    @Test
    fun `stale snapshots never decrease the count`() {
        val local = snapshot(mapOf(PHONE to 20, WATCH to 7))
        val stale = snapshot(mapOf(PHONE to 3, WATCH to 2))
        val result = SessionMerger.merge(local, stale)
        assertEquals(27, result.merged.completedCount)
        assertFalse(result.changed)
    }

    @Test
    fun `merge is order independent`() {
        val a = snapshot(mapOf(PHONE to 5), status = SessionStatus.PAUSED, statusAt = 5, updated = 5)
        val b = snapshot(mapOf(WATCH to 6), status = SessionStatus.ACTIVE, statusAt = 9, statusBy = WATCH, updated = 9)
        val c = snapshot(mapOf(PHONE to 7, WATCH to 1), status = SessionStatus.PAUSED, statusAt = 12, updated = 12)
        val abc = SessionMerger.merge(SessionMerger.merge(a, b).merged, c).merged
        val cba = SessionMerger.merge(SessionMerger.merge(c, b).merged, a).merged
        val bac = SessionMerger.merge(SessionMerger.merge(b, a).merged, c).merged
        assertEquals(abc, cba)
        assertEquals(abc, bac)
        assertEquals(13, abc.completedCount)
        assertEquals(SessionStatus.PAUSED, abc.status)
    }

    @Test
    fun `latest pause or resume wins`() {
        val paused = snapshot(mapOf(PHONE to 1), status = SessionStatus.PAUSED, statusAt = 100)
        val resumed = snapshot(mapOf(PHONE to 1), status = SessionStatus.ACTIVE, statusAt = 200, statusBy = WATCH)
        assertEquals(SessionStatus.ACTIVE, SessionMerger.merge(paused, resumed).merged.status)
        assertEquals(SessionStatus.ACTIVE, SessionMerger.merge(resumed, paused).merged.status)
    }

    @Test
    fun `reaching the target through merge completes exactly once`() {
        val phone = snapshot(mapOf(PHONE to 100, WATCH to 0), updated = 50)
        val watch = snapshot(mapOf(PHONE to 0, WATCH to 8), updated = 60)
        val first = SessionMerger.merge(phone, watch)
        assertEquals(SessionStatus.COMPLETED, first.merged.status)
        assertEquals(108, first.merged.completedCount)
        assertEquals(60L, first.merged.endTime)
        assertTrue(first.newlyCompleted)

        val second = SessionMerger.merge(first.merged, watch)
        assertFalse(second.newlyCompleted)
        assertFalse(second.changed)
    }

    @Test
    fun `overshoot from offline devices is capped at target`() {
        val phone = snapshot(mapOf(PHONE to 107, WATCH to 0))
        val watch = snapshot(mapOf(PHONE to 100, WATCH to 5))
        val merged = SessionMerger.merge(phone, watch).merged
        assertEquals(108, merged.completedCount)
        assertEquals(0, merged.remainingCount)
        assertEquals(SessionStatus.COMPLETED, merged.status)
    }

    @Test
    fun `completion recorded on watch keeps its original end time`() {
        val phone = snapshot(mapOf(PHONE to 0, WATCH to 50), updated = 10)
        val watch = snapshot(
            mapOf(WATCH to 108), status = SessionStatus.COMPLETED, statusAt = 500, statusBy = WATCH,
            endTime = 500, updated = 500,
        )
        val merged = SessionMerger.merge(phone, watch).merged
        assertEquals(SessionStatus.COMPLETED, merged.status)
        assertEquals(500L, merged.endTime)
        assertEquals(merged, SessionMerger.merge(watch, phone).merged)
    }

    @Test
    fun `cancellation is sticky against later pause or resume`() {
        val cancelled = snapshot(mapOf(PHONE to 3), status = SessionStatus.CANCELLED, statusAt = 10, endTime = 10)
        val resumed = snapshot(mapOf(PHONE to 3, WATCH to 2), status = SessionStatus.ACTIVE, statusAt = 99, statusBy = WATCH)
        val merged = SessionMerger.merge(resumed, cancelled).merged
        assertEquals(SessionStatus.CANCELLED, merged.status)
        assertEquals(5, merged.completedCount)
        assertEquals(10L, merged.endTime)
    }

    @Test
    fun `an existing session keeps its kind and a new one takes the remote kind`() {
        val local = snapshot(mapOf(PHONE to 1))
        val remote = snapshot(mapOf(PHONE to 2)).copy(kind = SessionKind.CHANTING)
        assertEquals(SessionKind.PRADARSHANA, SessionMerger.merge(local, remote).merged.kind)
        assertEquals(SessionKind.CHANTING, SessionMerger.merge(null, remote).merged.kind)
    }

    @Test
    fun `new remote session is adopted as is`() {
        val remote = snapshot(mapOf(WATCH to 4), statusBy = WATCH)
        val result = SessionMerger.merge(null, remote)
        assertTrue(result.changed)
        assertEquals(4, result.merged.completedCount)
    }

    @Test
    fun `an undo survives a repeated snapshot and does not restore the count`() {
        val counted = snapshot(mapOf(PHONE to 4))
        val undone = counted.copy(deviceUndos = mapOf(PHONE to 1))
        val once = SessionMerger.merge(counted, undone).merged
        assertEquals(3, once.completedCount)
        val again = SessionMerger.merge(once, undone).merged
        assertEquals(3, again.completedCount)
        val stale = SessionMerger.merge(again, counted).merged
        assertEquals(3, stale.completedCount)
        assertFalse(SessionMerger.merge(stale, undone).changed)
    }

    @Test
    fun `a finished session stays finished when a late undo arrives`() {
        val done = snapshot(
            mapOf(PHONE to 10),
            status = SessionStatus.COMPLETED,
            statusAt = 20,
            endTime = 20,
            target = 10,
        )
        val late = snapshot(mapOf(PHONE to 10), statusAt = 19).copy(deviceUndos = mapOf(PHONE to 1))
        val merged = SessionMerger.merge(done, late).merged
        assertEquals(SessionStatus.COMPLETED, merged.status)
        assertEquals(10, merged.completedCount)
        assertEquals(1, merged.deviceUndos[PHONE])
    }

    @Test
    fun `malformed completed status below target is downgraded`() {
        val bogus = snapshot(mapOf(WATCH to 3), status = SessionStatus.COMPLETED, statusAt = 5, endTime = 5)
        val result = SessionMerger.merge(null, bogus)
        assertEquals(SessionStatus.ACTIVE, result.merged.status)
        assertEquals(3, result.merged.toSession().completedCount)
    }

    private companion object {
        const val PHONE = "phone-device"
        const val WATCH = "watch-device"
    }
}
