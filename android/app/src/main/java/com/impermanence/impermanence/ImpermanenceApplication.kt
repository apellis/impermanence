package com.impermanence.impermanence

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.impermanence.impermanence.data.datastore.dayDataStore
import com.impermanence.impermanence.data.datastore.settingsDataStore
import com.impermanence.impermanence.data.repository.DayRepository
import com.impermanence.impermanence.data.repository.SettingsRepository
import com.impermanence.impermanence.model.Day

class ImpermanenceApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            dayDataStore = dayDataStore,
            settingsDataStore = settingsDataStore
        )
    }
}

class AppContainer(
    dayDataStore: DataStore<List<Day>>,
    settingsDataStore: DataStore<Preferences>
) {
    val dayRepository = DayRepository(dayDataStore)
    val settingsRepository = SettingsRepository(settingsDataStore)
}
