package com.pradarshana.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object Formatting {
    private val dateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    private val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    private val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun dateTime(epochMillis: Long): String = dateTime.format(zoned(epochMillis))

    fun date(epochMillis: Long): String = date.format(zoned(epochMillis))

    fun time(epochMillis: Long): String = time.format(zoned(epochMillis))

    private fun zoned(epochMillis: Long) = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
}
