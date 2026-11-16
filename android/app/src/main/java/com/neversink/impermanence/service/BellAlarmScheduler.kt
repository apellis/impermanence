package com.neversink.impermanence.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.neversink.impermanence.MainActivity

/**
 * Arms one exact alarm for the next bell. [AlarmManager.setAlarmClock] is used because it is the one
 * alarm type that fires exactly on time in Doze and App Standby, restarts a killed process, and allows
 * starting a foreground service from the background. The exact-alarm permission comes from
 * USE_EXACT_ALARM (API 33+) or SCHEDULE_EXACT_ALARM (API 31-32, granted by default).
 */
object BellAlarmScheduler {
    private const val LOG_TAG = "BellAlarmScheduler"
    const val ACTION_BELL_ALARM = "com.neversink.impermanence.action.BELL_ALARM"
    private const val REQUEST_CODE_ALARM = 4101
    private const val REQUEST_CODE_SHOW = 4102

    enum class Mode { ALARM_CLOCK, EXACT_WHILE_IDLE, INEXACT }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    /** Schedules (replacing any previous) the bell alarm at [atMillis] wall-clock time. */
    fun schedule(context: Context, atMillis: Long): Mode? {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return null
        val operation = alarmIntent(appContext)
        if (canScheduleExact(appContext)) {
            val setAsAlarmClock = runCatching {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent(appContext)), operation)
            }.onFailure { Log.e(LOG_TAG, "setAlarmClock failed", it) }.isSuccess
            if (setAsAlarmClock) return Mode.ALARM_CLOCK
            val setExact = runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
            }.onFailure { Log.e(LOG_TAG, "setExactAndAllowWhileIdle failed", it) }.isSuccess
            if (setExact) return Mode.EXACT_WHILE_IDLE
        }
        // Last resort when the exact-alarm permission was revoked: may be late, the UI warns about it.
        return runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
            Mode.INEXACT
        }.onFailure { Log.e(LOG_TAG, "setAndAllowWhileIdle failed", it) }.getOrNull()
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        runCatching { alarmManager.cancel(alarmIntent(appContext)) }
    }

    private fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, BellAlarmReceiver::class.java).setAction(ACTION_BELL_ALARM)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_SHOW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
