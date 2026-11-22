package com.neversink.impermanence.service

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.neversink.impermanence.domain.audio.BellOutput
import com.neversink.impermanence.domain.audio.BellPlayer
import com.neversink.impermanence.domain.session.SessionPlanner
import com.neversink.impermanence.domain.session.TimingSession
import com.neversink.impermanence.domain.timer.DaySchedule
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId
import java.util.UUID

/**
 * The single owner of bell timing. Every path that can make a bell due (the exact alarm, the foreground
 * service's in-process timer, boot, clock and time zone changes, the UI starting a session) calls
 * [process], which asks the pure [SessionPlanner] what to do, persists the result synchronously, rings at
 * most one bell, and arms the alarm for the next one. Calls are serialized, and the persisted
 * "handled through" instant makes a bell ring at most once however many triggers fire.
 */
object SessionEngine {
    private const val LOG_TAG = "SessionEngine"

    enum class Trigger { START, ALARM, SERVICE_TICK, SERVICE_RESTART, BOOT, TIME_CHANGE, APP_START, SETTINGS }

    enum class EndReason { LEFT_SCREEN, NOTIFICATION_STOP, TASK_REMOVED, QUICK_SIT_CANCELLED, REPLACED }

    @Volatile @VisibleForTesting var bellOutput: BellOutput = BellPlayer
    @Volatile @VisibleForTesting var clock: () -> Long = { System.currentTimeMillis() }
    @Volatile @VisibleForTesting var zone: () -> ZoneId = { ZoneId.systemDefault() }
    @Volatile @VisibleForTesting var serviceController: ServiceController = TimingService.Controller

    private val _session = MutableStateFlow<TimingSession?>(null)
    /** The active session, or null when nothing is being timed. */
    val session: StateFlow<TimingSession?> = _session.asStateFlow()

    private val _nextWakeMillis = MutableStateFlow<Long?>(null)
    val nextWakeMillis: StateFlow<Long?> = _nextWakeMillis.asStateFlow()

    private val _strikesUntilMillis = MutableStateFlow<Long?>(null)
    /**
     * Until when the foreground service must keep the process alive for the strikes of the bell that is
     * ringing (its last strike plus time to sound), or null when no strike is to come. A Quick Sit's
     * session ends at its end bell, but its later strikes still need the process.
     */
    val strikesUntilMillis: StateFlow<Long?> = _strikesUntilMillis.asStateFlow()

    /** The strikes this process posted to [bellOutput]. After process death it is null again. */
    @Volatile private var ownStrikes: SessionStore.Strikes? = null

    private val _alarmMode = MutableStateFlow<BellAlarmScheduler.Mode?>(null)
    val alarmMode: StateFlow<BellAlarmScheduler.Mode?> = _alarmMode.asStateFlow()

    fun now(): Long = clock()

    /** Loads the persisted session into memory. Safe in direct boot. */
    @Synchronized
    fun initialize(context: Context) {
        val store = SessionStore(context)
        _session.value = store.load()
        _strikesUntilMillis.value = store.strikes?.let { it.lastStrikeMillis + STRIKE_SOUND_MILLIS }
    }

    /** What a new process knows: the in-memory record of posted strikes is lost with the old process. */
    @VisibleForTesting
    fun forgetProcessState() {
        ownStrikes = null
    }

    /**
     * Begins timing [day], or resumes it when that day is already being timed (for example after the
     * process was killed and the screen was restored).
     */
    @Synchronized
    fun startDay(context: Context, day: Day, loopDays: Boolean) {
        val store = SessionStore(context)
        val existing = store.load()
        val nowMillis = now()
        val session = if (existing is TimingSession.DaySession && existing.day.id == day.id) {
            existing.copy(day = day, loopDays = loopDays)
        } else {
            if (existing != null) endInternal(context, store, EndReason.REPLACED)
            TimingSession.DaySession(
                sessionId = UUID.randomUUID().toString(),
                day = day,
                loopDays = loopDays,
                anchorDate = DaySchedule.initialDate(day, nowMillis, zone()).toString(),
                // A bell exactly at the open instant rings; nothing before it does.
                handledThroughMillis = nowMillis - 1
            )
        }
        store.save(session)
        _session.value = session
        processLocked(context, store, Trigger.START, allowServiceStart = true)
    }

    /** Starts a Quick Sit: start chimes now, end chimes at now + [durationMillis]. */
    @Synchronized
    fun startQuickSit(context: Context, durationMillis: Long, soundId: Int, startChimes: Int, endChimes: Int) {
        val store = SessionStore(context)
        store.load()?.let { endInternal(context, store, EndReason.REPLACED) }
        val nowMillis = now()
        val session = TimingSession.QuickSitSession(
            sessionId = UUID.randomUUID().toString(),
            startMillis = nowMillis,
            endMillis = nowMillis + durationMillis.coerceAtLeast(1_000L),
            endBell = Bell(soundId = soundId, numRings = endChimes).sanitized(),
            handledThroughMillis = nowMillis
        )
        store.save(session)
        _session.value = session
        val startBell = Bell(soundId = soundId, numRings = startChimes).sanitized()
        bellOutput.ring(context, startBell)
        recordStrikes(store, startBell, nowMillis)
        RingLog(context).add(RingLog.Entry(nowMillis, "quick-sit/start", nowMillis, "rang", Trigger.START.name))
        processLocked(context, store, Trigger.START, allowServiceStart = true)
    }

    @Synchronized
    fun setLoopDays(context: Context, loopDays: Boolean) {
        val store = SessionStore(context)
        val current = store.load() as? TimingSession.DaySession ?: return
        if (current.loopDays == loopDays) return
        store.save(current.copy(loopDays = loopDays))
        processLocked(context, store, Trigger.SETTINGS, allowServiceStart = true)
    }

