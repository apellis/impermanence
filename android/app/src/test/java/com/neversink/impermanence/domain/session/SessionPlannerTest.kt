package com.neversink.impermanence.domain.session

import com.neversink.impermanence.domain.timer.DaySchedule
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Exercises the bell decision logic the way the device drives it: a sequence of process() calls at
 * arbitrary moments (alarm, ticks, restarts), checking that each bell rings exactly once, on time, and
 * never when it shouldn't.
 */
class SessionPlannerTest {
    private val ny = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 6, 10)

    private fun at(date: LocalDate, hour: Int, minute: Int, second: Int = 0): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute, second), ny).toInstant().toEpochMilli()

    private fun day(startHour: Int, startMinute: Int, vararg minutes: Int) = Day(
        id = "day-1",
        name = "Retreat",
        startTimeSeconds = startHour * 3600 + startMinute * 60,
        segments = minutes.mapIndexed { i, m ->
            Day.Segment(id = "s$i", name = "Seg $i", durationSeconds = m * 60, customEndBell = Bell(0, i + 2))
        },
        startBell = Bell(0, 1),
        defaultBell = Bell(0, 1)
    )

    private fun open(day: Day, nowMillis: Long, loop: Boolean): TimingSession.DaySession =
        TimingSession.DaySession(
            sessionId = "s",
            day = day,
            loopDays = loop,
            anchorDate = DaySchedule.initialDate(day, nowMillis, ny).toString(),
            handledThroughMillis = nowMillis - 1
        )

    /** Drives the planner like the device: process at each given instant, collecting rings. */
    private class Driver(var session: TimingSession?, private val zone: ZoneId) {
        val rings = mutableListOf<Pair<Long, SessionPlanner.Ring>>()
        var lastNext: Long? = null

        fun processAt(now: Long): SessionPlanner.Result {
            val result = SessionPlanner.process(session!!, now, zone)
            result.ring?.let { rings += now to it }
            session = result.session
            lastNext = result.nextWakeMillis
            return result
        }

        /** Follows the alarms: process exactly at each next wake, up to [untilMillis]. */
        fun followAlarms(untilMillis: Long, lateByMillis: Long = 0) {
            var guard = 0
            while (session != null && guard++ < 10_000) {
                val next = lastNext ?: break
                if (next > untilMillis) break
                processAt(next + lateByMillis)
            }
        }
    }

    @Test
    fun `opening before the day rings every bell once on time`() {
        val d = day(7, 0, 45, 30)
        val driver = Driver(open(d, at(today, 6, 0), loop = false), ny)
        driver.processAt(at(today, 6, 0))
        assertEquals(at(today, 7, 0), driver.lastNext)
        driver.followAlarms(untilMillis = at(today, 23, 0))
        assertEquals(
            listOf("2026-06-10/start", "2026-06-10/end-0", "2026-06-10/end-1"),
            driver.rings.map { it.second.key }
        )
        assertEquals(listOf(Bell(0, 1), Bell(0, 2), Bell(0, 3)), driver.rings.map { it.second.bell })
        driver.rings.forEach { (now, ring) -> assertEquals(ring.scheduledAtMillis, now) }
        assertNull(driver.lastNext)
    }

    @Test
    fun `duplicate triggers never ring a bell twice`() {
        val d = day(7, 0, 45)
        val driver = Driver(open(d, at(today, 6, 0), loop = false), ny)
        driver.processAt(at(today, 6, 0))
        driver.processAt(at(today, 7, 0)) // alarm
        driver.processAt(at(today, 7, 0)) // service tick at the same moment
        driver.processAt(at(today, 7, 0, 5)) // restart a few seconds later
        assertEquals(1, driver.rings.size)
    }

    @Test
    fun `opening mid day rings nothing for past events including the start bell`() {
        val d = day(7, 0, 45, 30)
        val driver = Driver(open(d, at(today, 7, 10), loop = false), ny)
        val first = driver.processAt(at(today, 7, 10))
        assertNull(first.ring)
        assertEquals(at(today, 7, 45), first.nextWakeMillis)
        driver.followAlarms(at(today, 23, 0))
        assertEquals(listOf("2026-06-10/end-0", "2026-06-10/end-1"), driver.rings.map { it.second.key })
    }

    @Test
    fun `opening exactly at a bell instant rings it but nothing earlier`() {
        val d = day(7, 0, 45)
        val driver = Driver(open(d, at(today, 7, 0), loop = false), ny)
        assertEquals("2026-06-10/start", driver.processAt(at(today, 7, 0)).ring?.key)
        assertEquals(at(today, 7, 45), driver.lastNext)
        val late = Driver(open(d, at(today, 7, 0, 1), loop = false), ny)
        assertNull(late.processAt(at(today, 7, 0, 1)).ring)
    }

    @Test
    fun `a bell up to 60 seconds late still rings`() {
        val d = day(7, 0, 45)
        val driver = Driver(open(d, at(today, 6, 0), loop = false), ny)
        driver.processAt(at(today, 6, 0))
        val result = driver.processAt(at(today, 7, 1, 0))
        assertEquals("2026-06-10/start", result.ring?.key)
    }

    @Test
    fun `a bell more than 60 seconds late is skipped and the next one still rings`() {
        val d = day(7, 0, 45)
        val driver = Driver(open(d, at(today, 6, 0), loop = false), ny)
        driver.processAt(at(today, 6, 0))
        val late = driver.processAt(at(today, 7, 1, 1))
        assertNull(late.ring)
        assertEquals(listOf("2026-06-10/start"), late.skippedKeys)
        assertEquals(at(today, 7, 45), late.nextWakeMillis)
        val next = driver.processAt(at(today, 7, 45))
        assertEquals("2026-06-10/end-0", next.ring?.key)
    }

    @Test
    fun `after a long outage only the most recent bell can ring`() {
        val d = day(7, 0, 10, 10, 10)
        val driver = Driver(open(d, at(today, 6, 0), loop = false), ny)
        driver.processAt(at(today, 6, 0))
        // Process was dead from 6:00 until 7:30:30, 30 s after the last bell at 7:30.
        val result = driver.processAt(at(today, 7, 30, 30))
        assertEquals("2026-06-10/end-2", result.ring?.key)
        assertEquals(listOf("2026-06-10/start", "2026-06-10/end-0", "2026-06-10/end-1"), result.skippedKeys)
    }

    @Test
    fun `no loop day completes and stops scheduling`() {
        val d = day(7, 0, 45)
        val driver = Driver(open(d, at(today, 6, 0), loop = false), ny)
        driver.processAt(at(today, 6, 0))
        driver.followAlarms(at(today.plusDays(3), 0, 0))
        assertEquals(2, driver.rings.size)
        assertNull(driver.lastNext)
        assertNotNull(driver.session) // the screen still shows the completed day
        assertNull(driver.processAt(at(today.plusDays(1), 7, 0)).ring) // never rings again
    }

    @Test
    fun `loop days rings the next day and the day after`() {
        val d = day(7, 0, 45)
        val driver = Driver(open(d, at(today, 6, 0), loop = true), ny)
        driver.processAt(at(today, 6, 0))
        driver.followAlarms(at(today.plusDays(2), 23, 0))
        assertEquals(
            listOf(
                "2026-06-10/start", "2026-06-10/end-0",
                "2026-06-11/start", "2026-06-11/end-0",
                "2026-06-12/start", "2026-06-12/end-0"
            ),
            driver.rings.map { it.second.key }
        )
        driver.rings.forEach { (now, ring) -> assertEquals(ring.scheduledAtMillis, now) }
    }

    @Test
    fun `loop days keeps a day crossing midnight running and rings all its bells`() {
        val d = day(22, 0, 120, 120) // 22:00-00:00, 00:00-02:00
        val driver = Driver(open(d, at(today, 21, 0), loop = true), ny)
        driver.processAt(at(today, 21, 0))
        driver.followAlarms(at(today.plusDays(1), 23, 59))
        assertEquals(
            listOf("2026-06-10/start", "2026-06-10/end-0", "2026-06-10/end-1", "2026-06-11/start"),
            driver.rings.map { it.second.key }
        )
        assertEquals(at(today.plusDays(1), 0, 0), driver.rings[1].first)
        assertEquals(at(today.plusDays(1), 2, 0), driver.rings[2].first)
    }

    @Test
    fun `day ending exactly at midnight rings its final bell`() {
        val d = day(23, 0, 60) // ends 00:00
        for (loop in listOf(true, false)) {
            val driver = Driver(open(d, at(today, 22, 0), loop = loop), ny)
            driver.processAt(at(today, 22, 0))
            driver.followAlarms(at(today.plusDays(1), 0, 30))
            assertEquals("loop=$loop", listOf("2026-06-10/start", "2026-06-10/end-0"), driver.rings.map { it.second.key })
            assertEquals(at(today.plusDays(1), 0, 0), driver.rings.last().first)
        }
    }

    @Test
    fun `opening after midnight during a day crossing midnight continues it`() {
        val d = day(22, 0, 240)
        val driver = Driver(open(d, at(today, 1, 0), loop = false), ny)
        val first = driver.processAt(at(today, 1, 0))
        assertNull(first.ring)
        assertEquals(at(today, 2, 0), first.nextWakeMillis)
        assertEquals("2026-06-09/end-0", driver.processAt(at(today, 2, 0)).ring?.key)
    }

    @Test
    fun `loop day after a long outage resumes the current occurrence without backfill`() {
        val d = day(7, 0, 60, 60)
        val driver = Driver(open(d, at(today, 6, 0), loop = true), ny)
        driver.processAt(at(today, 6, 0))
        // Asleep with no alarms for two days; wakes at 8:00:20 on day 3, 20 s after a bell.
        val result = driver.processAt(at(today.plusDays(2), 8, 0, 20))
        assertEquals("2026-06-12/end-0", result.ring?.key)
        assertEquals(at(today.plusDays(2), 9, 0), result.nextWakeMillis)
    }

    @Test
    fun `bells follow wall clock across spring forward`() {
        val sf = LocalDate.of(2026, 3, 8)
        val d = day(7, 0, 30)
        val driver = Driver(open(d, at(sf.minusDays(1), 6, 0), loop = true), ny)
        driver.processAt(at(sf.minusDays(1), 6, 0))
        driver.followAlarms(at(sf, 23, 0))
        assertEquals(4, driver.rings.size)
        assertEquals(at(sf, 7, 0), driver.rings[2].first)
        assertEquals(at(sf, 7, 30), driver.rings[3].first)
    }

    @Test
    fun `a segment spanning fall back lasts its real duration`() {
        val fb = LocalDate.of(2026, 11, 1)
        val d = day(0, 30, 120) // 00:30 EDT + 2h = 01:30 EST
        val driver = Driver(open(d, at(fb, 0, 0), loop = false), ny)
        driver.processAt(at(fb, 0, 0))
        driver.followAlarms(at(fb, 23, 0))
        assertEquals(2 * 3600_000L, driver.rings[1].first - driver.rings[0].first)
    }

    @Test
    fun `time zone change recomputes without double ringing`() {
        val d = day(7, 0, 60)
        var session: TimingSession = open(d, at(today, 6, 0), loop = false)
        session = SessionPlanner.process(session, at(today, 6, 0), ny).session!!
        val start = SessionPlanner.process(session, at(today, 7, 0), ny)
        assertEquals("2026-06-10/start", start.ring?.key)
        session = start.session!!
        // Travel west by three hours: in Los Angeles the day's 7:00 is three hours later.
        val la = ZoneId.of("America/Los_Angeles")
        val moved = SessionPlanner.process(session, at(today, 7, 30), la)
        assertNull(moved.ring)
        assertEquals(ZonedDateTime.of(today, LocalTime.of(7, 0), la).toInstant().toEpochMilli(), moved.nextWakeMillis)
    }

    @Test
    fun `clock set backwards does not ring handled bells again`() {
        val d = day(7, 0, 60)
        var session: TimingSession = open(d, at(today, 6, 0), loop = false)
        session = SessionPlanner.process(session, at(today, 7, 0), ny).session!!
        val back = SessionPlanner.process(session, at(today, 6, 30), ny)
        assertNull(back.ring)
        val again = SessionPlanner.process(back.session!!, at(today, 7, 0), ny)
        assertNull(again.ring)
        assertEquals(at(today, 8, 0), again.nextWakeMillis)
    }

    @Test
    fun `toggling loop on after completion schedules the next day`() {
        val d = day(7, 0, 30)
        var session = open(d, at(today, 6, 0), loop = false)
        session = SessionPlanner.process(session, at(today, 7, 30), ny).session as TimingSession.DaySession
        assertNull(SessionPlanner.process(session, at(today, 9, 0), ny).nextWakeMillis)
        val looping = SessionPlanner.process(session.copy(loopDays = true), at(today, 9, 0), ny)
        assertEquals(at(today.plusDays(1), 7, 0), looping.nextWakeMillis)
    }

    @Test
    fun `quick sit rings its end once then finishes`() {
        val start = at(today, 10, 0)
        val qs = TimingSession.QuickSitSession("q", start, start + 15 * 60_000L, Bell(0, 3), start)
        val running = SessionPlanner.process(qs, start + 60_000L, ny)
        assertNull(running.ring)
        assertEquals(start + 15 * 60_000L, running.nextWakeMillis)
        val end = SessionPlanner.process(running.session!!, start + 15 * 60_000L, ny)
        assertEquals("quick-sit/end", end.ring?.key)
        assertEquals(Bell(0, 3), end.ring?.bell)
        assertNull(end.session)
    }

    @Test
    fun `quick sit end more than 60 seconds late is skipped`() {
        val start = at(today, 10, 0)
        val qs = TimingSession.QuickSitSession("q", start, start + 60_000L, Bell(0, 1), start)
        val result = SessionPlanner.process(qs, start + 60_000L + 61_000L, ny)
        assertNull(result.ring)
        assertNull(result.session)
        assertTrue(result.skippedKeys.contains("quick-sit/end"))
    }

    @Test
    fun `quick sit is unaffected by time zone changes`() {
        val start = at(today, 10, 0)
        val qs = TimingSession.QuickSitSession("q", start, start + 60_000L, Bell(0, 1), start)
        val la = ZoneId.of("America/Los_Angeles")
        assertEquals(start + 60_000L, SessionPlanner.process(qs, start + 1_000L, la).nextWakeMillis)
    }
}
