package com.neversink.impermanence.data.portability

import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.model.DaySanitizer
import com.neversink.impermanence.model.Theme
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.util.UUID
import kotlin.math.roundToInt

@Serializable
data class PortableDayPlanFile(
    val version: Int = CURRENT_VERSION,
    val days: List<Day>
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * Reads and writes day plans. Import accepts the portable envelope `{ "version": 1, "days": [...] }`, a
 * bare list of days, and the iOS app's native `[Day]` file (`startTime`, `duration`, `endBell` keys).
 * Unknown themes become teal and every value is sanitized, so a hand-edited file can't break the app.
 */
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
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull()
            ?: throw SerializationException("Invalid day plan file format")
        val list = when (root) {
            is JsonObject -> root["days"] as? JsonArray
            is JsonArray -> root
            else -> null
        } ?: throw SerializationException("Invalid day plan file format")
        val days = list.map { element ->
            (element as? JsonObject)?.let(::dayFrom) ?: throw SerializationException("Invalid day in file")
        }
        return sanitize(days)
    }

    private fun dayFrom(obj: JsonObject): Day {
        val name = obj.string("name") ?: throw SerializationException("Day has no name")
        val start = obj.number("startTimeSeconds") ?: obj.number("startTime") ?: 0.0
        val segments = (obj["segments"] as? JsonArray)?.map { element ->
            val seg = element as? JsonObject ?: throw SerializationException("Invalid segment in file")
            Day.Segment(
                id = seg.string("id") ?: UUID.randomUUID().toString(),
                name = seg.string("name") ?: "",
                durationSeconds = (seg.number("durationSeconds") ?: seg.number("duration") ?: 0.0).roundSafely(),
                customEndBell = seg.bell("customEndBell") ?: seg.bell("endBell")
            )
        } ?: emptyList()
        val decodedDefault = obj.bell("defaultBell")
        val decodedManual = obj.bell("manualBell")
        val decodedStart = obj.bell("startBell")
        val defaultBell = decodedDefault ?: decodedManual ?: decodedStart ?: Bell.SingleBell
        return Day(
            id = obj.string("id") ?: UUID.randomUUID().toString(),
            name = name,
            startTimeSeconds = start.roundSafely(),
            segments = segments,
            startBell = decodedStart ?: defaultBell,
            manualBell = decodedManual ?: defaultBell,
            defaultBell = defaultBell,
            theme = themeFrom(obj.string("theme"))
        )
    }

    private fun themeFrom(name: String?): Theme =
        Theme.values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Theme.TEAL

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull

    private fun JsonObject.bell(key: String): Bell? {
        val obj = this[key] as? JsonObject ?: return null
        val soundId = (obj["soundId"] as? JsonPrimitive)?.intOrNull ?: return null
        val rings = (obj["numRings"] as? JsonPrimitive)?.intOrNull ?: return null
        return Bell(soundId = soundId, numRings = rings)
    }

    private fun Double.roundSafely(): Int = when {
        isNaN() -> 0
        this > Int.MAX_VALUE -> Int.MAX_VALUE
        this < Int.MIN_VALUE -> Int.MIN_VALUE
        else -> roundToInt()
    }

    private fun sanitize(days: List<Day>): List<Day> = DaySanitizer.sanitize(days)
}
