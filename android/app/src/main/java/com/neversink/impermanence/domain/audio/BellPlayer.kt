package com.neversink.impermanence.domain.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.neversink.impermanence.model.Bell

/** Where bells go. Production uses [BellPlayer]; tests substitute a recorder. */
interface BellOutput {
    /** Rings [bell] now: chime i starts 3 s × i after this call. */
    fun ring(context: Context, bell: Bell)

    /** Cancels chimes that have not started yet. A chime already sounding plays to its end. */
    fun cancelPending()
}

/**
 * Plays bells on the alarm audio stream, so they sound in silent/vibrate mode and through Do Not
 * Disturb's default "alarms allowed" setting. A timed wake lock covers the whole chime sequence, so later
 * chimes are not delayed by the CPU sleeping between them with the screen off.
 */
object BellPlayer : BellOutput {
    private const val LOG_TAG = "BellPlayer"
    const val CHIME_SPACING_MILLIS = 3_000L
    private const val WAKE_LOCK_TAG = "Impermanence:BellChimes"
    /** Generous upper bound on one chime's audio length, for the wake lock. */
    private const val MAX_CHIME_AUDIO_MILLIS = 15_000L

    private val playbackThread = HandlerThread("BellPlayer").apply { start() }
    private val handler = Handler(playbackThread.looper)
    private val pendingToken = Any()
    private val activePlaybacks = mutableSetOf<ActivePlayback>()
    private var wakeLock: PowerManager.WakeLock? = null

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    override fun ring(context: Context, bell: Bell) {
        val appContext = context.applicationContext
        val sanitized = bell.sanitized()
        val rings = sanitized.numRings
        acquireWakeLock(appContext, (rings - 1) * CHIME_SPACING_MILLIS + MAX_CHIME_AUDIO_MILLIS)
        val base = SystemClock.uptimeMillis()
        repeat(rings) { index ->
            handler.postAtTime({ playSingle(appContext, sanitized) }, pendingToken, base + index * CHIME_SPACING_MILLIS)
        }
    }

    /** Back-compat name used by the UI's "Ring now". */
    fun play(context: Context, bell: Bell) = ring(context, bell)

    override fun cancelPending() {
        handler.removeCallbacksAndMessages(pendingToken)
    }

    private fun acquireWakeLock(context: Context, timeoutMillis: Long) {
        runCatching {
            synchronized(this) {
                val lock = wakeLock ?: (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                    .apply { setReferenceCounted(false) }
                    .also { wakeLock = it }
                // Non-reference-counted: re-acquiring extends the timeout to cover the newest bell.
                lock.acquire(timeoutMillis)
            }
        }.onFailure { Log.e(LOG_TAG, "Unable to acquire chime wake lock", it) }
    }

    private fun playSingle(context: Context, bell: Bell) {
        val resId = resolveRawResourceId(context, bell)
        if (resId == null) {
            Log.e(LOG_TAG, "No audio resource for bell sound ${bell.soundId}")
            return
        }
        val audioManager = context.getSystemService(AudioManager::class.java)
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .build()
        // Bells must always ring, so focus is requested (to duck other audio) but a refusal is ignored.
        runCatching { audioManager?.requestAudioFocus(focusRequest) }
        val mediaPlayer = MediaPlayer()
        val playback = ActivePlayback(mediaPlayer, audioManager, focusRequest)
        try {
            context.resources.openRawResourceFd(resId).use { afd ->
                mediaPlayer.setAudioAttributes(audioAttributes)
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mediaPlayer.setOnCompletionListener { teardown(playback) }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                Log.e(LOG_TAG, "MediaPlayer error $what/$extra")
                teardown(playback)
                true
            }
            synchronized(activePlaybacks) { activePlaybacks.add(playback) }
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (throwable: Throwable) {
            Log.e(LOG_TAG, "Bell playback failed", throwable)
            teardown(playback)
        }
    }

    private fun teardown(playback: ActivePlayback) {
        synchronized(activePlaybacks) { activePlaybacks.remove(playback) }
        runCatching { playback.mediaPlayer.release() }
        runCatching { playback.audioManager?.abandonAudioFocusRequest(playback.focusRequest) }
    }

    private fun resolveRawResourceId(context: Context, bell: Bell): Int? {
        bell.sound.resourceCandidates.forEach { candidate ->
            val resId = context.resources.getIdentifier(candidate, "raw", context.packageName)
            if (resId != 0) return resId
        }
        return null
    }

    private data class ActivePlayback(
        val mediaPlayer: MediaPlayer,
        val audioManager: AudioManager?,
        val focusRequest: AudioFocusRequest
    )
}
