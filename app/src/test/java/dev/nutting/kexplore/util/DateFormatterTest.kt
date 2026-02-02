package dev.nutting.kexplore.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DateFormatterTest {

    @Test
    fun `age returns Unknown for null timestamp`() {
        assertEquals("Unknown", DateFormatter.age(null))
    }

    @Test
    fun `age returns Unknown for malformed timestamp`() {
        assertEquals("Unknown", DateFormatter.age("not-a-date"))
    }

    @Test
    fun `age returns seconds for recent timestamp`() {
        val now = Instant.now().minusSeconds(30).toString()
        val result = DateFormatter.age(now)
        assertEquals("30s", result)
    }

    @Test
    fun `age returns minutes for recent timestamp`() {
        val ts = Instant.now().minusSeconds(300).toString()
        val result = DateFormatter.age(ts)
        assertEquals("5m", result)
    }

    @Test
    fun `age returns hours for timestamp`() {
        val ts = Instant.now().minusSeconds(7200).toString()
        val result = DateFormatter.age(ts)
        assertEquals("2h", result)
    }

    @Test
    fun `age returns days for timestamp`() {
        val ts = Instant.now().minusSeconds(172800).toString()
        val result = DateFormatter.age(ts)
        assertEquals("2d", result)
    }

    @Test
    fun `age returns years for old timestamp`() {
        val ts = Instant.now().minusSeconds(400L * 24 * 3600).toString()
        val result = DateFormatter.age(ts)
        assertEquals("1y", result)
    }

    @Test
    fun `age returns 0s for just-now timestamp`() {
        val now = Instant.now().toString()
        val result = DateFormatter.age(now)
        assertEquals("0s", result)
    }
}
