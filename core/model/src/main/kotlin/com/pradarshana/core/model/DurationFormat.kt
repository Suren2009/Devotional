package com.pradarshana.core.model

object DurationFormat {
    /** Formats as `1h 05m 09s`, `12m 03s` or `45s`. */
    fun format(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0) / 1000)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "%dh %02dm %02ds".format(hours, minutes, seconds)
            minutes > 0 -> "%dm %02ds".format(minutes, seconds)
            else -> "%ds".format(seconds)
        }
    }

    /** Compact clock form for small screens: `1:05:09` or `12:03`. */
    fun formatCompact(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0) / 1000)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
    }
}
