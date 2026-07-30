package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {
    @Test
    fun statusDistinguishesNotRequestedDeniedAndEnabled() {
        assertEquals("Not set up", NotificationPermissionPolicy.statusText(false, false))
        assertEquals("Off", NotificationPermissionPolicy.statusText(false, true))
        assertEquals("Enabled", NotificationPermissionPolicy.statusText(true, true))
    }

    @Test
    fun settingsRecoveryAppearsAfterDenialOrSystemDisablement() {
        assertFalse(NotificationPermissionPolicy.showSettings(false, false, true))
        assertTrue(NotificationPermissionPolicy.showSettings(false, true, true))
        assertTrue(NotificationPermissionPolicy.showSettings(false, false, false))
        assertFalse(NotificationPermissionPolicy.showSettings(true, true, true))
    }
}
