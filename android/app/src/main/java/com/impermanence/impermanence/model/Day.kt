package com.impermanence.impermanence.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Day(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var startTimeSeconds: Int,
    var segments: List<Segment>,
    var startBell: Bell = Bell.SingleBell,
    var manualBell: Bell = Bell.SingleBell,
    var defaultBell: Bell = Bell.SingleBell,
    var theme: Theme = Theme.BUBBLEGUM
) {
    val startEndTimeSeconds: Pair<Int, Int>
        get() {
            val end = segments.fold(startTimeSeconds) { acc, segment -> acc + segment.durationSeconds }
            return startTimeSeconds to end
        }

    fun segmentSchedule(): List<Pair<Int, Int>> {
        var cursor = startTimeSeconds
        return segments.map { segment ->
            val start = cursor
            val end = start + segment.durationSeconds
            cursor = end
            start to end
        }
    }

    fun currentSegment(now: Long): Pair<Segment, Long>? {
        val nowSeconds = ((now / 1000L) % 86400).toInt()
        segmentSchedule().zip(segments).firstOrNull { (schedule, _) ->
            nowSeconds in schedule.first until schedule.second
        }?.let { (schedule, segment) ->
            val remaining = schedule.second - nowSeconds
            return segment to remaining.toLong()
        }
        return null
    }

    fun copyWith(segments: List<Segment> = this.segments): Day = copy(segments = segments)

    @Serializable
    data class Segment(
        val id: String = UUID.randomUUID().toString(),
        var name: String,
        var durationSeconds: Int,
        var customEndBell: Bell? = null
    ) {
        fun resolvedBell(defaultBell: Bell): Bell = customEndBell ?: defaultBell

        fun useDefaultBell(): Segment = copy(customEndBell = null)

        fun withCustomBell(bell: Bell): Segment = copy(customEndBell = bell)
    }

    companion object {
        val Empty = Day(
            name = "",
            startTimeSeconds = 0,
            segments = emptyList(),
            startBell = Bell.SingleBell,
            manualBell = Bell.SingleBell,
            defaultBell = Bell.SingleBell,
            theme = Theme.TEAL
        )

        val OpeningDay = Day(
            name = "Opening Day",
            startTimeSeconds = 18 * 60 * 60,
            segments = listOf(
                Segment(name = "Sit", durationSeconds = 45 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Dharma Talk", durationSeconds = 90 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Sit", durationSeconds = 45 * 60)
            ),
            startBell = Bell.SingleBell,
            manualBell = Bell.SingleBell,
            defaultBell = Bell.SingleBell,
            theme = Theme.ORANGE
        )

        val FullDay = Day(
            name = "Full Day",
            startTimeSeconds = 7 * 60 * 60,
            segments = listOf(
                Segment(name = "Wake", durationSeconds = 15 * 60),
                Segment(name = "Sit", durationSeconds = 45 * 60),
                Segment(name = "Eat", durationSeconds = 45 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Sit", durationSeconds = 45 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Sit", durationSeconds = 60 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Sit", durationSeconds = 45 * 60),
                Segment(name = "Eat", durationSeconds = 45 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Sit", durationSeconds = 45 * 60),
                Segment(name = "Exercise", durationSeconds = 45 * 60),
                Segment(name = "Sit", durationSeconds = 60 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Sit", durationSeconds = 45 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Eat", durationSeconds = 45 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Dharma Talk", durationSeconds = 90 * 60),
                Segment(name = "Walk", durationSeconds = 30 * 60),
                Segment(name = "Sit", durationSeconds = 45 * 60)
            ),
            startBell = Bell.SingleBell,
            manualBell = Bell.SingleBell,
            defaultBell = Bell.SingleBell,
            theme = Theme.POPPY
        )
    }
}
