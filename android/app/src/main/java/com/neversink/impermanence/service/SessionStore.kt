package com.neversink.impermanence.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.neversink.impermanence.domain.session.TimingSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persists the active [TimingSession] in device-protected storage, so it can be read after a reboot
 * before the user unlocks the phone (direct boot). Writes are synchronous ([SharedPreferences.Editor.commit])
 * so a bell marked as handled is never rung again after a crash.
 */
class SessionStore(context: Context) {
    private val prefs: SharedPreferences = storageContext(context)
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): TimingSession? {
        val text = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString(TimingSession.serializer(), text) }
            .onFailure {
                Log.e(LOG_TAG, "Discarding unreadable session", it)
                clear()
            }
            .getOrNull()
    }

    fun save(session: TimingSession) {
        prefs.edit().putString(KEY_SESSION, json.encodeToString(TimingSession.serializer(), session)).commit()
    }

    fun clear() {
        prefs.edit().remove(KEY_SESSION).remove(KEY_SCHEDULED_ALARM).commit()
    }

    var scheduledAlarmMillis: Long
        get() = prefs.getLong(KEY_SCHEDULED_ALARM, 0L)
        set(value) {
            prefs.edit().putLong(KEY_SCHEDULED_ALARM, value).commit()
        }

    companion object {
        private const val LOG_TAG = "SessionStore"
        private const val PREFS_NAME = "timing_session"
        private const val KEY_SESSION = "session"
        private const val KEY_SCHEDULED_ALARM = "scheduled_alarm_millis"

        internal val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun storageContext(context: Context): Context =
            context.applicationContext.createDeviceProtectedStorageContext() ?: context.applicationContext
    }
}

/** Last [MAX_ENTRIES] bell decisions, for diagnostics ("did the 7:00 bell ring?") and device tests. */
class RingLog(context: Context) {
    @Serializable
    data class Entry(
        val atMillis: Long,
        val key: String,
        val scheduledAtMillis: Long,
        val outcome: String,
        val trigger: String
    )

    private val prefs = SessionStore.storageContext(context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val serializer = ListSerializer(Entry.serializer())

    fun entries(): List<Entry> {
        val text = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { SessionStore.json.decodeFromString(serializer, text) }.getOrDefault(emptyList())
    }

    fun add(entry: Entry) {
        val updated = (entries() + entry).takeLast(MAX_ENTRIES)
        prefs.edit().putString(KEY, SessionStore.json.encodeToString(serializer, updated)).apply()
        Log.i("RingLog", "${entry.outcome} ${entry.key} scheduled=${entry.scheduledAtMillis} at=${entry.atMillis} via ${entry.trigger}")
    }

    fun clear() {
        prefs.edit().remove(KEY).commit()
    }

    companion object {
        private const val PREFS_NAME = "ring_log"
        private const val KEY = "entries"
        const val MAX_ENTRIES = 100
    }
}
