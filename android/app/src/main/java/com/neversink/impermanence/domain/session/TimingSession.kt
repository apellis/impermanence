package com.neversink.impermanence.domain.session

import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the app is timing right now. A session exists exactly while the person is on the active-day
 * screen or a running Quick Sit (iOS semantics: being on that screen means "we are timing"). It is
 * persisted in device-protected storage so bells survive process death and reboots.
 */
@Serializable
sealed class TimingSession {
    abstract val sessionId: String

    /** Bell events at or before this instant are handled (rung or deliberately skipped). */
    abstract val handledThroughMillis: Long

    @Serializable
    @SerialName("day")
    data class DaySession(
        override val sessionId: String,
        val day: Day,
        val loopDays: Boolean,
        /** ISO-8601 local date of the occurrence currently being timed. */
        val anchorDate: String,
        override val handledThroughMillis: Long
    ) : TimingSession()

    @Serializable
    @SerialName("quickSit")
    data class QuickSitSession(
        override val sessionId: String,
        val startMillis: Long,
        val endMillis: Long,
        val endBell: Bell,
        override val handledThroughMillis: Long
    ) : TimingSession()
}
