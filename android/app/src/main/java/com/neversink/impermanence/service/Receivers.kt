package com.neversink.impermanence.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires at each bell instant (exact alarm). Rings the bell and makes sure the service is up. */
class BellAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != BellAlarmScheduler.ACTION_BELL_ALARM) return
        // An exact/alarm-clock alarm is allowed to start a foreground service from the background.
        SessionEngine.process(context, SessionEngine.Trigger.ALARM, allowServiceStart = true)
    }
}

/**
 * Re-arms the bell alarm after events that clear or invalidate it: reboot (including before first unlock),
 * app update, wall-clock or time zone change, and exact-alarm permission changes. It does not start the
 * foreground service (not allowed from these broadcasts on recent Android); the re-armed alarm does that.
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val trigger = when (intent?.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> SessionEngine.Trigger.BOOT
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> SessionEngine.Trigger.TIME_CHANGE
            else -> return
        }
        SessionEngine.process(context, trigger, allowServiceStart = false)
    }
}
