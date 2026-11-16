package com.neversink.impermanence.domain.session

import com.neversink.impermanence.domain.timer.DaySchedule
import com.neversink.impermanence.model.Bell
import java.time.LocalDate
import java.time.ZoneId

/**
 * Decides, for a session and a moment in time, which bell (if any) must ring now, what the session's
 * new persisted state is, and when the next bell is due. Pure and deterministic: every trigger path
 * (exact alarm, in-process timer, boot, clock change) goes through [process], and the persisted
 * [TimingSession.handledThroughMillis] guarantees a bell event rings at most once.
 *
 * Rules (docs/BEHAVIOR.md):
 * - Nothing at or before the session's open instant ever rings (no backfill).
 * - A due event rings only if it is at most [LATE_GRACE_MILLIS] late, and only the most recent one;
 *   older missed events are skipped silently. All chimes of an event ring or none do.
 */
object SessionPlanner {
    const val LATE_GRACE_MILLIS = 60_000L

    data class Ring(val key: String, val bell: Bell, val scheduledAtMillis: Long)

    data class Result(
        /** The session to persist, or null when the session is finished (Quick Sit ended). */
        val session: TimingSession?,
        /** The bell to ring right now, if any. */
        val ring: Ring?,
        /** When [process] must run again (next bell instant), or null when no bell is pending. */
        val nextWakeMillis: Long?,
        /** Keys of due events skipped because they were too late. For logging. */
        val skippedKeys: List<String> = emptyList()
    )

    fun process(session: TimingSession, nowMillis: Long, zone: ZoneId): Result = when (session) {
        is TimingSession.DaySession -> processDay(session, nowMillis, zone)
        is TimingSession.QuickSitSession -> processQuickSit(session, nowMillis)
    }

    private fun processDay(session: TimingSession.DaySession, nowMillis: Long, zone: ZoneId): Result {
        val day = session.day
        var date = LocalDate.parse(session.anchorDate)
        var handled = session.handledThroughMillis
        var ring: Ring? = null
        val skipped = mutableListOf<String>()

        // Bound the loop defensively; each pass either rolls to a strictly later date or stops.
        repeat(MAX_ROLLOVERS_PER_PASS) {
            val occurrence = DaySchedule.occurrence(day, date, zone)
                ?: return Result(session.copy(anchorDate = date.toString(), handledThroughMillis = handled), null, null)
            val due = DaySchedule.events(day, occurrence)
                .filter { it.atMillis > handled && it.atMillis <= nowMillis }
            if (due.isNotEmpty()) {
                val latest = due.last()
                due.dropLast(1).forEach { skipped += it.key }
                if (nowMillis - latest.atMillis <= LATE_GRACE_MILLIS) {
                    ring?.let { skipped += it.key }
                    ring = Ring(latest.key, latest.bell, latest.atMillis)
                } else {
                    skipped += latest.key
                }
                handled = maxOf(handled, latest.atMillis)
            }
            val next = DaySchedule.effectiveDate(day, date, nowMillis, zone, session.loopDays)
            if (next == date) {
                val updated = session.copy(anchorDate = date.toString(), handledThroughMillis = handled)
                return Result(updated, ring, nextDayWake(updated, zone), skipped)
            }
            date = next
        }
        val updated = session.copy(anchorDate = date.toString(), handledThroughMillis = handled)
        return Result(updated, ring, nextDayWake(updated, zone), skipped)
    }

    /** The next instant a bell is due, including the first bell of the next occurrence when looping. */
    fun nextDayWake(session: TimingSession.DaySession, zone: ZoneId): Long? {
        val day = session.day
        val date = LocalDate.parse(session.anchorDate)
        val occurrence = DaySchedule.occurrence(day, date, zone) ?: return null
        DaySchedule.events(day, occurrence).firstOrNull { it.atMillis > session.handledThroughMillis }
            ?.let { return it.atMillis }
        if (!session.loopDays) return null
        // All of this occurrence is handled. The next bell is the first one of a later occurrence. The
        // rollover itself happens lazily the next time process() runs at or after that instant.
        var candidate = date.plusDays(1)
        repeat(3) {
            val nextOccurrence = DaySchedule.occurrence(day, candidate, zone) ?: return null
            if (nextOccurrence.startMillis > session.handledThroughMillis) return nextOccurrence.startMillis
            candidate = candidate.plusDays(1)
        }
        return null
    }

    private fun processQuickSit(session: TimingSession.QuickSitSession, nowMillis: Long): Result {
        if (session.handledThroughMillis >= session.endMillis) {
            return Result(null, null, null)
        }
        if (nowMillis < session.endMillis) {
            return Result(session, null, session.endMillis)
        }
        val lateBy = nowMillis - session.endMillis
        return if (lateBy <= LATE_GRACE_MILLIS) {
            Result(null, Ring("quick-sit/end", session.endBell.sanitized(), session.endMillis), null)
        } else {
            Result(null, null, null, listOf("quick-sit/end"))
        }
    }

    private const val MAX_ROLLOVERS_PER_PASS = 8
}
