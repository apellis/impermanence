package com.neversink.impermanence.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormattingTest {
    @Test
    fun `whole minutes show no seconds`() {
        assertEquals("7:15 AM", TimeFormatting.formattedTimeFromSeconds(7 * 3600 + 15 * 60, use24HourClock = false))
        assertEquals("07:15", TimeFormatting.formattedTimeFromSeconds(7 * 3600 + 15 * 60, use24HourClock = true))
        assertEquals("12:00 AM", TimeFormatting.formattedTimeFromSeconds(0, use24HourClock = false))
    }

    @Test
    fun `times off the minute add seconds`() {
        val t = 7 * 3600 + 15 * 60 + 30
        assertEquals("7:15:30 AM", TimeFormatting.formattedTimeFromSeconds(t, use24HourClock = false))
        assertEquals("07:15:30", TimeFormatting.formattedTimeFromSeconds(t, use24HourClock = true))
    }

    @Test
    fun `times wrap past midnight`() {
        assertEquals("01:00", TimeFormatting.formattedTimeFromSeconds(25 * 3600, use24HourClock = true))
    }

    @Test
    fun `durations read in hours minutes and seconds`() {
        assertEquals("1h 5m", TimeFormatting.formattedDurationSeconds(3900))
        assertEquals("45m", TimeFormatting.formattedDurationSeconds(2700))
        assertEquals("2m 30s", TimeFormatting.formattedDurationSeconds(150))
        assertEquals("1s", TimeFormatting.formattedDurationSeconds(1))
        assertEquals("0m", TimeFormatting.formattedDurationSeconds(0))
        assertEquals("24h", TimeFormatting.formattedDurationMinutes(1440))
    }
}
