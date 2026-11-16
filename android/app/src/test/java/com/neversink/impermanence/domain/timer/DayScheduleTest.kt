package com.neversink.impermanence.domain.timer

import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DayScheduleTest {
    private val ny = ZoneId.of("America/New_York")
    private val springForward = LocalDate.of(2026, 3, 8) // 02:00 EST -> 03:00 EDT
    private val fallBack = LocalDate.of(2026, 11, 1) // 02:00 EDT -> 01:00 EST
    private val ordinary = LocalDate.of(2026, 6, 10)

    private fun at(date: LocalDate, hour: Int, minute: Int, second: Int = 0, zone: ZoneId = ny): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute, second), zone).toInstant().toEpochMilli()

    private fun utc(text: String): Long = Instant.parse(text).toEpochMilli()

    private fun day(startHour: Int, startMinute: Int, vararg minutes: Int, startBell: Bell = Bell(0, 2)) = Day(
        id = "d1",
        name = "Test",
        startTimeSeconds = startHour * 3600 + startMinute * 60,
        segments = minutes.mapIndexed { i, m -> Day.Segment(id = "s$i", name = "Seg $i", durationSeconds = m * 60) },
        startBell = startBell,
        defaultBell = Bell(0, 1)
    )

    @Test
    fun `events are the start bell then each segment end`() {
        val d = day(7, 0, 45, 30)
        val occ = DaySchedule.occurrence(d, ordinary, ny)!!
        val events = DaySchedule.events(d, occ)
        assertEquals(listOf(at(ordinary, 7, 0), at(ordinary, 7, 45), at(ordinary, 8, 15)), events.map { it.atMillis })
        assertEquals(listOf(DaySchedule.EventKind.START, DaySchedule.EventKind.SEGMENT_END, DaySchedule.EventKind.SEGMENT_END), events.map { it.kind })
        assertEquals(Bell(0, 2), events[0].bell)
        assertEquals(listOf(-1, 0, 1), events.map { it.segmentIndex })
        assertEquals(listOf("2026-06-10/start", "2026-06-10/end-0", "2026-06-10/end-1"), events.map { it.key })
    }

    @Test
    fun `segment bells use custom bell or default and are sanitized`() {
        val d = Day(
            id = "d",
            name = "T",
            startTimeSeconds = 0,
            segments = listOf(
                Day.Segment(id = "a", name = "A", durationSeconds = 60, customEndBell = Bell(99, 40)),
                Day.Segment(id = "b", name = "B", durationSeconds = 60)
            ),
            startBell = Bell(0, 0),
            defaultBell = Bell(0, 5)
        )
        val events = DaySchedule.events(d, DaySchedule.occurrence(d, ordinary, ny)!!)
        assertEquals(Bell(0, 1), events[0].bell)
        assertEquals(Bell(0, 12), events[1].bell)
        assertEquals(Bell(0, 5), events[2].bell)
    }

    @Test
    fun `empty day has no occurrence`() {
        assertNull(DaySchedule.occurrence(day(7, 0), ordinary, ny))
    }

    @Test
    fun `segments are absolute durations across spring forward`() {
        val d = day(1, 30, 60)
        val occ = DaySchedule.occurrence(d, springForward, ny)!!
        assertEquals(utc("2026-03-08T06:30:00Z"), occ.startMillis) // 01:30 EST
        assertEquals(utc("2026-03-08T07:30:00Z"), occ.endMillis) // 03:30 EDT, one real hour later
    }

    @Test
    fun `start time in the DST gap shifts forward by the gap`() {
        val occ = DaySchedule.occurrence(day(2, 30, 10), springForward, ny)!!
        assertEquals(at(springForward, 3, 30), occ.startMillis)
        assertEquals(utc("2026-03-08T07:30:00Z"), occ.startMillis)
    }

    @Test
    fun `ambiguous fall back start uses the first occurrence`() {
        val occ = DaySchedule.occurrence(day(1, 30, 60), fallBack, ny)!!
        assertEquals(utc("2026-11-01T05:30:00Z"), occ.startMillis) // 01:30 EDT
        assertEquals(utc("2026-11-01T06:30:00Z"), occ.endMillis) // 01:30 EST, one real hour later
    }

    @Test
    fun `wall clock start is kept on the day after a DST change`() {
        val d = day(7, 0, 45)
        val before = DaySchedule.occurrence(d, springForward.minusDays(1), ny)!!
        val after = DaySchedule.occurrence(d, springForward, ny)!!
        assertEquals(at(springForward, 7, 0), after.startMillis)
        assertEquals(23L * 3600_000L, after.startMillis - before.startMillis)
        val fallBefore = DaySchedule.occurrence(d, fallBack.minusDays(1), ny)!!
        val fallAfter = DaySchedule.occurrence(d, fallBack, ny)!!
        assertEquals(25L * 3600_000L, fallAfter.startMillis - fallBefore.startMillis)
    }

    @Test
    fun `initial date picks yesterday while a day crossing midnight is still running`() {
        val d = day(22, 0, 240) // 22:00 to 02:00
        val today = ordinary
        assertEquals(today.minusDays(1), DaySchedule.initialDate(d, at(today, 1, 0), ny))
        assertEquals(today, DaySchedule.initialDate(d, at(today, 2, 0), ny))
        assertEquals(today, DaySchedule.initialDate(d, at(today, 23, 0), ny))
    }

    @Test
    fun `initial date is today for a normal day at any time`() {
        val d = day(7, 0, 60)
        assertEquals(ordinary, DaySchedule.initialDate(d, at(ordinary, 0, 0), ny))
        assertEquals(ordinary, DaySchedule.initialDate(d, at(ordinary, 23, 59), ny))
    }

    @Test
    fun `normal day rolls over only after midnight`() {
        val d = day(7, 0, 60)
        assertFalse(DaySchedule.shouldRollOver(d, ordinary, at(ordinary, 9, 0), ny, loopDays = true))
        assertFalse(DaySchedule.shouldRollOver(d, ordinary, at(ordinary, 23, 59, 59), ny, loopDays = true))
        assertTrue(DaySchedule.shouldRollOver(d, ordinary, at(ordinary.plusDays(1), 0, 0), ny, loopDays = true))
        assertFalse(DaySchedule.shouldRollOver(d, ordinary, at(ordinary.plusDays(1), 0, 0), ny, loopDays = false))
    }

    @Test
    fun `day crossing midnight rolls over at its end not at midnight`() {
        val d = day(22, 0, 240)
        assertFalse(DaySchedule.shouldRollOver(d, ordinary, at(ordinary.plusDays(1), 0, 30), ny, loopDays = true))
        assertFalse(DaySchedule.shouldRollOver(d, ordinary, at(ordinary.plusDays(1), 1, 59, 59), ny, loopDays = true))
        assertTrue(DaySchedule.shouldRollOver(d, ordinary, at(ordinary.plusDays(1), 2, 0), ny, loopDays = true))
        assertEquals(
            ordinary.plusDays(1),
            DaySchedule.effectiveDate(d, ordinary, at(ordinary.plusDays(1), 2, 0), ny, loopDays = true)
        )
    }

    @Test
    fun `effective date jumps to the current occurrence after a long gap`() {
        val d = day(7, 0, 60)
        val threeDaysLater = ordinary.plusDays(3)
        assertEquals(threeDaysLater, DaySchedule.effectiveDate(d, ordinary, at(threeDaysLater, 7, 30), ny, loopDays = true))
        assertEquals(ordinary, DaySchedule.effectiveDate(d, ordinary, at(threeDaysLater, 7, 30), ny, loopDays = false))
    }

    @Test
    fun `display state follows the occurrence`() {
        val d = day(7, 0, 45, 30)
        val occ = DaySchedule.occurrence(d, ordinary, ny)
        val before = DaySchedule.displayState(d, occ, at(ordinary, 6, 0))
        assertEquals(DaySchedule.Status.NOT_STARTED, before.status)
        assertEquals(-1, before.segmentIndex)
        assertEquals("(Day has not started yet)", before.segmentText)
        assertEquals(at(ordinary, 7, 0), before.nextEvent?.atMillis)

        val during = DaySchedule.displayState(d, occ, at(ordinary, 7, 10))
        assertEquals(DaySchedule.Status.ACTIVE, during.status)
        assertEquals(0, during.segmentIndex)
        assertEquals("Now: Seg 0", during.segmentText)
        assertEquals(600L, during.elapsedSeconds)
        assertEquals(35L * 60, during.remainingSeconds)
        assertEquals(at(ordinary, 7, 45), during.nextEvent?.atMillis)

        val atBoundary = DaySchedule.displayState(d, occ, at(ordinary, 7, 45))
        assertEquals(1, atBoundary.segmentIndex)

        val after = DaySchedule.displayState(d, occ, at(ordinary, 8, 15))
        assertEquals(DaySchedule.Status.COMPLETE, after.status)
        assertEquals(2, after.segmentIndex)
        assertEquals("(Day is over)", after.segmentText)
        assertNull(after.nextEvent)

        val empty = DaySchedule.displayState(day(7, 0), null, at(ordinary, 7, 0))
        assertEquals(DaySchedule.Status.EMPTY, empty.status)
        assertEquals(-2, empty.segmentIndex)
    }

    @Test
    fun `zero length segments last one second`() {
        val d = Day(id = "d", name = "T", startTimeSeconds = 0, segments = listOf(Day.Segment(id = "a", name = "A", durationSeconds = 0)))
        val occ = DaySchedule.occurrence(d, ordinary, ZoneOffset.UTC)!!
        assertEquals(1000L, occ.endMillis - occ.startMillis)
    }

    @Test
    fun `start seconds outside one day are normalized`() {
        assertEquals(86400 - 120, DaySchedule.normalizedStartSeconds(-120))
        assertEquals(60, DaySchedule.normalizedStartSeconds(86400 + 60))
    }
}
