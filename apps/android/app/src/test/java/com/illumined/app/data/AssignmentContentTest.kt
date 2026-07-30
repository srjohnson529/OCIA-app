package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentContentTest {
    @Test fun cleansMultipleLessonLinksAndReadings() {
        val content = AssignmentContent(
            listOf(AssignmentLessonLink(" lesson-1 ", " First "), AssignmentLessonLink("lesson-2", "Second")),
            listOf(AssignmentReading("reading-1", " Reading One ", " Full text "), AssignmentReading("reading-2", "Reading Two", "More text")),
        )
        assertFalse(content.hasPartialReading)
        assertEquals(listOf("lesson-1", "lesson-2"), content.cleanedLessonLinks.map { it.lessonId })
        assertEquals(listOf("Reading One", "Reading Two"), content.cleanedReadings.map { it.title })
    }

    @Test fun detectsEveryPartialReadingCombination() {
        assertTrue(AssignmentContent(emptyList(), listOf(AssignmentReading("1", "Title", ""))).hasPartialReading)
        assertTrue(AssignmentContent(emptyList(), listOf(AssignmentReading("1", "", "Text"))).hasPartialReading)
        assertFalse(AssignmentContent(emptyList(), listOf(AssignmentReading("1", "", ""))).hasPartialReading)
    }

    @Test fun resolvesOnlyCompleteTrimmedModernReadingsLikeIos() {
        val resolved = AssignmentContent(
            emptyList(),
            listOf(
                AssignmentReading("partial-title", "Title only", ""),
                AssignmentReading("partial-text", "", "Text only"),
                AssignmentReading("complete", " Complete title ", " Complete text "),
            ),
        ).resolvedReadings("Legacy", "Legacy text")

        assertEquals(listOf("complete"), resolved.map { it.id })
        assertEquals("Complete title", resolved.single().title)
        assertEquals("Complete text", resolved.single().text)
    }

    @Test fun legacyReadingRequiresBothTitleAndFullTextLikeIos() {
        val empty = AssignmentContent(emptyList(), emptyList())
        assertTrue(empty.resolvedReadings("Legacy title", "").isEmpty())
        assertTrue(empty.resolvedReadings("", "Legacy text").isEmpty())
        assertEquals(
            listOf(AssignmentReading("legacy-reading", "Legacy title", "Legacy text")),
            empty.resolvedReadings(" Legacy title ", " Legacy text "),
        )
    }
}
