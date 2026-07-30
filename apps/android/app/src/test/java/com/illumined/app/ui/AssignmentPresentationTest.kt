package com.illumined.app.ui

import com.illumined.app.data.Assignment
import com.illumined.app.data.AssignmentLessonLink
import com.illumined.app.data.AssignmentReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssignmentPresentationTest {
    private fun assignment(id: String, lessons: List<AssignmentLessonLink> = emptyList(), readings: List<AssignmentReading> = emptyList()) =
        Assignment(id, "class", id, "", "", lessons, "", readings, true, null)

    @Test fun homePreviewMatchesIosFiveItemLimitAndRemainder() {
        val assignments = (1..7).map { assignment("a$it") }
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5"), homeAssignmentPreview(assignments).map { it.id })
        assertEquals(2, remainingHomeAssignmentCount(assignments))
        assertEquals(0, remainingHomeAssignmentCount(assignments.take(3)))
    }

    @Test fun summaryPrefersReadingCountThenFirstLessonLikeIos() {
        val lesson = AssignmentLessonLink("lesson-1", "The Trinity")
        assertEquals("2 readings", assignment("readings", listOf(lesson), listOf(AssignmentReading("1", "One", ""), AssignmentReading("2", "Two", ""))).homeContentLabel())
        assertEquals("The Trinity", assignment("lesson", listOf(lesson)).homeContentLabel())
        assertNull(assignment("empty").homeContentLabel())
    }
}
