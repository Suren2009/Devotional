package com.pradarshana.core.data.sync

import com.google.android.gms.wearable.DataMap
import com.pradarshana.core.model.SessionSnapshot
import com.pradarshana.core.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SnapshotCodecTest {

    private val snapshot = SessionSnapshot(
        sessionId = "abc",
        targetCount = 108,
        startTime = 100,
        endTime = null,
        status = SessionStatus.PAUSED,
        statusUpdatedAt = 150,
        statusUpdatedBy = "watch-1",
        lastUpdatedTime = 200,
        originDeviceId = "phone-1",
        deviceCounts = sortedMapOf("phone-1" to 20, "watch-1" to 7),
        title = "Meenakshi",
        latitude = 9.9195,
        longitude = 78.1193,
    )

    @Test
    fun roundTripsThroughDataMap() {
        val map = DataMap()
        SnapshotCodec.write(map, snapshot, "phone-1")
        val decoded = SnapshotCodec.read(DataMap.fromByteArray(map.toByteArray()))!!
        assertEquals("phone-1", decoded.sourceDeviceId)
        assertEquals(snapshot, decoded.snapshot)
        assertEquals(27, decoded.snapshot.completedCount)
        assertEquals(81, map.getInt("remaining_count"))
    }

    @Test
    fun endTimeRoundTrips() {
        val done = snapshot.copy(status = SessionStatus.CANCELLED, endTime = 300)
        val map = DataMap().also { SnapshotCodec.write(it, done, "watch-1") }
        assertEquals(300L, SnapshotCodec.read(map)!!.snapshot.endTime)
    }

    @Test
    fun malformedPayloadsAreIgnored() {
        assertNull(SnapshotCodec.read(DataMap()))
        val map = DataMap().also { SnapshotCodec.write(it, snapshot, "phone-1") }
        map.putInt("target_count", 0)
        assertNull(SnapshotCodec.read(map))
        val future = DataMap().also { SnapshotCodec.write(it, snapshot, "phone-1") }
        future.putInt("schema", 99)
        assertNull(SnapshotCodec.read(future))
    }
}
