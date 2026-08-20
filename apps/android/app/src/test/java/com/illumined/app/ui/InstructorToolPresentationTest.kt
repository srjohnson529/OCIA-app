package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InstructorToolPresentationTest {
    @Test
    fun `menu ordering and copy match iOS`() {
        assertEquals(
            listOf("Announcements", "Assignments", "Discussion Boards", "Student Progress", "Class Schedule", "Classes", "Instructor Invites"),
            InstructorToolPresentation.items.map { it.title },
        )
        assertEquals("Open", InstructorToolPresentation.Status)
    }

    @Test
    fun `every iOS tool intent has a nonblank scalable glyph`() {
        assertEquals(7, InstructorToolPresentation.items.map { it.key }.distinct().size)
        InstructorToolPresentation.items.forEach { item ->
            require(item.symbolName.isNotBlank())
            require(item.subtitle.isNotBlank())
        }
    }

    @Test
    fun `manager actions use exact iOS labels`() {
        assertEquals("New Announcement", InstructorToolPresentation.managerAction("Announcements"))
        assertEquals("New Class", InstructorToolPresentation.managerAction("Classes"))
        assertEquals("New Assignment", InstructorToolPresentation.managerAction("Assignments"))
        assertEquals("New Discussion", InstructorToolPresentation.managerAction("Discussion Boards"))
    }
}
