package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class FirestoreDatePolicyTest {
    @Test fun instructorDatesAreWrittenAtLocalStartOfDayLikeIos() {
        val input = Instant.parse("2026-07-22T19:38:45Z").toEpochMilli()
        val result = FirestoreDatePolicy.localStartOfDayMillis(input, TimeZone.getTimeZone("America/New_York"))
        assertEquals(Instant.parse("2026-07-22T04:00:00Z").toEpochMilli(), result)
    }

    @Test fun localMidnightUsesTheCorrectDaylightSavingOffset() {
        val input = Instant.parse("2026-12-22T19:38:45Z").toEpochMilli()
        val result = FirestoreDatePolicy.localStartOfDayMillis(input, TimeZone.getTimeZone("America/New_York"))
        assertEquals(Instant.parse("2026-12-22T05:00:00Z").toEpochMilli(), result)
    }
}
