package com.neversink.impermanence.domain.timer

import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure, time-zone-aware schedule math for a [Day]. No Android dependencies, so it is fully unit tested
 * on the JVM. Semantics (shared with iOS, see docs/BEHAVIOR.md):
 *
 * - A day occurrence belongs to a calendar date D. It starts at the day's wall-clock start time on D in
 *   the current zone (a start time that falls in a DST gap moves forward, per [ZonedDateTime.of]).
 * - Segment boundaries are the start plus cumulative segment durations, in absolute elapsed time, so a
 *   45-minute segment always lasts 45 real minutes, even across a DST change.
 * - Bell events: the day's start bell at the start, then each segment's end bell at that segment's end.
 */
object DaySchedule {
    const val SECONDS_PER_DAY = 24 * 60 * 60

    data class SegmentWindow(
        val index: Int,
        val segment: Day.Segment,
        val startMillis: Long,
        val endMillis: Long
    )

    data class Occurrence(
        val date: LocalDate,
        val startMillis: Long,
        val endMillis: Long,
        val segments: List<SegmentWindow>
    )

    enum class EventKind { START, SEGMENT_END }

    data class BellEvent(
        val date: LocalDate,
        val kind: EventKind,
        /** -1 for the start bell, otherwise the index of the segment that just ended. */
        val segmentIndex: Int,
        val atMillis: Long,
        val bell: Bell
    ) {
        /** Stable identity of this event, used for de-duplication and logging. */
        val key: String
            get() = if (kind == EventKind.START) "$date/start" else "$date/end-$segmentIndex"
    }

    fun normalizedStartSeconds(seconds: Int): Int {
        val mod = seconds % SECONDS_PER_DAY
        return if (mod < 0) mod + SECONDS_PER_DAY else mod
    }

    /** The occurrence of [day] that belongs to [date], or null when the day has no segments. */
    fun occurrence(day: Day, date: LocalDate, zone: ZoneId): Occurrence? {
        if (day.segments.isEmpty()) return null
        val startTime = LocalTime.ofSecondOfDay(normalizedStartSeconds(day.startTimeSeconds).toLong())
        val start = wallClockInstant(date, startTime, zone).toEpochMilli()
        var cursor = start
        val windows = day.segments.mapIndexed { index, segment ->
            val segmentStart = cursor
            val segmentEnd = segmentStart + segment.durationSeconds.coerceAtLeast(1) * 1000L
            cursor = segmentEnd
            SegmentWindow(index, segment, segmentStart, segmentEnd)
        }
        return Occurrence(date = date, startMillis = start, endMillis = cursor, segments = windows)
    }

    /**
     * The instant of a wall-clock time on a date. A time inside a DST gap shifts forward by the gap length
     * (02:30 becomes 03:30 on a one-hour spring-forward day). An ambiguous time (fall-back) uses its first
     * occurrence (the earlier offset). This is java.time's default and matches iOS
     * `.nextTimePreservingSmallerComponents`.
     */
    fun wallClockInstant(date: LocalDate, time: LocalTime, zone: ZoneId): Instant =
        ZonedDateTime.ofLocal(date.atTime(time), zone, null).toInstant()

    /** All bell events of an occurrence, in chronological order. */
    fun events(day: Day, occurrence: Occurrence): List<BellEvent> {
        val events = ArrayList<BellEvent>(occurrence.segments.size + 1)
        events += BellEvent(
            date = occurrence.date,
            kind = EventKind.START,
            segmentIndex = -1,
            atMillis = occurrence.startMillis,
            bell = day.startBell.sanitized()
        )
        occurrence.segments.forEach { window ->
            events += BellEvent(
                date = occurrence.date,
                kind = EventKind.SEGMENT_END,
                segmentIndex = window.index,
                atMillis = window.endMillis,
                bell = window.segment.resolvedBell(day.defaultBell).sanitized()
            )
        }
        return events
    }

    /**
     * The occurrence date to use when timing begins at [nowMillis]: yesterday's occurrence when it is still
     * running (a day that crosses midnight), otherwise today's.
     */
    fun initialDate(day: Day, nowMillis: Long, zone: ZoneId): LocalDate {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val yesterday = today.minusDays(1)
        val yesterdayOccurrence = occurrence(day, yesterday, zone)
        if (yesterdayOccurrence != null && nowMillis < yesterdayOccurrence.endMillis) {
            return yesterday
        }
        return today
    }

    /**
     * With loop days on, timing moves from occurrence date D to the next one only once [nowMillis] is past
     * the end of D *and* the calendar date is past D. A normal day therefore shows "complete" until
     * midnight, and a day that crosses midnight keeps running past midnight.
     */
    fun shouldRollOver(day: Day, date: LocalDate, nowMillis: Long, zone: ZoneId, loopDays: Boolean): Boolean {
        if (!loopDays) return false
        val occurrence = occurrence(day, date, zone) ?: return false
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return nowMillis >= occurrence.endMillis && nowDate.isAfter(date)
    }

    /** The occurrence date in effect at [nowMillis], applying loop rollover from [anchorDate]. */
    fun effectiveDate(day: Day, anchorDate: LocalDate, nowMillis: Long, zone: ZoneId, loopDays: Boolean): LocalDate {
        if (!shouldRollOver(day, anchorDate, nowMillis, zone, loopDays)) return anchorDate
        val next = initialDate(day, nowMillis, zone)
        return if (next.isAfter(anchorDate)) next else anchorDate
    }

    enum class Status { EMPTY, NOT_STARTED, ACTIVE, COMPLETE }

    data class DisplayState(
        val status: Status,
        /** -2 empty, -1 not started, segment index while active, segment count when complete. */
        val segmentIndex: Int,
        val segmentText: String,
        val elapsedSeconds: Long,
        val remainingSeconds: Long,
        val occurrence: Occurrence?,
        /** The next bell event after now in this occurrence, if any. */
        val nextEvent: BellEvent?
    )

    fun displayState(day: Day, occurrence: Occurrence?, nowMillis: Long): DisplayState {
        if (occurrence == null) {
            return DisplayState(Status.EMPTY, -2, "(Day is empty)", -1, -1, null, null)
        }
        val next = events(day, occurrence).firstOrNull { it.atMillis > nowMillis }
        if (nowMillis < occurrence.startMillis) {
            return DisplayState(Status.NOT_STARTED, -1, "(Day has not started yet)", -1, -1, occurrence, next)
        }
        val window = occurrence.segments.firstOrNull { nowMillis >= it.startMillis && nowMillis < it.endMillis }
            ?: return DisplayState(Status.COMPLETE, occurrence.segments.size, "(Day is over)", -1, -1, occurrence, null)
        val elapsed = (nowMillis - window.startMillis) / 1000L
        val remaining = (window.endMillis - nowMillis + 999L) / 1000L
        return DisplayState(
            status = Status.ACTIVE,
            segmentIndex = window.index,
            segmentText = "Now: ${window.segment.name}",
            elapsedSeconds = elapsed,
            remainingSeconds = remaining,
            occurrence = occurrence,
            nextEvent = next
        )
    }
}
