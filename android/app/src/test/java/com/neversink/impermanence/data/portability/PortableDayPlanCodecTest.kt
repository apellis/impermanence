package com.neversink.impermanence.data.portability

import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.model.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableDayPlanCodecTest {

    @Test
    fun `round trip preserves day plans`() {
        val source = listOf(Day.OpeningDay, Day.FullDay)

        val encoded = PortableDayPlanCodec.encode(source)
        val decoded = PortableDayPlanCodec.decode(encoded)

        assertEquals(source.size, decoded.size)
        assertEquals(source.first().name, decoded.first().name)
        assertEquals(source.last().segments.size, decoded.last().segments.size)
        assertEquals(source.last().theme, decoded.last().theme)
    }

    @Test
    fun `decode sanitizes invalid durations and bell rings`() {
        val payload = """
            {
              "version": 1,
              "days": [
                {
                  "id": "11f9fdab-08ec-4ce4-9037-6ecdb057737f",
                  "name": "Imported",
                  "startTimeSeconds": -120,
                  "segments": [
                    {
                      "id": "8f5259dd-e5a8-4f00-bff7-fd8d5ed5f51b",
                      "name": "Sit",
                      "durationSeconds": 0,
                      "customEndBell": {
                        "soundId": 0,
                        "numRings": 0
                      }
                    }
                  ],
                  "startBell": { "soundId": 0, "numRings": 0 },
                  "manualBell": { "soundId": 0, "numRings": 0 },
                  "defaultBell": { "soundId": 0, "numRings": 0 },
                  "theme": "teal"
                }
              ]
            }
        """.trimIndent()

        val decoded = PortableDayPlanCodec.decode(payload)
        val day = decoded.first()

        assertEquals(1, decoded.size)
        assertEquals(24 * 60 * 60 - 120, day.startTimeSeconds)
        assertEquals(1, day.segments.first().durationSeconds)
        assertEquals(1, day.startBell.numRings)
        assertEquals(1, day.manualBell.numRings)
        assertEquals(1, day.defaultBell.numRings)
        assertEquals(Theme.TEAL, day.theme)
        assertEquals(1, day.segments.first().customEndBell?.numRings)
    }

    @Test
    fun `decode supports legacy list payload`() {
        val day = Day(
            id = "legacy-day",
            name = "Legacy Day",
            startTimeSeconds = 0,
            segments = listOf(
                Day.Segment(
                    id = "legacy-segment",
                    name = "Sit",
                    durationSeconds = 60,
                    customEndBell = Bell.SingleBell
                )
            ),
            startBell = Bell.SingleBell,
            manualBell = Bell.SingleBell,
            defaultBell = Bell.SingleBell,
            theme = Theme.TEAL
        )
        val legacyJson = """
            [
              {
                "id": "${day.id}",
                "name": "${day.name}",
                "startTimeSeconds": ${day.startTimeSeconds},
                "segments": [
                  {
                    "id": "${day.segments.first().id}",
                    "name": "${day.segments.first().name}",
                    "durationSeconds": ${day.segments.first().durationSeconds},
                    "customEndBell": { "soundId": 0, "numRings": 1 }
                  }
                ],
                "startBell": { "soundId": 0, "numRings": 1 },
                "manualBell": { "soundId": 0, "numRings": 1 },
                "defaultBell": { "soundId": 0, "numRings": 1 },
                "theme": "teal"
              }
            ]
        """.trimIndent()

        val decoded = PortableDayPlanCodec.decode(legacyJson)

        assertEquals(1, decoded.size)
        assertEquals("Legacy Day", decoded.first().name)
        assertTrue(decoded.first().segments.isNotEmpty())
    }
}
