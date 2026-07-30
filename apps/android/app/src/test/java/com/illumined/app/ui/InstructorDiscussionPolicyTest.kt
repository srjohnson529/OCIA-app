package com.illumined.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructorDiscussionPolicyTest {
    @Test
    fun visibilityLabelsMatchIos() {
        assertEquals("Visible to students", InstructorDiscussionPolicy.statusText(true))
        assertEquals("Hidden from students", InstructorDiscussionPolicy.statusText(false))
    }

    @Test
    fun saveRequiresTrimmedContentLessonAndIdleState() {
        assertTrue(InstructorDiscussionPolicy.canSave("The Eucharist", "Reflect on this.", true, false))
        assertFalse(InstructorDiscussionPolicy.canSave("   ", "Reflect on this.", true, false))
        assertFalse(InstructorDiscussionPolicy.canSave("The Eucharist", "\n", true, false))
        assertFalse(InstructorDiscussionPolicy.canSave("The Eucharist", "Reflect on this.", false, false))
        assertFalse(InstructorDiscussionPolicy.canSave("The Eucharist", "Reflect on this.", true, true))
    }
}
