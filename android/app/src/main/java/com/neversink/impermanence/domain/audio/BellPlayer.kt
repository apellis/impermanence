package com.neversink.impermanence.domain.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import com.neversink.impermanence.model.Bell

object BellPlayer {
    private const val RING_DELAY_MILLIS = 3000L

    private val playbackThread = HandlerThread("BellPlayer").apply { start() }
    private val handler = Handler(playbackThread.looper)
    private val activePlaybacks = mutableSetOf<ActivePlayback>()

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun play(context: Context, bell: Bell) {
        val appContext = context.applicationContext
        val rings = bell.numRings.coerceAtLeast(1)
        repeat(rings) { index ->
            val delayMillis = index * RING_DELAY_MILLIS
            handler.postDelayed({ playSingle(appContext, bell) }, delayMillis)
        }
    }

    fun stopAll() {
        val playbacks = synchronized(activePlaybacks) {
            activePlaybacks.toList().also { activePlaybacks.clear() }
        }
        playbacks.forEach { playback ->
            teardownPlayback(playback)
        }
        handler.removeCallbacksAndMessages(null)
    }

    private fun playSingle(context: Context, bell: Bell) {
        val resId = resolveRawResourceId(context, bell) ?: return
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        val focusRequest = buildFocusRequest()
        val focusGranted = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!focusGranted) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            return
        }

        val assetFileDescriptor = context.resources.openRawResourceFd(resId) ?: run {
            audioManager.abandonAudioFocusRequest(focusRequest)
            return
        }
        val mediaPlayer = MediaPlayer()
        val playback = ActivePlayback(mediaPlayer, audioManager, focusRequest)
        try {
            mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mediaPlayer.setAudioAttributes(audioAttributes)
            mediaPlayer.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )

            mediaPlayer.setOnCompletionListener { teardownPlayback(playback) }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                teardownPlayback(playback)
                true
            }

            registerPlayback(playback)

            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (throwable: Throwable) {
            teardownPlayback(playback)
        } finally {
            assetFileDescriptor.close()
        }
    }

    private fun registerPlayback(playback: ActivePlayback) {
        synchronized(activePlaybacks) {
            activePlaybacks.add(playback)
        }
    }

    private fun teardownPlayback(playback: ActivePlayback) {
        synchronized(activePlaybacks) {
            activePlaybacks.remove(playback)
        }

        val player = playback.mediaPlayer
        try {
            player.stop()
        } catch (_: IllegalStateException) {
        }
        try {
            player.reset()
        } catch (_: IllegalStateException) {
        }
        player.release()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playback.audioManager.abandonAudioFocusRequest(playback.focusRequest)
        }
    }

    private fun buildFocusRequest(): AudioFocusRequest {
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .build()
    }

    private fun resolveRawResourceId(context: Context, bell: Bell): Int? {
        val packageName = context.packageName
        val resources = context.resources
        bell.sound.resourceCandidates.forEach { candidate ->
            val resId = resources.getIdentifier(candidate, "raw", packageName)
            if (resId != 0) {
                return resId
            }
        }
        return null
    }

    private data class ActivePlayback(
        val mediaPlayer: MediaPlayer,
        val audioManager: AudioManager,
        val focusRequest: AudioFocusRequest
    )
}
