package com.neversink.impermanence.service

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neversink.impermanence.domain.audio.BellOutput
import com.neversink.impermanence.domain.session.TimingSession
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** A bell output that records what would have rung. */
class RecordingBellOutput : BellOutput {
    val rings = mutableListOf<Bell>()
    /** Calls to [ringRemaining]: the bell, the first strike rung, and the delay before it. */
    val resumed = mutableListOf<Triple<Bell, Int, Long>>()
    var cancelCount = 0
    override fun ring(context: Context, bell: Bell) { rings += bell }
    override fun ringRemaining(context: Context, bell: Bell, fromStrike: Int, firstDelayMillis: Long) {
        resumed += Triple(bell, fromStrike, firstDelayMillis)
    }
    override fun cancelPending() { cancelCount++ }
}

class RecordingServiceController : SessionEngine.ServiceController {
    var ensureCount = 0
    var stopCount = 0
    override fun ensureRunning(context: Context) { ensureCount++ }
    override fun stop(context: Context) { stopCount++ }
}

/**
 * Drives the real engine, store, alarm scheduler and receivers under Robolectric with a fake clock,
 * covering the device scenarios: alarm delivery, duplicate triggers, process death, reboot, clock and
 * time zone changes, and leaving the screen.
 */
@RunWith(AndroidJUnit4::class)
class SessionEngineTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val ny = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 6, 10)
    private var now = 0L
    private lateinit var bells: RecordingBellOutput
    private lateinit var services: RecordingServiceController
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    private fun at(hour: Int, minute: Int, second: Int = 0, date: LocalDate = today, zone: ZoneId = ny): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute, second), zone).toInstant().toEpochMilli()

    private val day = Day(
        id = "day-1",
        name = "Retreat",
        startTimeSeconds = 7 * 3600,
        segments = listOf(
            Day.Segment(id = "a", name = "Sit", durationSeconds = 45 * 60),
            Day.Segment(id = "b", name = "Walk", durationSeconds = 30 * 60, customEndBell = Bell(0, 3))
        ),
        startBell = Bell(0, 2),
        defaultBell = Bell(0, 1)
    )

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        bells = RecordingBellOutput()
        services = RecordingServiceController()
        SessionEngine.bellOutput = bells
        SessionEngine.serviceController = services
        SessionEngine.clock = { now }
        SessionEngine.zone = { ny }
        SessionStore(context).clear()
        SessionStore(context).strikes = null
        RingLog(context).clear()
        SessionEngine.forgetProcessState()
        SessionEngine.initialize(context)
    }

    @After
    fun tearDown() {
        SessionEngine.end(context, SessionEngine.EndReason.LEFT_SCREEN)
    }

    private fun nextAlarmClockMillis(): Long? = alarmManager.nextAlarmClock?.triggerTime

    private fun scheduledAlarmCount(): Int = shadowOf(alarmManager).scheduledAlarms.size

    private fun fireAlarm() {
        BellAlarmReceiver().onReceive(context, Intent(BellAlarmScheduler.ACTION_BELL_ALARM))
    }

    @Test
    fun `starting a day arms an exact alarm clock for the first bell and starts the service`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        assertEquals(at(7, 0), nextAlarmClockMillis())
        assertEquals(at(7, 0), SessionEngine.nextWakeMillis.value)
        assertTrue(services.ensureCount > 0)
        assertTrue(bells.rings.isEmpty())
    }

    @Test
    fun `each alarm rings its bell once and arms the next`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        now = at(7, 0)
        fireAlarm()
        assertEquals(listOf(Bell(0, 2)), bells.rings)
        assertEquals(at(7, 45), nextAlarmClockMillis())
        // A second trigger at the same moment (service tick) must not ring again.
        SessionEngine.process(context, SessionEngine.Trigger.SERVICE_TICK, allowServiceStart = false)
        fireAlarm()
        assertEquals(1, bells.rings.size)
        now = at(7, 45)
        fireAlarm()
        now = at(8, 15)
        fireAlarm()
        assertEquals(listOf(Bell(0, 2), Bell(0, 1), Bell(0, 3)), bells.rings)
        assertNull(SessionEngine.nextWakeMillis.value)
        // The completed day stays on screen, but nothing more is scheduled.
        assertNotNull(SessionEngine.session.value)
        assertEquals(listOf("rang", "rang", "rang"), RingLog(context).entries().map { it.outcome })
    }

    @Test
    fun `process death resumes from storage and rings a just missed bell`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        // Process dies; a fresh process starts 20 s after the 7:00 bell (alarm restarted it).
        SessionEngine.initialize(context)
        now = at(7, 0, 20)
        fireAlarm()
        assertEquals(listOf(Bell(0, 2)), bells.rings)
        assertEquals(at(7, 45), nextAlarmClockMillis())
    }

    @Test
    fun `a bell more than a minute late is skipped, never rung late`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        now = at(7, 2)
        fireAlarm()
        assertTrue(bells.rings.isEmpty())
        assertEquals("skipped-late", RingLog(context).entries().last().outcome)
        assertEquals(at(7, 45), nextAlarmClockMillis())
    }

    @Test
    fun `reopening the same day resumes instead of restarting`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        val id = SessionEngine.session.value!!.sessionId
        now = at(7, 0)
        fireAlarm()
        now = at(7, 10)
        SessionEngine.startDay(context, day, loopDays = false)
        assertEquals(id, SessionEngine.session.value!!.sessionId)
        assertEquals(1, bells.rings.size)
    }

    @Test
    fun `leaving the screen cancels the alarm and pending chimes`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        assertEquals(1, scheduledAlarmCount())
        SessionEngine.end(context, SessionEngine.EndReason.LEFT_SCREEN)
        assertEquals(0, scheduledAlarmCount())
        assertNull(SessionEngine.session.value)
        assertNull(SessionStore(context).load())
        assertEquals(1, bells.cancelCount)
        assertTrue(services.stopCount > 0)
        // A stale alarm delivery after leaving rings nothing.
        now = at(7, 0)
        fireAlarm()
        assertTrue(bells.rings.isEmpty())
    }

    @Test
    fun `a stale screen cannot end a newer session`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        val old = SessionEngine.session.value!!.sessionId
        SessionEngine.startQuickSit(context, 60_000L, 0, 1, 1)
        SessionEngine.endIfCurrent(context, old, SessionEngine.EndReason.LEFT_SCREEN)
        assertTrue(SessionEngine.session.value is TimingSession.QuickSitSession)
    }

    @Test
    fun `boot re-arms the alarm without starting the service`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = true)
        // Reboot clears alarms and the process.
        BellAlarmScheduler.cancel(context)
        assertEquals(0, scheduledAlarmCount())
        SessionEngine.initialize(context)
        val before = services.ensureCount
        now = at(6, 30)
        SystemEventReceiver().onReceive(context, Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED))
        assertEquals(at(7, 0), nextAlarmClockMillis())
        assertEquals(before, services.ensureCount)
    }

    @Test
    fun `time zone change re-arms for the new local time`() {
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        val la = ZoneId.of("America/Los_Angeles")
        SessionEngine.zone = { la }
        SystemEventReceiver().onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))
        assertEquals(at(7, 0, zone = la), nextAlarmClockMillis())
    }

    @Test
    fun `loop days toggled on while complete arms tomorrow`() {
        now = at(9, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        assertNull(SessionEngine.nextWakeMillis.value)
        SessionEngine.setLoopDays(context, true)
        assertEquals(at(7, 0, date = today.plusDays(1)), nextAlarmClockMillis())
    }

    @Test
    fun `quick sit rings start chimes now and end chimes once at the end`() {
        now = at(10, 0)
        SessionEngine.startQuickSit(context, durationMillis = 15 * 60_000L, soundId = 0, startChimes = 2, endChimes = 4)
        assertEquals(listOf(Bell(0, 2)), bells.rings)
        assertEquals(at(10, 15), nextAlarmClockMillis())
        now = at(10, 15)
        fireAlarm()
        fireAlarm()
        assertEquals(listOf(Bell(0, 2), Bell(0, 4)), bells.rings)
        assertNull(SessionEngine.session.value)
        assertEquals(0, scheduledAlarmCount())
    }

    @Test
    fun `the service keeps the process alive until the last end strike of a quick sit`() {
        now = at(10, 0)
        SessionEngine.startQuickSit(context, 60_000L, soundId = 0, startChimes = 1, endChimes = 3)
        val stopsBefore = services.stopCount
        now = at(10, 1)
        fireAlarm()
        assertEquals(Bell(0, 3), bells.rings.last())
        // Strikes at +0, +3 and +6 s are still to come: stopping the foreground service now would leave
        // them to a background process that Android may kill at any moment.
        assertEquals(stopsBefore, services.stopCount)
        now = at(10, 1, 3)
        SessionEngine.process(context, SessionEngine.Trigger.SERVICE_TICK, allowServiceStart = false)
        assertEquals(stopsBefore, services.stopCount)
        // The last strike starts at +6 s; the service is held while it sounds, then stops.
        now = at(10, 1, 20)
        SessionEngine.process(context, SessionEngine.Trigger.SERVICE_TICK, allowServiceStart = false)
        assertEquals(stopsBefore, services.stopCount)
        now = at(10, 1, 22)
        SessionEngine.process(context, SessionEngine.Trigger.SERVICE_TICK, allowServiceStart = false)
        assertTrue(services.stopCount > stopsBefore)
        assertNull(SessionEngine.strikesUntilMillis.value)
        assertEquals(2, bells.rings.size)
        assertTrue("strikes posted by this process are never posted again", bells.resumed.isEmpty())
    }

    @Test
    fun `after process death between end strikes the strikes still to come ring once, on their beat`() {
        now = at(10, 0)
        SessionEngine.startQuickSit(context, 60_000L, soundId = 0, startChimes = 1, endChimes = 3)
        now = at(10, 1)
        fireAlarm()
        // The process dies 1 s after the first end strike; the sticky service restarts it 0.5 s later.
        SessionEngine.forgetProcessState()
        SessionEngine.initialize(context)
        now = at(10, 1, 1) + 500
        SessionEngine.process(context, SessionEngine.Trigger.SERVICE_RESTART, allowServiceStart = false)
        // Strike 0 already sounded and is not repeated; strikes 1 and 2 ring at +3 s and +6 s.
        assertEquals(listOf(Triple(Bell(0, 3), 1, 1_500L)), bells.resumed)
        assertNull(SessionEngine.session.value)
        assertNotNull(SessionEngine.strikesUntilMillis.value)
        // Later triggers in the new process don't post them again, and the end bell itself never repeats.
        now = at(10, 1, 4)
        SessionEngine.process(context, SessionEngine.Trigger.SERVICE_TICK, allowServiceStart = false)
        fireAlarm()
        assertEquals(1, bells.resumed.size)
        assertEquals(2, bells.rings.size)
    }

    @Test
    fun `a restart after the strikes are over rings nothing`() {
        now = at(10, 0)
        SessionEngine.startQuickSit(context, 60_000L, soundId = 0, startChimes = 1, endChimes = 3)
        now = at(10, 1)
        fireAlarm()
        SessionEngine.forgetProcessState()
        SessionEngine.initialize(context)
        now = at(10, 1, 7)
        SessionEngine.process(context, SessionEngine.Trigger.SERVICE_RESTART, allowServiceStart = false)
        assertTrue("strike 2 at +6 s may have sounded, so it is not repeated", bells.resumed.isEmpty())
        assertEquals(2, bells.rings.size)
    }

    @Test
    fun `ending timing during the strikes cancels the rest and lets the service stop`() {
        now = at(10, 0)
        SessionEngine.startQuickSit(context, 60_000L, soundId = 0, startChimes = 1, endChimes = 3)
        now = at(10, 1)
        fireAlarm()
        val cancels = bells.cancelCount
        SessionEngine.end(context, SessionEngine.EndReason.TASK_REMOVED)
        assertTrue(bells.cancelCount > cancels)
        assertNull(SessionEngine.strikesUntilMillis.value)
        SessionEngine.forgetProcessState()
        SessionEngine.initialize(context)
        SessionEngine.process(context, SessionEngine.Trigger.SERVICE_RESTART, allowServiceStart = false)
        assertTrue(bells.resumed.isEmpty())
    }

    @Test
    fun `cancelling a quick sit means no end bell`() {
        now = at(10, 0)
        SessionEngine.startQuickSit(context, 60_000L, 0, 1, 1)
        SessionEngine.end(context, SessionEngine.EndReason.QUICK_SIT_CANCELLED)
        now = at(10, 1)
        fireAlarm()
        assertEquals(1, bells.rings.size)
    }

    @Test
    fun `chime counts are clamped`() {
        now = at(10, 0)
        SessionEngine.startQuickSit(context, 60_000L, soundId = 42, startChimes = 99, endChimes = 0)
        assertEquals(Bell(0, 12), bells.rings.single())
        val session = SessionEngine.session.value as TimingSession.QuickSitSession
        assertEquals(Bell(0, 1), session.endBell)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `without exact alarm permission it still schedules and reports inexact mode`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        now = at(6, 0)
        SessionEngine.startDay(context, day, loopDays = false)
        assertEquals(BellAlarmScheduler.Mode.INEXACT, SessionEngine.alarmMode.value)
        assertEquals(1, scheduledAlarmCount())
    }

    @Test
    fun `corrupt stored session is discarded safely`() {
        SessionStore.storageContext(context).getSharedPreferences("timing_session", Context.MODE_PRIVATE)
            .edit().putString("session", "{not json").commit()
        assertNull(SessionStore(context).load())
        SessionEngine.process(context, SessionEngine.Trigger.APP_START, allowServiceStart = false)
        assertNull(SessionEngine.session.value)
    }
}
