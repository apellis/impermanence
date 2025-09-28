package com.impermanence.impermanence.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.impermanence.impermanence.MainActivity
import com.impermanence.impermanence.R
import com.impermanence.impermanence.domain.audio.BellPlayer
import com.impermanence.impermanence.domain.timer.DayTimerCoordinator
import com.impermanence.impermanence.domain.timer.DayTimerEngine
import com.impermanence.impermanence.model.Day
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DayTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob = serviceScope.launch { }
    private var engine: DayTimerEngine? = null
    private var currentDay: Day? = null
    private var loopDays: Boolean = true
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val dayJson = intent?.getStringExtra(EXTRA_DAY_JSON)
        val requestedLoopDays = intent?.getBooleanExtra(EXTRA_LOOP_DAYS, true) ?: true
        if (dayJson == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val day = runCatching { Json.decodeFromString<Day>(dayJson) }.getOrNull()
        if (day == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!wakeLock.isHeld) {
            wakeLock.acquire()
        }

        startForeground(NOTIFICATION_ID, buildNotification(day.name))

        if (engine == null || currentDay?.id != day.id) {
            engine = DayTimerEngine(day, requestedLoopDays)
            currentDay = day
        }
        loopDays = requestedLoopDays
        engine?.updateLoopDays(loopDays)
        startLoop()

        return START_STICKY
    }

    override fun onDestroy() {
        timerJob.cancel()
        serviceScope.cancel()
        DayTimerCoordinator.clear()
        BellPlayer.stopAll()
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLoop() {
        timerJob.cancel()
        val activeEngine = engine ?: return
        val context = applicationContext
        timerJob = serviceScope.launch {
            while (isActive) {
                val evaluation = activeEngine.evaluate()
                DayTimerCoordinator.update(evaluation.state)
                evaluation.bell?.let { bell ->
                    BellPlayer.play(context, bell)
                }
                delay(500L)
            }
        }
    }

    private fun buildNotification(dayName: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_running_day, dayName))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val EXTRA_DAY_JSON = "extra_day_json"
        private const val EXTRA_LOOP_DAYS = "extra_loop_days"
        private const val CHANNEL_ID = "impermanence_timer"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TAG = "Impermanence:DayTimer"

        fun start(context: Context, day: Day, loopDays: Boolean) {
            val intent = Intent(context, DayTimerService::class.java).apply {
                putExtra(EXTRA_DAY_JSON, Json.encodeToString(day))
                putExtra(EXTRA_LOOP_DAYS, loopDays)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DayTimerService::class.java))
        }
    }
}
