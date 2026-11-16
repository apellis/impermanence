package com.neversink.impermanence.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.neversink.impermanence.R
import com.neversink.impermanence.service.BellAlarmScheduler
import kotlinx.coroutines.delay

/** Things on the device that would stop bells from being heard on time. */
data class BellHealth(
    val alarmVolumeOff: Boolean,
    val exactAlarmsOff: Boolean,
    val notificationsOff: Boolean
) {
    val hasProblem: Boolean get() = alarmVolumeOff || exactAlarmsOff || notificationsOff

    companion object {
        fun check(context: Context): BellHealth {
            val audio = context.getSystemService(AudioManager::class.java)
            val alarmVolume = runCatching { audio?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 1 }.getOrDefault(1)
            return BellHealth(
                alarmVolumeOff = alarmVolume == 0,
                exactAlarmsOff = !BellAlarmScheduler.canScheduleExact(context),
                notificationsOff = !NotificationManagerCompat.from(context).areNotificationsEnabled()
            )
        }
    }
}

/** Shows a warning card while something would keep bells from ringing, rechecked every two seconds. */
@Composable
fun BellHealthBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var health by remember { mutableStateOf(BellHealth.check(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            health = BellHealth.check(context)
            delay(2_000L)
        }
    }
    if (!health.hasProblem) return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (health.alarmVolumeOff) {
                Text(stringResource(R.string.warning_alarm_volume_off), style = MaterialTheme.typography.bodyMedium)
            }
            if (health.exactAlarmsOff) {
                Text(stringResource(R.string.warning_exact_alarms_off), style = MaterialTheme.typography.bodyMedium)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) { Text(stringResource(R.string.warning_exact_alarms_action)) }
                }
            }
            if (health.notificationsOff) {
                Text(stringResource(R.string.warning_notifications_off), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Asks once per screen visit for the notification permission (Android 13+), used by the timer notification. */
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
