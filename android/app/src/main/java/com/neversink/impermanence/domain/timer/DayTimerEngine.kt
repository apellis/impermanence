package com.neversink.impermanence.domain.timer

import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

class DayTimerEngine(
    private val day: Day,
    loopDays: Boolean
) {
    data class SegmentSchedule(
        val index: Int,
        val segment: Day.Segment,
        val startEpochSecond: Long,
        val endEpochSecond: Long
    )

    data class State(
        val segmentIndex: Int,
        val activeSegmentName: String,
        val activeSegmentTimeElapsed: Long,
        val activeSegmentTimeRemaining: Long,
        val status: TimerStatus,
        val schedules: List<SegmentSchedule>
    )

    enum class TimerStatus { EMPTY, NOT_STARTED, ACTIVE, COMPLETE }

    data class Evaluation(
        val state: State,
        val bell: Bell?
    )

    private var loopDaysInternal: Boolean = loopDays
    private var baseDate: LocalDate = LocalDate.now()
    private var schedules: List<SegmentSchedule> = buildSchedules(baseDate)
    private var lastSegmentIndex: Int = Int.MIN_VALUE
    private var freshTimer: Boolean = true

    fun updateLoopDays(loopDays: Boolean) {
        loopDaysInternal = loopDays
    }

    fun evaluate(now: Instant = Instant.now()): Evaluation {
        val zone = ZoneId.systemDefault()
        val nowDate = LocalDate.ofInstant(now, zone)
        if (loopDaysInternal && nowDate.isAfter(baseDate)) {
            baseDate = nowDate
            schedules = buildSchedules(baseDate)
            freshTimer = true
            lastSegmentIndex = Int.MIN_VALUE
        }

        val evaluation = buildState(now)
        val bell = resolveBell(evaluation.state.segmentIndex)
        lastSegmentIndex = evaluation.state.segmentIndex
        freshTimer = false
        return evaluation.copy(bell = bell)
    }

    fun stream(intervalMillis: Long = 500L): Flow<Evaluation> = flow {
        while (true) {
            emit(evaluate())
            kotlinx.coroutines.delay(intervalMillis)
        }
    }

    private fun buildSchedules(date: LocalDate): List<SegmentSchedule> {
        if (day.segments.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().epochSecond
        var cursor = startOfDay + day.startTimeSeconds
        return day.segments.mapIndexed { index, segment ->
            val start = cursor
            val end = start + max(1, segment.durationSeconds)
            cursor = end
            SegmentSchedule(index, segment, start, end)
        }
    }

    private fun buildState(now: Instant): Evaluation {
        val schedulesSnapshot = schedules
        if (schedulesSnapshot.isEmpty()) {
            return Evaluation(
                state = State(
                    segmentIndex = -2,
                    activeSegmentName = "(Day is empty)",
                    activeSegmentTimeElapsed = -1,
                    activeSegmentTimeRemaining = -1,
                    status = TimerStatus.EMPTY,
                    schedules = emptyList()
                ),
                bell = null
            )
        }

        val nowEpoch = now.epochSecond
        val first = schedulesSnapshot.first()
        val last = schedulesSnapshot.last()

        val (index, status) = when {
            nowEpoch < first.startEpochSecond -> -1 to TimerStatus.NOT_STARTED
            nowEpoch >= last.endEpochSecond -> schedulesSnapshot.size to TimerStatus.COMPLETE
            else -> {
                val match = schedulesSnapshot.firstOrNull { nowEpoch in it.startEpochSecond until it.endEpochSecond }
                if (match != null) match.index to TimerStatus.ACTIVE else last.index + 1 to TimerStatus.COMPLETE
            }
        }

        val (elapsed, remaining) = if (index in schedulesSnapshot.indices) {
            val schedule = schedulesSnapshot[index]
            val elapsedSeconds = nowEpoch - schedule.startEpochSecond
            val remainingSeconds = schedule.endEpochSecond - nowEpoch
            elapsedSeconds to remainingSeconds
        } else {
            -1L to -1L
        }

        val segmentName = when {
            status == TimerStatus.EMPTY -> "(Day is empty)"
            status == TimerStatus.NOT_STARTED -> "(Day has not started yet)"
            status == TimerStatus.COMPLETE -> "(Day is over)"
            else -> "Now: ${schedulesSnapshot[index].segment.name}"
        }

        return Evaluation(
            state = State(
                segmentIndex = index,
                activeSegmentName = segmentName,
                activeSegmentTimeElapsed = elapsed,
                activeSegmentTimeRemaining = remaining,
                status = status,
                schedules = schedulesSnapshot
            ),
            bell = null
        )
    }

    private fun resolveBell(newIndex: Int): Bell? {
        val previous = lastSegmentIndex
        if (newIndex == previous) return null
        val schedulesSnapshot = schedules
        if (schedulesSnapshot.isEmpty()) return null

        return when {
            newIndex == 0 && previous < 0 -> day.startBell
            previous in schedulesSnapshot.indices -> schedulesSnapshot[previous].segment.resolvedBell(day.defaultBell)
            else -> null
        }.takeIf { !freshTimer }
    }
}
