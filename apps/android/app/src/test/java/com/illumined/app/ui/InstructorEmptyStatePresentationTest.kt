package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InstructorEmptyStatePresentationTest {
    @Test
    fun `manager empty-state copy matches iOS`() {
        assertEquals("No Announcements", InstructorEmptyStatePresentation.announcements.title)
        assertEquals("No Schedule Items", InstructorEmptyStatePresentation.schedule.title)
        assertEquals("No Assignments", InstructorEmptyStatePresentation.assignments.title)
        assertEquals("No Discussion Boards", InstructorEmptyStatePresentation.discussions.title)
        assertEquals("No Students Found", InstructorEmptyStatePresentation.students.title)
    }

    @Test
    fun `code empty-state copy matches iOS`() {
        assertEquals("Create a code when you need to add another instructor.", InstructorEmptyStatePresentation.invites.description)
        assertEquals("Tap New Code when a new parish needs its first instructor account.", InstructorEmptyStatePresentation.setupCodes.description)
    }
}
