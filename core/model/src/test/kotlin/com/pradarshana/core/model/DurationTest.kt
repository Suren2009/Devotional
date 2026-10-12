package com.pradarshana.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTest {

    @Test
    fun totalTimeIsStartToEndWithoutThePausedGap() {
        val session = PradarshanaSession(
            id = "s",
            targetCount = 3,
            completedCount = 3,
            startTime = 1_000,
            endTime = 31_000,
            status = SessionStatus.COMPLETED,
            lastUpdatedTime = 31_000,
            pausedMillis = 20_000,
            statusUpdatedAt = 31_000,
        )
        assertEquals(10_000, session.durationMillis())
    }

    @Test
    fun aPauseStillInProgressIsLeftOut() {
        val session = PradarshanaSession(
            id = "s",
            targetCount = 3,
            completedCount = 1,
            startTime = 0,
            endTime = null,
            status = SessionStatus.PAUSED,
            lastUpdatedTime = 5_000,
            pausedMillis = 0,
            statusUpdatedAt = 5_000,
        )
        assertEquals(5_000, session.durationMillis(now = 25_000))
    }
}
