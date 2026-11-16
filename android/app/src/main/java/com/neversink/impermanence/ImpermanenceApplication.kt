package com.neversink.impermanence

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.neversink.impermanence.data.datastore.dayDataStore
import com.neversink.impermanence.data.datastore.settingsDataStore
import com.neversink.impermanence.data.repository.DayRepository
import com.neversink.impermanence.data.repository.SettingsRepository
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.service.SessionEngine
import com.neversink.impermanence.service.TimingService

class ImpermanenceApplication : Application() {
    /** Created on first use: the app can start in direct boot, before credential storage is readable. */
    val container: AppContainer by lazy {
        AppContainer(
            dayDataStore = dayDataStore,
            settingsDataStore = settingsDataStore
        )
    }

    override fun onCreate() {
        super.onCreate()
        TimingService.createNotificationChannel(this)
        SessionEngine.initialize(this)
        // Re-arm the bell alarm on every process start (an alarm can be lost to a force stop or update).
        runCatching {
            SessionEngine.process(this, SessionEngine.Trigger.APP_START, allowServiceStart = false)
        }
    }
}

class AppContainer(
    dayDataStore: DataStore<List<Day>>,
    settingsDataStore: DataStore<Preferences>
) {
    val dayRepository = DayRepository(dayDataStore)
    val settingsRepository = SettingsRepository(settingsDataStore)
}
