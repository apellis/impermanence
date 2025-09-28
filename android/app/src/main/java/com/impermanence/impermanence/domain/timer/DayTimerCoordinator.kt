package com.impermanence.impermanence.domain.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DayTimerCoordinator {
    private val _state = MutableStateFlow<DayTimerEngine.State?>(null)
    val state: StateFlow<DayTimerEngine.State?> = _state.asStateFlow()

    fun update(state: DayTimerEngine.State) {
        _state.value = state
    }

    fun clear() {
        _state.value = null
    }
}
