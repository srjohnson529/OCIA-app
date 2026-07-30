package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Date
import java.util.TimeZone

class PrayerRequestPolicyTest {
    private data class Request(val id: String, val created: Long?, val expires: Long?)

    @Test fun keepsOnlyFiveNewestActiveRequests() {
        val values = (1L..7L).map { Request("$it", it, 100) } + Request("expired", 99, 10) + Request("missing", 100, null)
        val result = PrayerRequestPolicy.recentActive(values, 50, { it.expires }, { it.created })
        assertEquals(listOf("7", "6", "5", "4", "3"), result.map { it.id })
    }

    @Test fun lifetimeIsExactlyThreeDays() {
        assertEquals(259_200_000L, PrayerRequestPolicy.LIFETIME_MILLIS)
    }

    @Test fun expirationUsesThreeLocalCalendarDaysAcrossDaylightSaving() {
        val start = Date.from(Instant.parse("2026-03-07T17:00:00Z"))
        val expiration = PrayerRequestPolicy.expirationDate(start, TimeZone.getTimeZone("America/New_York"))
        assertEquals(Instant.parse("2026-03-10T16:00:00Z"), expiration.toInstant())
        assertTrue(expiration.time - start.time < PrayerRequestPolicy.LIFETIME_MILLIS)
    }

    @Test fun creationValidationMatchesIosOrderAndCopy() {
        assertEquals("Please sign in before posting a prayer request.", PrayerRequestPolicy.creationError(null, "", ""))
        assertEquals("Please add a title for the prayer request.", PrayerRequestPolicy.creationError("user", " ", ""))
        assertEquals("Please join a class before posting a prayer request.", PrayerRequestPolicy.creationError("user", "Healing", ""))
        assertEquals(null, PrayerRequestPolicy.creationError("user", "Healing", "OCIA"))
    }
}
