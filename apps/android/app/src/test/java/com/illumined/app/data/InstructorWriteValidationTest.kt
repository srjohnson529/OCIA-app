package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstructorWriteValidationTest {
    @Test fun messagesMatchIosWriteServices() {
        assertEquals("Please add both a title and message.", InstructorWriteValidation.announcementError(" ", "Message"))
        assertEquals("Please add a class topic.", InstructorWriteValidation.scheduleError("\n"))
        assertEquals("Please add an assignment title.", InstructorWriteValidation.assignmentError(""))
        assertEquals("Please add a title and prompt.", InstructorWriteValidation.discussionError("Title", " "))
    }

    @Test fun validInstructorContentPasses() {
        assertNull(InstructorWriteValidation.announcementError("Title", "Message"))
        assertNull(InstructorWriteValidation.scheduleError("The Eucharist"))
        assertNull(InstructorWriteValidation.assignmentError("Read chapter 1"))
        assertNull(InstructorWriteValidation.discussionError("Reflection", "What stood out?"))
    }
}
