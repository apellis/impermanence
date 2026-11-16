package com.neversink.impermanence.data.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.neversink.impermanence.model.Day

val Context.dayDataStore: DataStore<List<Day>> by dataStore(
    fileName = "days.json",
    serializer = DayListSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { exception ->
        // Unreadable saved days must never crash the app: fall back to the seed days.
        Log.e("DayDataStore", "days.json is unreadable; restoring default days", exception)
        DayListSerializer.defaultValue
    }
)
