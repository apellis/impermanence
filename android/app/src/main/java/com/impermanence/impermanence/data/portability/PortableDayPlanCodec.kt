package com.impermanence.impermanence.data.portability

import com.impermanence.impermanence.model.Bell
import com.impermanence.impermanence.model.Day
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.max

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

    private fun sanitize(days: List<Day>): List<Day> {
        return days.map { day ->
            day.copy(
                startTimeSeconds = normalizeDaySeconds(day.startTimeSeconds),
                segments = day.segments.map { segment ->
                    segment.copy(
                        durationSeconds = max(1, segment.durationSeconds),
                        customEndBell = segment.customEndBell?.sanitize()
                    )
                },
                startBell = day.startBell.sanitize(),
                manualBell = day.manualBell.sanitize(),
                defaultBell = day.defaultBell.sanitize()
            )
        }
    }

    private fun Bell.sanitize(): Bell {
        return copy(numRings = numRings.coerceAtLeast(1))
    }

    private fun normalizeDaySeconds(seconds: Int): Int {
        val mod = seconds % SECONDS_PER_DAY
        return if (mod < 0) mod + SECONDS_PER_DAY else mod
    }

    private const val SECONDS_PER_DAY = 24 * 60 * 60
}
