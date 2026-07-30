package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructorEditorOperationPolicyTest {
    @Test
    fun `navigation and destructive controls lock during firebase operation`() {
        assertTrue(InstructorEditorOperationPolicy.canInteract(false))
        assertFalse(InstructorEditorOperationPolicy.canInteract(true))
    }

    @Test
    fun `working label matches iOS save state`() {
        assertEquals("Save Assignment", InstructorEditorOperationPolicy.saveLabel(false, "Save Assignment"))
        assertEquals("Saving...", InstructorEditorOperationPolicy.saveLabel(true, "Save Assignment"))
    }
}
