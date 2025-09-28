package com.impermanence.impermanence.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.impermanence.impermanence.model.Day

val Context.dayDataStore: DataStore<List<Day>> by dataStore(
    fileName = "days.json",
    serializer = DayListSerializer
)
