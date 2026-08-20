package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleImportParserTest {
    @Test fun parsesHeaderCsvAndSortsDates() {
        val result = ScheduleImportParser.parse("date,topic,details\n9/10/2026,Prayer,Evening prayer\n2026-08-20,Creed,The profession of faith")
        assertTrue(result is ScheduleParseResult.Success)
        val rows = (result as ScheduleParseResult.Success).rows
        assertEquals(listOf(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 10)), rows.map { it.date })
        assertEquals("The profession of faith", rows.first().details)
    }

    @Test fun parsesTabsAndQuotedCommas() {
        val tabs = ScheduleImportParser.parse("9/3/2026\tIntroduction\tWelcome night") as ScheduleParseResult.Success
        assertEquals("Introduction", tabs.rows.single().topic)
        val csv = ScheduleImportParser.parse("2026-09-03,Introduction,\"Welcome, prayer, and tour\"") as ScheduleParseResult.Success
        assertEquals("Welcome, prayer, and tour", csv.rows.single().details)
    }

    @Test fun preservesSpreadsheetOrderForRepeatedDates() {
        val result = ScheduleImportParser.parse(
            "2/10/2027\tAsh Wednesday\n" +
                "2/14/2027\tRite of Sending\n" +
                "2/14/2027\tRite of Election\n" +
                "2/14/2027\tIntroduction to the Ten Commandments & Commandments 1–3",
        ) as ScheduleParseResult.Success

        assertEquals(
            listOf(
                "Ash Wednesday",
                "Rite of Sending",
                "Rite of Election",
                "Introduction to the Ten Commandments & Commandments 1–3",
            ),
            result.rows.map { it.topic },
        )
    }

    @Test fun reportsOriginalRowNumberForBadDate() {
        val result = ScheduleImportParser.parse("date,topic\nnot-a-date,Creed")
        assertTrue(result is ScheduleParseResult.Failure)
        assertTrue((result as ScheduleParseResult.Failure).message.contains("Row 2"))
    }

    @Test fun rejectsMissingTopicAndEmptyInput() {
        assertTrue(ScheduleImportParser.parse("2026-09-03,") is ScheduleParseResult.Failure)
        assertTrue(ScheduleImportParser.parse("  \n") is ScheduleParseResult.Failure)
    }
}
