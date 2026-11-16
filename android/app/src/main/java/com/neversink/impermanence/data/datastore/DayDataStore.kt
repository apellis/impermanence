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
        // Unreadable saved days must never crash the app. Start with an empty list (docs/BEHAVIOR.md);
        // the serializer already kept a copy of the file for the person to save.
        Log.e("DayDataStore", "days.json is unreadable; starting with no days", exception)
        emptyList()
    }
)