    /** Ends timing: no further bells. Chimes already sounding finish; chimes not yet started are cancelled. */
    @Synchronized
    fun end(context: Context, reason: EndReason) {
        endInternal(context, SessionStore(context), reason)
    }

    /** Ends the session only if it is still [sessionId] (so a stale screen can't end a newer session). */
    @Synchronized
    fun endIfCurrent(context: Context, sessionId: String, reason: EndReason) {
        val store = SessionStore(context)
        if (store.load()?.sessionId == sessionId) endInternal(context, store, reason)
    }

    @Synchronized
    fun process(context: Context, trigger: Trigger, allowServiceStart: Boolean) {
        processLocked(context, SessionStore(context), trigger, allowServiceStart)
    }

    private fun processLocked(context: Context, store: SessionStore, trigger: Trigger, allowServiceStart: Boolean) {
        val nowMillis = now()
        resumeStrikes(context, store, nowMillis, trigger)
        val session = store.load()
        if (session == null) {
            BellAlarmScheduler.cancel(context)
            _session.value = null
            _nextWakeMillis.value = null
            stopServiceUnlessStriking(context, allowServiceStart)
            return
        }
        val result = SessionPlanner.process(session, nowMillis, zone())
        val log = RingLog(context)
        if (result.session == null) store.clear() else store.save(result.session)
        result.skippedKeys.forEach { key ->
            log.add(RingLog.Entry(nowMillis, key, -1, "skipped-late", trigger.name))
        }
        result.ring?.let { ring ->
            runCatching { bellOutput.ring(context, ring.bell) }
                .onFailure { Log.e(LOG_TAG, "Bell output failed for ${ring.key}", it) }
            recordStrikes(store, ring.bell, nowMillis)
            log.add(RingLog.Entry(nowMillis, ring.key, ring.scheduledAtMillis, "rang", trigger.name))
        }
        val next = result.nextWakeMillis
        if (next != null) {
            if (store.scheduledAlarmMillis != next || trigger != Trigger.SERVICE_TICK) {
                _alarmMode.value = BellAlarmScheduler.schedule(context, next)
                store.scheduledAlarmMillis = next
            }
        } else {
            BellAlarmScheduler.cancel(context)
            if (result.session != null) store.scheduledAlarmMillis = 0L
        }
        _nextWakeMillis.value = next
        _session.value = result.session
        when {
            result.session == null -> stopServiceUnlessStriking(context, allowServiceStart)
            allowServiceStart -> serviceController.ensureRunning(context)
        }
    }

    /** Stops the service once no session runs, but not while the last bell still has strikes to come. */
    private fun stopServiceUnlessStriking(context: Context, allowServiceStart: Boolean) {
        when {
            _strikesUntilMillis.value == null -> serviceController.stop(context)
            allowServiceStart -> serviceController.ensureRunning(context)
        }
    }

    private fun recordStrikes(store: SessionStore, bell: Bell, nowMillis: Long) {
        val strikes = SessionStore.Strikes(bell.sanitized(), nowMillis)
        store.strikes = strikes
        ownStrikes = strikes
        _strikesUntilMillis.value = strikes.lastStrikeMillis + STRIKE_SOUND_MILLIS
    }

    /**
     * Keeps [strikesUntilMillis] current and, in a new process, finishes a bell whose strikes the old
     * process had posted: strikes still to come ring at their own instants. A strike whose instant has
     * passed may already have sounded, so it is never repeated (a bell never rings twice).
     */
    private fun resumeStrikes(context: Context, store: SessionStore, nowMillis: Long, trigger: Trigger) {
        val strikes = store.strikes
        if (strikes == null) {
            _strikesUntilMillis.value = null
            return
        }
        val until = strikes.lastStrikeMillis + STRIKE_SOUND_MILLIS
        // Over, or the clock was set back far enough that the record no longer means anything.
        if (nowMillis > until || nowMillis < strikes.firstStrikeMillis - SessionPlanner.LATE_GRACE_MILLIS) {
            store.strikes = null
            ownStrikes = null
            _strikesUntilMillis.value = null
            return
        }
        _strikesUntilMillis.value = until
        if (strikes == ownStrikes) return
        ownStrikes = strikes
        val spacing = BellPlayer.CHIME_SPACING_MILLIS
        val from = (0 until strikes.bell.numRings).firstOrNull { strikes.firstStrikeMillis + it * spacing > nowMillis } ?: return
        val firstAt = strikes.firstStrikeMillis + from * spacing
        runCatching { bellOutput.ringRemaining(context, strikes.bell, from, firstAt - nowMillis) }
            .onFailure { Log.e(LOG_TAG, "Bell output failed resuming strikes", it) }
        RingLog(context).add(RingLog.Entry(nowMillis, "strikes/from-$from", firstAt, "resumed", trigger.name))
    }

    private fun endInternal(context: Context, store: SessionStore, reason: EndReason) {
        val existing = store.load()
        store.clear()
        store.strikes = null
        ownStrikes = null
        _strikesUntilMillis.value = null
        BellAlarmScheduler.cancel(context)
        bellOutput.cancelPending()
        _session.value = null
        _nextWakeMillis.value = null
        if (existing != null) {
            RingLog(context).add(RingLog.Entry(now(), "session/end:${reason.name}", -1, "ended", reason.name))
        }
        if (reason != EndReason.REPLACED) serviceController.stop(context)
    }

    /** How long after a strike starts it may still be sounding (matches BellPlayer's wake lock bound). */
    private const val STRIKE_SOUND_MILLIS = 15_000L

    /** Lets the engine start and stop the foreground service; replaced in tests. */
    interface ServiceController {
        fun ensureRunning(context: Context)
        fun stop(context: Context)
    }
}
