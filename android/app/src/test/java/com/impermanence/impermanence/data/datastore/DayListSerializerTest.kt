package com.impermanence.impermanence.data.datastore

import androidx.datastore.core.CorruptionException
import com.impermanence.impermanence.model.Day
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DayListSerializerTest {

    @Test
    fun `blank input falls back to default days`() = runBlocking {
        val input = ByteArrayInputStream("".encodeToByteArray())

        val decoded = DayListSerializer.readFrom(input)

        assertEquals(2, decoded.size)
        assertEquals(Day.OpeningDay.name, decoded.first().name)
        assertEquals(Day.FullDay.name, decoded.last().name)
    }

    @Test
    fun `invalid json throws corruption exception`() = runBlocking {
        val input = ByteArrayInputStream("{not-json}".encodeToByteArray())

        try {
            DayListSerializer.readFrom(input)
            throw AssertionError("Expected CorruptionException for invalid JSON")
        } catch (error: Throwable) {
            assertTrue(error is CorruptionException)
        }
    }

    @Test
    fun `write and read preserves serialized days`() = runBlocking {
        val source = listOf(Day.OpeningDay)
        val output = ByteArrayOutputStream()

        DayListSerializer.writeTo(source, output)
        val decoded = DayListSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(1, decoded.size)
        assertEquals(source.first().name, decoded.first().name)
        assertEquals(source.first().segments.size, decoded.first().segments.size)
    }
}
