package com.impermanence.impermanence.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.impermanence.impermanence.model.Day
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object DayListSerializer : Serializer<List<Day>> {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val delegate = ListSerializer(Day.serializer())

    override val defaultValue: List<Day> = listOf(Day.OpeningDay, Day.FullDay)

    override suspend fun readFrom(input: InputStream): List<Day> = withContext(Dispatchers.IO) {
        try {
            val text = input.readBytes().decodeToString()
            if (text.isBlank()) return@withContext defaultValue
            json.decodeFromString(delegate, text)
        } catch (exception: SerializationException) {
            throw CorruptionException("Unable to read stored days", exception)
        }
    }

    override suspend fun writeTo(t: List<Day>, output: OutputStream) = withContext(Dispatchers.IO) {
        val text = json.encodeToString(delegate, t)
        output.write(text.encodeToByteArray())
    }
}
