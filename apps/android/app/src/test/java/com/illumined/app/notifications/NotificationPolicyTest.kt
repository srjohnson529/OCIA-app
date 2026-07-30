package com.illumined.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPolicyTest {
    @Test fun notificationPayloadTakesPriority() {
        assertEquals(NotificationContent("New Assignment", "Read chapter one"), resolveNotificationContent("New Assignment", "Read chapter one", mapOf("title" to "Data")))
    }

    @Test fun dataOnlyMessagesUseTitleBodyOrMessage() {
        assertEquals(NotificationContent("Announcement", "Class is at seven"), resolveNotificationContent(null, null, mapOf("title" to "Announcement", "message" to "Class is at seven")))
    }

    @Test fun emptyPayloadHasSafeIlluminedFallback() {
        assertEquals(NotificationContent("Illumined", "You have a new class update."), resolveNotificationContent("", null, emptyMap()))
    }
}
