package com.illumined.app.data

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Date

class ClassScheduleSelectionTest {
    private val utc = ZoneId.of("UTC")

    @Test fun emptyInstructorScheduleHasNoNextClass() {
        assertNull(ClassScheduleSelection.nextDay(emptyList(), Instant.parse("2026-07-22T12:00:00Z"), utc))
    }

    @Test fun onlyFutureInstructorSessionsAreShown() {
        fun item(id: String, instant: String, topic: String) =
            ScheduleItem(id, "class", topic, "", Timestamp(Date.from(Instant.parse(instant))))

        val schedule = listOf(
            item("past", "2026-07-20T00:00:00Z", "Past"),
            item("next", "2026-07-30T00:00:00Z", "Instructor's Next Class"),
            item("later", "2026-08-06T00:00:00Z", "Later Class"),
        )

        assertEquals(
            "Instructor's Next Class",
            ClassScheduleSelection.nextDay(schedule, Instant.parse("2026-07-22T12:00:00Z"), utc)?.sessions?.single()?.topic,
        )
    }

    @Test fun pastInstructorScheduleDoesNotCreateAFallbackClass() {
        val past = ScheduleItem(
            "past",
            "class",
            "Past",
            "",
            Timestamp(Date.from(Instant.parse("2026-07-20T00:00:00Z"))),
        )

        assertNull(ClassScheduleSelection.nextDay(listOf(past), Instant.parse("2026-07-22T12:00:00Z"), utc))
    }

    @Test fun repeatedDateReturnsEverySessionInImportedOrder() {
        fun item(id: String, topic: String, order: Long) = ScheduleItem(
            id,
            "class",
            topic,
            "",
            Timestamp(Date.from(Instant.parse("2027-02-14T00:00:00Z"))),
            order,
        )
        val schedule = listOf(
            item("commandments", "Introduction to the Ten Commandments & Commandments 1–3", 2),
            item("sending", "Rite of Sending", 0),
            item("election", "Rite of Election", 1),
        )

        val nextDay = ClassScheduleSelection.nextDay(
            schedule,
            Instant.parse("2027-02-10T12:00:00Z"),
            utc,
        )

        assertEquals(
            listOf(
                "Rite of Sending",
                "Rite of Election",
                "Introduction to the Ten Commandments & Commandments 1–3",
            ),
            nextDay?.sessions?.map { it.topic },
        )
    }

    @Test fun sessionsRemainVisibleOnTheirScheduledDay() {
        val today = ScheduleItem(
            "today",
            "class",
            "Today's Event",
            "",
            Timestamp(Date.from(Instant.parse("2027-02-14T00:00:00Z"))),
            0,
        )

        assertEquals(
            "Today's Event",
            ClassScheduleSelection.nextDay(
                listOf(today),
                Instant.parse("2027-02-14T12:00:00Z"),
                utc,
            )?.sessions?.single()?.topic,
        )
    }
}
