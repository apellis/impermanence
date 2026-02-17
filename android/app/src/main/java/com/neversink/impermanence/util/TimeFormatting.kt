package com.neversink.impermanence.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormatting {
    private val twelveHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val twentyFourHourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun formattedTimeFromSeconds(seconds: Int, use24HourClock: Boolean): String {
        val normalized = ((seconds % 86400) + 86400) % 86400
        val time = LocalTime.ofSecondOfDay(normalized.toLong())
        val formatter = if (use24HourClock) twentyFourHourFormatter else twelveHourFormatter
        return time.format(formatter)
    }

    fun formattedDurationMinutes(minutes: Int): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return when {
            hours == 0 -> "${remainder}m"
            remainder == 0 -> "${hours}h"
            else -> "${hours}h ${remainder}m"
        }
    }
}
