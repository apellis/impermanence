package com.neversink.impermanence.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.neversink.impermanence.model.BellCatalog

private const val LOG_TAG = "QuickSitAlarmScheduler"

object QuickSitAlarmScheduler {
    private const val REQUEST_CODE = 19017
    private const val PREFS_NAME = "quick_sit_alarm"
    private const val KEY_END_ELAPSED_REALTIME = "end_elapsed_realtime"

    fun schedule(
        context: Context,
        endElapsedRealtime: Long,
        soundId: Int,
        chimes: Int
    ): Boolean {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return false
        val triggerAt = endElapsedRealtime.coerceAtLeast(SystemClock.elapsedRealtime() + 250L)
        val pendingIntent = pendingIntent(appContext, soundId, chimes)
        alarmManager.cancel(pendingIntent)

        val scheduled = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
            }
        }.onFailure { throwable ->
            Log.e(LOG_TAG, "Unable to schedule quick sit alarm", throwable)
        }.isSuccess

        if (scheduled) {
            prefs(appContext).edit().putLong(KEY_END_ELAPSED_REALTIME, triggerAt).apply()
        }
        return scheduled
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(appContext))
        clearState(appContext)
    }

    fun clearState(context: Context) {
        prefs(context.applicationContext).edit().remove(KEY_END_ELAPSED_REALTIME).apply()
    }

    fun scheduledEndElapsedRealtime(context: Context): Long {
        return prefs(context.applicationContext).getLong(KEY_END_ELAPSED_REALTIME, 0L)
    }

    private fun pendingIntent(
        context: Context,
        soundId: Int = BellCatalog.defaultSound.id,
        chimes: Int = 1
    ): PendingIntent {
        val intent = Intent(context, QuickSitAlarmReceiver::class.java).apply {
            action = ACTION_QUICK_SIT_ALARM
            putExtra(EXTRA_SOUND_ID, soundId)
            putExtra(EXTRA_CHIMES, chimes.coerceIn(1, 12))
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

const val ACTION_QUICK_SIT_ALARM = "com.neversink.impermanence.action.QUICK_SIT_ALARM"
const val EXTRA_SOUND_ID = "extra_sound_id"
const val EXTRA_CHIMES = "extra_chimes"
