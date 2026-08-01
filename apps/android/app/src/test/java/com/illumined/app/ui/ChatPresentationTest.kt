package com.illumined.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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

    @Test
    fun detectsHttpAndWwwLinksWithoutTrailingPunctuation() {
        val message = "Visit https://example.org/path, then www.usccb.org."

        assertEquals(
            listOf(
                ChatLink(6, 30, "https://example.org/path"),
                ChatLink(37, 50, "https://www.usccb.org"),
            ),
            ChatPresentation.linksIn(message),
        )
    }

    @Test
    fun ignoresUnsafeAndNonWebSchemes() {
        val message = "Do not link javascript:alert(1) or ftp://example.org."

        assertTrue(ChatPresentation.linksIn(message).isEmpty())
    }
}
