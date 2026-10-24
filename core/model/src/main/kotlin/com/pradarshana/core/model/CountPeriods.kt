package com.pradarshana.core.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/** Sums of counts whose sessions started today, this week (from Monday), and this month. */
object CountPeriods {
    data class Totals(val today: Int, val week: Int, val month: Int)

    fun totals(rows: List<Pair<Long, Int>>, now: Long, zone: ZoneId): Totals {
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val todayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekStart = date.with(DayOfWeek.MONDAY).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthStart = date.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var today = 0
        var week = 0
        var month = 0
        for ((start, count) in rows) {
            val counted = count.coerceAtLeast(0)
            if (start >= todayStart) today += counted
            if (start >= weekStart) week += counted
            if (start >= monthStart) month += counted
        }
        return Totals(today, week, month)
    }
}
