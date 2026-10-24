package com.pradarshana.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CountPeriodsTest {
    @Test
    fun todayWeekAndMonthUseTheSessionStart() {
        val zone = ZoneId.of("Asia/Kolkata")
        val today = LocalDate.of(2026, 10, 24).atStartOfDay(zone).toInstant().toEpochMilli()
        val yesterday = LocalDate.of(2026, 10, 23).atStartOfDay(zone).plusHours(12).toInstant().toEpochMilli()
        val lastMonth = LocalDate.of(2026, 9, 30).atStartOfDay(zone).toInstant().toEpochMilli()
        val totals = CountPeriods.totals(
            listOf(today to 10, yesterday to 4, lastMonth to 100),
            today + 3_600_000,
            zone,
        )
        assertEquals(10, totals.today)
        assertEquals(14, totals.week)
        assertEquals(14, totals.month)
    }
}
