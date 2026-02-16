package com.impermanence.impermanence.domain.timer

import com.impermanence.impermanence.model.Bell
import com.impermanence.impermanence.model.Day
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DayTimerEngineTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    @Test
    fun `rings start and end bells on transitions`() {
        val startBell = Bell(soundId = 0, numRings = 2)
        val endBell = Bell(soundId = 0, numRings = 3)
        val day = Day(
            name = "Test Day",
            startTimeSeconds = 60 * 60,
            segments = listOf(
                Day.Segment(
                    name = "Sit",
                    durationSeconds = 2 * 60,
                    customEndBell = endBell
                )
            ),
            startBell = startBell,
            manualBell = Bell.SingleBell,
            defaultBell = Bell.SingleBell
        )
        val engine = DayTimerEngine(day, loopDays = false)
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        val beforeStart = engine.evaluate(startOfDay.plusSeconds((60 * 60 - 1).toLong()))
        assertNull(beforeStart.bell)

        val atStart = engine.evaluate(startOfDay.plusSeconds((60 * 60).toLong()))
        assertEquals(startBell, atStart.bell)

        val atEnd = engine.evaluate(startOfDay.plusSeconds((60 * 60 + 2 * 60).toLong()))
        assertEquals(endBell, atEnd.bell)
        assertEquals(DayTimerEngine.TimerStatus.COMPLETE, atEnd.state.status)
    }

    @Test
    fun `does not roll schedules when loop days is off`() {
        val day = Day(
            name = "No Loop",
            startTimeSeconds = 0,
            segments = listOf(Day.Segment(name = "Sit", durationSeconds = 60)),
            startBell = Bell.SingleBell,
            manualBell = Bell.SingleBell,
            defaultBell = Bell.SingleBell
        )
        val engine = DayTimerEngine(day, loopDays = false)
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        val initial = engine.evaluate(startOfDay.plusSeconds(10))
        val initialStartEpoch = initial.state.schedules.first().startEpochSecond

        val nextDay = engine.evaluate(startOfDay.plus(1, ChronoUnit.DAYS).plusSeconds(10))
        val nextDayStartEpoch = nextDay.state.schedules.first().startEpochSecond

        assertEquals(initialStartEpoch, nextDayStartEpoch)
        assertEquals(DayTimerEngine.TimerStatus.COMPLETE, nextDay.state.status)
    }

    @Test
    fun `rolls schedules forward when loop days is on`() {
        val day = Day(
            name = "Loop",
            startTimeSeconds = 0,
            segments = listOf(Day.Segment(name = "Sit", durationSeconds = 60)),
            startBell = Bell.SingleBell,
            manualBell = Bell.SingleBell,
            defaultBell = Bell.SingleBell
        )
        val engine = DayTimerEngine(day, loopDays = true)
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        val initial = engine.evaluate(startOfDay.plusSeconds(10))
        val initialStartEpoch = initial.state.schedules.first().startEpochSecond

        val nextDay = engine.evaluate(startOfDay.plus(1, ChronoUnit.DAYS).plusSeconds(10))
        val nextDayStartEpoch = nextDay.state.schedules.first().startEpochSecond

        val initialDate = Instant.ofEpochSecond(initialStartEpoch).atZone(zone).toLocalDate()
        val nextDate = Instant.ofEpochSecond(nextDayStartEpoch).atZone(zone).toLocalDate()

        assertEquals(initialDate.plusDays(1), nextDate)
        assertEquals(DayTimerEngine.TimerStatus.ACTIVE, nextDay.state.status)
        assertTrue(nextDayStartEpoch > initialStartEpoch)
    }
}
