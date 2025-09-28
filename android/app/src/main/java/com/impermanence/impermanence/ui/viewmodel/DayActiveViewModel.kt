package com.impermanence.impermanence.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.impermanence.impermanence.domain.timer.DayTimerCoordinator
import com.impermanence.impermanence.domain.timer.DayTimerEngine
import com.impermanence.impermanence.model.Day
import com.impermanence.impermanence.service.DayTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DayActiveViewModel(
    application: Application,
    private val day: Day,
    private val initialLoopDays: Boolean
) : AndroidViewModel(application) {

    private val _timerState = MutableStateFlow(DayTimerEngine(day, initialLoopDays).evaluate().state)
    val timerState: StateFlow<DayTimerEngine.State> = _timerState.asStateFlow()

    init {
        DayTimerService.start(getApplication(), day, initialLoopDays)
        viewModelScope.launch {
            DayTimerCoordinator.state.collect { state ->
                state?.let { _timerState.value = it }
            }
        }
    }

    fun updateLoopDays(loopDays: Boolean) {
        DayTimerService.start(getApplication(), day, loopDays)
    }

    override fun onCleared() {
        DayTimerService.stop(getApplication())
        DayTimerCoordinator.clear()
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
