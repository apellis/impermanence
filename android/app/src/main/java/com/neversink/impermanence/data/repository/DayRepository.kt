package com.neversink.impermanence.data.repository

import androidx.datastore.core.DataStore
import com.neversink.impermanence.model.Day
import kotlinx.coroutines.flow.Flow

class DayRepository(
    private val dataStore: DataStore<List<Day>>
) {
    val days: Flow<List<Day>> = dataStore.data

    suspend fun overwrite(days: List<Day>) {
        dataStore.updateData { days }
    }

    suspend fun addDay(day: Day) {
        dataStore.updateData { current -> current + day }
    }

    suspend fun updateDay(updatedDay: Day) {
        dataStore.updateData { current ->
            current.map { existing -> if (existing.id == updatedDay.id) updatedDay else existing }
        }
    }

    suspend fun deleteDay(id: String) {
        dataStore.updateData { current -> current.filterNot { it.id == id } }
    }

    suspend fun moveDay(fromIndex: Int, toIndex: Int) {
        dataStore.updateData { current ->
            val mutable = current.toMutableList()
            if (fromIndex in mutable.indices) {
                val item = mutable.removeAt(fromIndex)
                val destination = toIndex.coerceIn(0, mutable.size)
                mutable.add(destination, item)
            }
            mutable
        }
    }
}
