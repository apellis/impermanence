package com.neversink.impermanence.data.datastore

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Keeps a copy of a days file that couldn't be read, before the default days replace it, so nothing is
 * lost silently. The app tells the person and offers to save the copy.
 */
object UnreadableDaysBackup {
    @Volatile var directory: File? = null

    private val _pending = MutableStateFlow<File?>(null)
    /** The kept copy the person hasn't been told about yet, or null. */
    val pending: StateFlow<File?> = _pending.asStateFlow()

    fun keep(bytes: ByteArray) {
        val dir = directory ?: return
        runCatching {
            dir.mkdirs()
            val file = File(dir, "days-unreadable-${System.currentTimeMillis()}.json")
            file.writeBytes(bytes)
            _pending.value = file
        }.onFailure { Log.e("UnreadableDaysBackup", "Could not keep a copy of unreadable days", it) }
    }

    fun dismiss() {
        _pending.value = null
    }
}
