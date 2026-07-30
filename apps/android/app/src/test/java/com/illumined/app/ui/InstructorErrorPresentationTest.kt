package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InstructorErrorPresentationTest {
    @Test
    fun `manager alert titles match iOS`() {
        assertEquals("Schedule Error", InstructorErrorPresentation.ScheduleTitle)
        assertEquals("Assignment Error", InstructorErrorPresentation.AssignmentTitle)
        assertEquals("Student Progress Error", InstructorErrorPresentation.StudentProgressTitle)
    }

    @Test
    fun `firebase detail is preferred with a stable fallback`() {
        assertEquals("Permission denied", InstructorErrorPresentation.message(IllegalStateException("Permission denied"), "Fallback"))
        assertEquals("Fallback", InstructorErrorPresentation.message(IllegalStateException(""), "Fallback"))
    }
}
