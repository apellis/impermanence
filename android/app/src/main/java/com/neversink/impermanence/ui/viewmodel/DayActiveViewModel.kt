package com.neversink.impermanence.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neversink.impermanence.domain.session.TimingSession
import com.neversink.impermanence.domain.timer.DaySchedule
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.service.SessionEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Owns the timing session for the active-day screen. Being on this screen *is* timing the day: the
 * session starts (or resumes, after process death) when the screen opens and ends when the screen's
 * back stack entry is destroyed, that is, when the person leaves the screen.
 */
class DayActiveViewModel(
    application: Application,
    private val day: Day,
    loopDays: Boolean
) : AndroidViewModel(application) {
    private val _displayState = MutableStateFlow(computeState())
    val displayState: StateFlow<DaySchedule.DisplayState> = _displayState.asStateFlow()

    /** True once the session this screen owns was ended from elsewhere (notification Stop, swipe away). */
    private val _endedExternally = MutableStateFlow(false)
    val endedExternally: StateFlow<Boolean> = _endedExternally.asStateFlow()

    private var sessionId: String? = null

    init {
        SessionEngine.startDay(application, day, loopDays)
        sessionId = SessionEngine.session.value?.sessionId
        viewModelScope.launch {
            SessionEngine.session.collect { session ->
                val ours = sessionId
                if (ours != null && session?.sessionId != ours) _endedExternally.value = true
            }
        }
        viewModelScope.launch {
            while (isActive) {
                _displayState.value = computeState()
                val now = SessionEngine.now()
                delay((1_000L - now % 1_000L).coerceIn(50L, 1_000L))
            }
        }
    }

    fun updateLoopDays(loopDays: Boolean) {
        SessionEngine.setLoopDays(getApplication(), loopDays)
    }

    private fun computeState(): DaySchedule.DisplayState {
        val now = SessionEngine.now()
        val zone = SessionEngine.zone()
        val session = SessionEngine.session.value as? TimingSession.DaySession
        val date = if (session != null && session.day.id == day.id) {
            DaySchedule.effectiveDate(day, LocalDate.parse(session.anchorDate), now, zone, session.loopDays)
        } else {
            DaySchedule.initialDate(day, now, zone)
        }
        return DaySchedule.displayState(day, DaySchedule.occurrence(day, date, zone), now)
    }

    override fun onCleared() {
        sessionId?.let { SessionEngine.endIfCurrent(getApplication(), it, SessionEngine.EndReason.LEFT_SCREEN) }
        super.onCleared()
    }

    companion object {
        fun provideFactory(
            application: Application,
            day: Day,
            loopDays: Boolean
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DayActiveViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return DayActiveViewModel(application, day, loopDays) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
