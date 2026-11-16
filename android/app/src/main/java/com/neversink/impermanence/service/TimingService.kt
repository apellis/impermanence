package com.neversink.impermanence.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.neversink.impermanence.MainActivity
import com.neversink.impermanence.R
import com.neversink.impermanence.domain.session.TimingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Foreground service that runs while a session has a bell pending. It keeps the process at foreground
 * priority, shows the ongoing notification (with Stop), and runs an in-process timer as a second trigger
 * next to the exact alarm. While a session runs it also holds a partial wake lock (renewed on each
 * tick with a timeout, so it can never leak), so the ticker keeps running with the screen off even on
 * devices whose maker delays alarms. Alex chose reliability over battery for long sits and retreat days.
 */
class TimingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private var notificationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            SessionEngine.end(this, SessionEngine.EndReason.NOTIFICATION_STOP)
            stopSelfSafely()
            return START_NOT_STICKY
        }
        if (SessionEngine.session.value == null) SessionEngine.initialize(this)
        val session = SessionEngine.session.value
        if (session == null) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(this, session, SessionEngine.nextWakeMillis.value),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            )
        }.onFailure { Log.e(LOG_TAG, "startForeground failed", it) }.isSuccess
        if (!started) {
            // The alarm path still rings bells without the service.
            stopSelfSafely()
            return START_NOT_STICKY
        }
        if (intent == null) {
            // Sticky restart after the process was killed: resync, which rings a just-missed bell.
            SessionEngine.process(this, SessionEngine.Trigger.SERVICE_RESTART, allowServiceStart = false)
        }
        startTicker()
        observeForNotification()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away is leaving the timing screen.
        SessionEngine.end(this, SessionEngine.EndReason.TASK_REMOVED)
        stopSelfSafely()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        val context = applicationContext
        tickerJob = scope.launch {
            while (isActive) {
                val next = SessionEngine.nextWakeMillis.value
                if (SessionEngine.session.value == null) break
                val wait = if (next == null) MAX_TICK_MILLIS else (next - SessionEngine.now()).coerceIn(MIN_TICK_MILLIS, MAX_TICK_MILLIS)
                holdWakeLock(wait + WAKE_LOCK_MARGIN_MILLIS)
                delay(wait)
                runCatching {
                    SessionEngine.process(context, SessionEngine.Trigger.SERVICE_TICK, allowServiceStart = false)
                }.onFailure { Log.e(LOG_TAG, "Tick failed", it) }
                if (SessionEngine.session.value != null && SessionEngine.nextWakeMillis.value == null) {
                    // No-loop day is complete: nothing left to ring. The screen still shows the session.
                    stopSelfSafely()
                    break
                }
            }
        }
    }

    private fun observeForNotification() {
        if (notificationJob?.isActive == true) return
        notificationJob = scope.launch {
            SessionEngine.session.combine(SessionEngine.nextWakeMillis) { session, next -> session to next }
                .collect { (session, next) ->
                    if (session == null) {
                        stopSelfSafely()
                    } else {
                        runCatching {
                            getSystemService(NotificationManager::class.java)
                                ?.notify(NOTIFICATION_ID, buildNotification(this@TimingService, session, next))
                        }
                    }
                }
        }
    }

    /** Keeps the CPU awake until the next tick. Not reference counted, so each call just moves the timeout. */
    private fun holdWakeLock(timeoutMillis: Long) {
        runCatching {
            val lock = wakeLock ?: getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Impermanence:timing")
                ?.also { it.setReferenceCounted(false); wakeLock = it }
            lock?.acquire(timeoutMillis)
        }.onFailure { Log.w(LOG_TAG, "Wake lock unavailable; the alarm still rings bells", it) }
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    private fun stopSelfSafely() {
        releaseWakeLock()
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    object Controller : SessionEngine.ServiceController {
        override fun ensureRunning(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, TimingService::class.java))
            }.onFailure { Log.w(LOG_TAG, "Could not start timing service; alarms still ring bells", it) }
        }

        override fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, TimingService::class.java)) }
        }
    }

    companion object {
        private const val LOG_TAG = "TimingService"
        const val ACTION_STOP = "com.neversink.impermanence.action.STOP_TIMING"
        const val CHANNEL_ID = "impermanence_timing"
        const val NOTIFICATION_ID = 1001
        private const val MIN_TICK_MILLIS = 50L
        private const val MAX_TICK_MILLIS = 30_000L
        private const val WAKE_LOCK_MARGIN_MILLIS = 30_000L

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        fun buildNotification(context: Context, session: TimingSession, nextWakeMillis: Long?): Notification {
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val stop = PendingIntent.getService(
                context,
                1,
                Intent(context, TimingService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val title = when (session) {
                is TimingSession.DaySession -> session.day.name
                is TimingSession.QuickSitSession -> context.getString(R.string.quick_sit_title)
            }
            val text = nextWakeMillis?.let {
                val time = DateFormat.getTimeFormat(context).format(Date(it))
                when (session) {
                    is TimingSession.DaySession -> context.getString(R.string.notification_next_bell, time)
                    is TimingSession.QuickSitSession -> context.getString(R.string.notification_quick_sit_ends, time)
                }
            } ?: context.getString(R.string.notification_no_more_bells)
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openApp)
                .addAction(0, context.getString(R.string.notification_stop), stop)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        }
    }
}
