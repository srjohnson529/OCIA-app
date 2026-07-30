package com.illumined.app.data

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnnouncementTimestampParityTest {
    @Test
    fun `updated timestamp takes precedence like iOS`() {
        val created = Timestamp(Date(1_000))
        val updated = Timestamp(Date(2_000))
        assertEquals(updated, announcement(updated, created).displayTimestamp)
    }

    @Test
    fun `created timestamp is the iOS fallback for legacy announcements`() {
        val created = Timestamp(Date(1_000))
        assertEquals(created, announcement(null, created).displayTimestamp)
        assertNull(announcement(null, null).displayTimestamp)
    }

    private fun announcement(updated: Timestamp?, created: Timestamp?) =
        Announcement("id", "Title", "Message", true, updated, created)
}
