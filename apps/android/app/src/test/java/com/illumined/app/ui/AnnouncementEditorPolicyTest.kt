package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementEditorPolicyTest {
    @Test
    fun statusLabelsMatchIos() {
        assertEquals("Active", AnnouncementEditorPolicy.statusText(true))
        assertEquals("Hidden", AnnouncementEditorPolicy.statusText(false))
    }

    @Test
    fun saveRequiresContentAndIdleState() {
        assertTrue(AnnouncementEditorPolicy.canSave("Retreat", "Please register this week.", false))
        assertFalse(AnnouncementEditorPolicy.canSave(" ", "Please register this week.", false))
        assertFalse(AnnouncementEditorPolicy.canSave("Retreat", "\n", false))
        assertFalse(AnnouncementEditorPolicy.canSave("Retreat", "Please register this week.", true))
    }
}
