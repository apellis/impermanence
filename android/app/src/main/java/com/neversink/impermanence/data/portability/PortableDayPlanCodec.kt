package com.neversink.impermanence.data.portability

import com.neversink.impermanence.model.Day
import com.neversink.impermanence.model.DaySanitizer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class PortableDayPlanFile(
    val version: Int = CURRENT_VERSION,
    val days: List<Day>
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

object PortableDayPlanCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(days: List<Day>): String {
        val payload = PortableDayPlanFile(days = sanitize(days))
        return json.encodeToString(PortableDayPlanFile.serializer(), payload)
    }

    @Throws(SerializationException::class)
    fun decode(text: String): List<Day> {
        val fromEnvelope = runCatching {
            json.decodeFromString(PortableDayPlanFile.serializer(), text).days
        }.getOrNull()
        if (fromEnvelope != null) {
            return sanitize(fromEnvelope)
        }

        val delegate = ListSerializer(Day.serializer())
        val fromLegacyList = runCatching {
            json.decodeFromString(delegate, text)
        }.getOrNull()
        if (fromLegacyList != null) {
            return sanitize(fromLegacyList)
        }

        throw SerializationException("Invalid day plan file format")
    }

    private fun sanitize(days: List<Day>): List<Day> = DaySanitizer.sanitize(days)
}
