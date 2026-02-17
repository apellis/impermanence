package com.neversink.impermanence

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.neversink.impermanence.data.datastore.dayDataStore
import com.neversink.impermanence.data.datastore.settingsDataStore
import com.neversink.impermanence.data.repository.DayRepository
import com.neversink.impermanence.data.repository.SettingsRepository
import com.neversink.impermanence.model.Day

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
