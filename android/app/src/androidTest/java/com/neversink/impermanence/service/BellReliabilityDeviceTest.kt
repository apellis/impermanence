package com.neversink.impermanence.service

import android.content.Context
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neversink.impermanence.domain.audio.BellOutput
import com.neversink.impermanence.domain.audio.BellPlayer
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Runs the real alarm path on an emulator with the screen off and the device forced into Doze. The
 * foreground service is replaced by a no-op so only the alarm can make bells ring: if these pass, the
 * service's ticker is a second line of defence, not the thing bells depend on.
 */
@RunWith(AndroidJUnit4::class)
class BellReliabilityDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val rings = mutableListOf<Pair<Long, Bell>>()

    private val recordingOutput = object : BellOutput {
        override fun ring(context: Context, bell: Bell) {
            synchronized(rings) { rings += System.currentTimeMillis() to bell }
        }
        override fun cancelPending() {}
    }

    private val noService = object : SessionEngine.ServiceController {
        override fun ensureRunning(context: Context) {}
        override fun stop(context: Context) {}
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).close()
    }

    @Before
    fun setUp() {
        SessionEngine.end(context, SessionEngine.EndReason.LEFT_SCREEN)
        RingLog(context).clear()
        SessionEngine.bellOutput = recordingOutput
        SessionEngine.serviceController = noService
        shell("dumpsys battery unplug")
        shell("input keyevent KEYCODE_SLEEP")
        shell("dumpsys deviceidle force-idle")
    }

    @After
    fun tearDown() {
        SessionEngine.end(context, SessionEngine.EndReason.LEFT_SCREEN)
        SessionEngine.bellOutput = BellPlayer
        SessionEngine.serviceController = TimingService.Controller
        shell("dumpsys deviceidle unforce")
        shell("dumpsys battery reset")
        shell("input keyevent KEYCODE_WAKEUP")
    }

    private fun waitFor(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            Thread.sleep(200)
        }
        return condition()
    }

    @Test
    fun quickSitEndBellRingsOnTimeInDozeWithScreenOff() {
        SessionEngine.startQuickSit(context, durationMillis = 20_000L, soundId = 0, startChimes = 1, endChimes = 2)
        val endAt = System.currentTimeMillis() + 20_000L
        assertTrue("end bell never rang", waitFor(60_000L) { synchronized(rings) { rings.size >= 2 } })
        Thread.sleep(5_000L)
        synchronized(rings) {
            assertEquals("start and end bell exactly once each", 2, rings.size)
            val lateness = rings[1].first - endAt
            assertTrue("end bell was ${lateness} ms off", lateness in -1_000L..5_000L)
            assertEquals(Bell(0, 2), rings[1].second)
        }
    }

    @Test
    fun dayBellsRingOnTimeInDozeWithScreenOff() {
        val zone = ZoneId.systemDefault()
        // Start 20 s from now, on a whole second, with two 15 s segments.
        val start = ZonedDateTime.now(zone).withNano(0).plusSeconds(20)
        val startSeconds = start.toLocalTime().toSecondOfDay()
        // Skip the rare run that would straddle midnight; the JVM tests cover that case.
        org.junit.Assume.assumeTrue(startSeconds in 60..(86_400 - 120))
        val day = Day(
            id = "device-test-day",
            name = "Device test",
            startTimeSeconds = startSeconds,
            segments = listOf(
                Day.Segment(id = "a", name = "A", durationSeconds = 15),
                Day.Segment(id = "b", name = "B", durationSeconds = 15)
            ),
            startBell = Bell(0, 1),
            defaultBell = Bell(0, 1)
        )
        SessionEngine.startDay(context, day, loopDays = false)
        val startMillis = start.toInstant().toEpochMilli()
        val expected = listOf(startMillis, startMillis + 15_000L, startMillis + 30_000L)
        assertTrue("not all day bells rang", waitFor(90_000L) { synchronized(rings) { rings.size >= 3 } })
        Thread.sleep(5_000L)
        synchronized(rings) {
            assertEquals("each bell exactly once", 3, rings.size)
            rings.map { it.first }.zip(expected).forEach { (actual, want) ->
                assertTrue("bell was ${actual - want} ms off", actual - want in -1_000L..5_000L)
            }
        }
        assertEquals(listOf("rang", "rang", "rang"), RingLog(context).entries().map { it.outcome })
    }

    @Test
    fun leavingStopsBellsEvenInDoze() {
        SessionEngine.startQuickSit(context, durationMillis = 10_000L, soundId = 0, startChimes = 1, endChimes = 1)
        SessionEngine.end(context, SessionEngine.EndReason.QUICK_SIT_CANCELLED)
        Thread.sleep(20_000L)
        synchronized(rings) { assertEquals("only the start bell", 1, rings.size) }
    }
}
