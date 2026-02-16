package com.impermanence.impermanence.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.impermanence.impermanence.data.repository.DayRepository
import com.impermanence.impermanence.data.repository.SettingsRepository
import com.impermanence.impermanence.model.Day
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AppUiState(
    val days: List<Day> = emptyList(),
    val use24HourClock: Boolean = false,
    val loopDays: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class AppViewModel(
    private val dayRepository: DayRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dayRepository.days,
                settingsRepository.use24HourClock,
                settingsRepository.loopDays
            ) { days, use24, loop ->
                AppUiState(
                    days = days,
                    use24HourClock = use24,
                    loopDays = loop,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addDay(day: Day) {
        viewModelScope.launch {
            dayRepository.addDay(day)
        }
    }

    fun updateDay(day: Day) {
        viewModelScope.launch {
            dayRepository.updateDay(day)
        }
    }

    fun overwriteDays(days: List<Day>) {
        viewModelScope.launch {
            dayRepository.overwrite(days)
        }
    }

    fun deleteDay(dayId: String) {
        viewModelScope.launch {
            dayRepository.deleteDay(dayId)
        }
    }

    fun moveDay(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            dayRepository.moveDay(fromIndex, toIndex)
        }
    }

    fun setUse24HourClock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUse24HourClock(enabled)
        }
    }

    fun setLoopDays(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLoopDays(enabled)
        }
    }

    fun duplicateDay(dayId: String) {
        val currentDay = _uiState.value.days.firstOrNull { it.id == dayId } ?: return
        val copy = currentDay.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${currentDay.name} Copy"
        )
        addDay(copy)
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun provideFactory(
            dayRepository: DayRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                    return AppViewModel(dayRepository, settingsRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
