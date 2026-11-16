package com.neversink.impermanence.model

import java.util.UUID

/**
 * Makes day plans safe to use, whatever file they came from: valid bells (1...12 chimes, known sound),
 * start times within one day, durations of at least one second and at most one day, and unique ids that
 * are safe in navigation routes. Duplicate or malformed ids are replaced, because lists key on them.
 */
object DaySanitizer {
    private const val SECONDS_PER_DAY = 24 * 60 * 60
    private val safeId = Regex("^[A-Za-z0-9-]{1,64}$")

    fun sanitize(days: List<Day>): List<Day> {
        val seenDayIds = HashSet<String>()
        return days.map { day ->
            val dayId = uniqueId(day.id, seenDayIds)
            val seenSegmentIds = HashSet<String>()
            day.copy(
                id = dayId,
                startTimeSeconds = normalizeStart(day.startTimeSeconds),
                segments = day.segments.map { segment ->
                    segment.copy(
                        id = uniqueId(segment.id, seenSegmentIds),
                        durationSeconds = segment.durationSeconds.coerceIn(1, SECONDS_PER_DAY),
                        customEndBell = segment.customEndBell?.sanitized()
                    )
                },
                startBell = day.startBell.sanitized(),
                manualBell = day.manualBell.sanitized(),
                defaultBell = day.defaultBell.sanitized()
            )
        }
    }

    private fun uniqueId(candidate: String, seen: MutableSet<String>): String {
        val id = if (safeId.matches(candidate) && candidate !in seen) candidate else UUID.randomUUID().toString()
        seen += id
        return id
    }

    private fun normalizeStart(seconds: Int): Int {
        val mod = seconds % SECONDS_PER_DAY
        return if (mod < 0) mod + SECONDS_PER_DAY else mod
    }
}
