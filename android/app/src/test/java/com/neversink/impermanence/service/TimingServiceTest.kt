package com.neversink.impermanence.service

import android.app.Application
import android.app.Notification
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neversink.impermanence.domain.session.TimingSession
import com.neversink.impermanence.model.Bell
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class TimingServiceTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private var now = 1_780_000_000_000L
    private lateinit var bells: RecordingBellOutput

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        bells = RecordingBellOutput()
        SessionEngine.bellOutput = bells
        SessionEngine.serviceController = RecordingServiceController()
        SessionEngine.clock = { now }
        SessionEngine.zone = { ZoneId.of("UTC") }
        SessionStore(context).clear()
        SessionEngine.initialize(context)
    }

    @After
    fun tearDown() {
        SessionEngine.end(context, SessionEngine.EndReason.LEFT_SCREEN)
    }

    @Test
    fun `starting the service shows an ongoing notification with a stop action`() {
        SessionEngine.startQuickSit(context, 60_000L, 0, 1, 1)
        val controller = Robolectric.buildService(TimingService::class.java, Intent(context, TimingService::class.java))
        val service = controller.create().startCommand(0, 1).get()
        val notification = shadowOf(service).lastForegroundNotification
        assertNotNull(notification)
        assertTrue(notification!!.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(1, notification.actions.size)
        assertEquals("Stop", notification.actions[0].title.toString())
        controller.destroy()
    }

    @Test
    fun `the stop action ends the session`() {
        SessionEngine.startQuickSit(context, 60_000L, 0, 1, 1)
        val stopIntent = Intent(context, TimingService::class.java).setAction(TimingService.ACTION_STOP)
        Robolectric.buildService(TimingService::class.java, stopIntent).create().startCommand(0, 1)
        assertNull(SessionEngine.session.value)
        assertNull(SessionStore(context).load())
        assertEquals("session/end:NOTIFICATION_STOP", RingLog(context).entries().last().key)
    }

    @Test
    fun `swiping the app away ends the session`() {
        SessionEngine.startQuickSit(context, 60_000L, 0, 1, 1)
        val controller = Robolectric.buildService(TimingService::class.java, Intent(context, TimingService::class.java))
        val service = controller.create().startCommand(0, 1).get()
        service.onTaskRemoved(Intent())
        assertNull(SessionEngine.session.value)
        now += 60_000L
        BellAlarmReceiver().onReceive(context, Intent(BellAlarmScheduler.ACTION_BELL_ALARM))
        assertEquals(1, bells.rings.size)
        controller.destroy()
    }

    @Test
    fun `with no session the service stops instead of showing a notification`() {
        val controller = Robolectric.buildService(TimingService::class.java, Intent(context, TimingService::class.java))
        val service = controller.create().startCommand(0, 1).get()
        assertNull(shadowOf(service).lastForegroundNotification)
        assertTrue(shadowOf(service).isStoppedBySelf)
        controller.destroy()
    }

    @Test
    fun `notification shows a live countdown on the lock screen, or says there are no more bells`() {
        val session = TimingSession.QuickSitSession("s", now, now + 60_000L, Bell(0, 1), now)
        val withNext = TimingService.buildNotification(context, session, now + 60_000L)
        assertEquals("Quick Sit", withNext.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("Time remaining", withNext.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(now + 60_000L, withNext.`when`)
        assertTrue(withNext.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(withNext.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(Notification.VISIBILITY_PUBLIC, withNext.visibility)
        val none = TimingService.buildNotification(context, session, null)
        assertEquals("No more bells today", none.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }
}
