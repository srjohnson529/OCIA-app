package com.illumined.app.notifications

import org.junit.Assert.*
import org.junit.Test

class MessageNotificationNavigationTest {
    @Test fun oldNotificationsCannotOpenForAnotherAccountOrRevokedClass() {
        val request = MessageOpenRequest("room", "student", true)
        assertTrue(canOpenMessageRequest(request, "student", listOf("room"), emptyList(), emptyList()))
        assertFalse(canOpenMessageRequest(request, "other", listOf("room"), emptyList(), emptyList()))
        assertFalse(canOpenMessageRequest(request, "student", emptyList(), emptyList(), emptyList()))
        assertFalse(canOpenMessageRequest(request, "student", listOf("room"), listOf("room"), emptyList()))
        assertFalse(canOpenMessageRequest(request, "student", listOf("room"), emptyList(), listOf("room")))
    }
    @Test fun privateTapRetainsRecipientAndClassWithoutMessageContent() {
        MessageNotificationNavigation.request = null
        MessageNotificationNavigation.accept(mapOf("type" to "private_message", "classId" to "room", "recipientId" to "student"))
        assertEquals(MessageOpenRequest("room", "student", true), MessageNotificationNavigation.request)
    }
    @Test fun repliesAndReactionsOpenClassroomChat() {
        for (type in listOf("classroom_message", "chat_reply", "chat_reaction")) {
            MessageNotificationNavigation.request = null
            MessageNotificationNavigation.accept(mapOf("type" to type, "classId" to "room", "recipientId" to "student"))
            assertEquals(MessageOpenRequest("room", "student", false), MessageNotificationNavigation.request)
        }
    }
    @Test fun missingIdentityAndUnrelatedAlertsAreIgnored() {
        MessageNotificationNavigation.request = null
        MessageNotificationNavigation.accept(mapOf("type" to "private_message", "classId" to "room"))
        assertNull(MessageNotificationNavigation.request)
        MessageNotificationNavigation.accept(mapOf("type" to "daily_formation", "classId" to "room", "recipientId" to "student"))
        assertNull(MessageNotificationNavigation.request)
    }
}
