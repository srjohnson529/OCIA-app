package com.illumined.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPresentationTest {
    @Test
    fun sendRequiresNonWhitespaceDraftAndProfile() {
        assertTrue(ChatPresentation.canSend("Please pray for us", true, false))
        assertFalse(ChatPresentation.canSend(" \n ", true, false))
        assertFalse(ChatPresentation.canSend("Please pray for us", false, false))
    }

    @Test
    fun sendIsLockedWhileRequestIsInFlight() {
        assertFalse(ChatPresentation.canSend("Please pray for us", true, true))
    }
}
