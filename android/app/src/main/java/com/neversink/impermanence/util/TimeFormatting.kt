package com.neversink.impermanence.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormatting {
    private val twelveHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val twentyFourHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val twelveHourSecondsFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.US)
    private val twentyFourHourSecondsFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)

    /** A time of day, with seconds only when it isn't on a whole minute. */
    fun formattedTimeFromSeconds(seconds: Int, use24HourClock: Boolean): String {
        val normalized = ((seconds % 86400) + 86400) % 86400
        val time = LocalTime.ofSecondOfDay(normalized.toLong())
        val withSeconds = normalized % 60 != 0
        val formatter = when {
            use24HourClock && withSeconds -> twentyFourHourSecondsFormatter
            use24HourClock -> twentyFourHourFormatter
            withSeconds -> twelveHourSecondsFormatter
            else -> twelveHourFormatter
        }
        return time.format(formatter)
    }

    fun formattedDurationMinutes(minutes: Int): String = formattedDurationSeconds(minutes * 60)

    /** A length like "1h 5m", "45m" or "2m 30s". */
    fun formattedDurationSeconds(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val parts = buildList {
            if (hours > 0) add("${hours}h")
            if (minutes > 0) add("${minutes}m")
            if (seconds > 0) add("${seconds}s")
        }
        return parts.ifEmpty { listOf("0m") }.joinToString(" ")
    }
}
