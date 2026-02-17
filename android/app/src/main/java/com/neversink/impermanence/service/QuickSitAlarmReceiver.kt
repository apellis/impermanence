package com.neversink.impermanence.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neversink.impermanence.model.BellCatalog

class QuickSitAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_QUICK_SIT_ALARM) return

        val soundId = intent.getIntExtra(EXTRA_SOUND_ID, BellCatalog.defaultSound.id)
        val chimes = intent.getIntExtra(EXTRA_CHIMES, 1).coerceIn(1, 12)

        QuickSitAlarmScheduler.clearState(context)
        QuickSitBellService.start(context, soundId, chimes)
    }
}
