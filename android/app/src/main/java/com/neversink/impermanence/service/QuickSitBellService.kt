package com.neversink.impermanence.service

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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.neversink.impermanence.MainActivity
import com.neversink.impermanence.R
import com.neversink.impermanence.domain.audio.BellPlayer
import com.neversink.impermanence.model.Bell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOG_TAG = "QuickSitBellService"

class QuickSitBellService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stopJob = serviceScope.launch { }
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
        val soundId = intent?.getIntExtra(EXTRA_SOUND_ID, 0) ?: 0
        val chimes = intent?.getIntExtra(EXTRA_CHIMES, 1)?.coerceIn(1, 12) ?: 1

        if (!startForegroundSafely()) {
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock(timeoutMillis = estimatePlaybackWindowMillis(chimes))
        BellPlayer.play(applicationContext, Bell(soundId = soundId, numRings = chimes))

        stopJob.cancel()
        stopJob = serviceScope.launch {
            delay(estimatePlaybackWindowMillis(chimes))
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopJob.cancel()
        serviceScope.cancel()
        releaseWakeLockIfHeld()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun estimatePlaybackWindowMillis(chimes: Int): Long {
        return (chimes.coerceIn(1, 12) * 6_000L) + 4_000L
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.quick_sit_notification_playing))
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.quick_sit_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundSafely(): Boolean {
        return runCatching { startForeground(NOTIFICATION_ID, buildNotification()) }
            .onFailure { throwable ->
                Log.e(LOG_TAG, "Unable to start quick sit bell foreground service", throwable)
            }
            .isSuccess
    }

    private fun acquireWakeLock(timeoutMillis: Long) {
        if (wakeLock.isHeld) return
        runCatching {
            wakeLock.acquire(timeoutMillis.coerceAtLeast(5_000L))
        }.onFailure { throwable ->
            Log.e(LOG_TAG, "Unable to acquire quick sit wake lock", throwable)
        }
    }

    private fun releaseWakeLockIfHeld() {
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            runCatching { wakeLock.release() }
                .onFailure { throwable ->
                    Log.e(LOG_TAG, "Unable to release quick sit wake lock", throwable)
                }
        }
    }

    companion object {
        private const val CHANNEL_ID = "impermanence_quick_sit_bell"
        private const val NOTIFICATION_ID = 1002
        private const val WAKE_LOCK_TAG = "Impermanence:QuickSitBell"

        fun start(context: Context, soundId: Int, chimes: Int) {
            val intent = Intent(context, QuickSitBellService::class.java).apply {
                putExtra(EXTRA_SOUND_ID, soundId)
                putExtra(EXTRA_CHIMES, chimes.coerceIn(1, 12))
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { throwable ->
                    Log.e(LOG_TAG, "Unable to start quick sit bell service", throwable)
                }
        }
    }
}
