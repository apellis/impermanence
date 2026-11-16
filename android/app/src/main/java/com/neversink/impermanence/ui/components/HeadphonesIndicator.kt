package com.neversink.impermanence.ui.components

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.neversink.impermanence.R

private val HEADPHONE_TYPES = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    26, // AudioDeviceInfo.TYPE_BLE_HEADSET (API 31)
    27  // AudioDeviceInfo.TYPE_BLE_SPEAKER (API 31)
)

/** True while headphones or a Bluetooth audio device are connected, so bells also play there. */
fun headphonesConnected(context: Context): Boolean = runCatching {
    context.getSystemService(AudioManager::class.java)
        ?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        ?.any { it.type in HEADPHONE_TYPES } == true
}.getOrDefault(false)

/**
 * A headphones icon for the timing screens' top bar, shown only while headphones are connected.
 * Tapping it explains where bells will sound.
 */
@Composable
fun HeadphonesIndicator() {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(headphonesConnected(context)) }
    // Updates the moment a device connects or disconnects, with no polling.
    DisposableEffect(context) {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                connected = headphonesConnected(context)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                connected = headphonesConnected(context)
            }
        }
        runCatching { audioManager?.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper())) }
        connected = headphonesConnected(context)
        onDispose { runCatching { audioManager?.unregisterAudioDeviceCallback(callback) } }
    }
    if (!connected) return
    val explanation = stringResource(R.string.headphones_connected)
    IconButton(onClick = { Toast.makeText(context, explanation, Toast.LENGTH_LONG).show() }) {
        Icon(imageVector = Icons.Default.Headphones, contentDescription = explanation)
    }
}
