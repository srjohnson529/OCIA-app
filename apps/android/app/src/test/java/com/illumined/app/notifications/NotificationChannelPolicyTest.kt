package com.illumined.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationChannelPolicyTest {
    @Test
    fun `background delivery uses the same stable class updates channel as foreground delivery`() {
        assertEquals("illumined_class_updates", NotificationChannelPolicy.ID)
        assertEquals("Class updates", NotificationChannelPolicy.NAME)
        assertEquals(
            "Announcements, assignments, and discussion activity from Illumined",
            NotificationChannelPolicy.DESCRIPTION,
        )
    }
}
