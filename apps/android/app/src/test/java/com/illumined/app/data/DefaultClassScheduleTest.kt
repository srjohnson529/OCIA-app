package com.illumined.app.data

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Date

class DefaultClassScheduleTest {
    private val utc = ZoneId.of("UTC")

    @Test fun catalogMatchesIosFiftySessionInventory() {
        assertEquals(50, DefaultClassSchedule.size)
        val next = DefaultClassSchedule.next(emptyList(), Instant.parse("2026-07-22T12:00:00Z"), utc)
        assertEquals("Introduction & The O.C.I.A.", next.topic)
        assertEquals(Instant.parse("2026-08-09T00:00:00Z"), next.date.toInstant())
    }

    @Test fun sameDaySessionIsNotUpcomingLikeIos() {
        val next = DefaultClassSchedule.next(emptyList(), Instant.parse("2026-08-09T12:00:00Z"), utc)
        assertEquals("Revelation — Scripture", next.topic)
    }

    @Test fun futureFirebaseSessionOverridesFallbackAndPastOneDoesNot() {
        fun item(id: String, instant: String, topic: String) = ScheduleItem(id, "class", topic, "", Timestamp(Date.from(Instant.parse(instant))))
        val remote = listOf(item("past", "2026-07-20T00:00:00Z", "Past"), item("future", "2026-07-30T00:00:00Z", "Custom Class"))
        assertEquals("Custom Class", DefaultClassSchedule.next(remote, Instant.parse("2026-07-22T12:00:00Z"), utc).topic)
        assertEquals("Introduction & The O.C.I.A.", DefaultClassSchedule.next(remote.take(1), Instant.parse("2026-07-22T12:00:00Z"), utc).topic)
    }
}
