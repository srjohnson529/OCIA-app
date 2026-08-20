package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InviteLinkTest {
    @Test fun `invite links round trip and privileged roles require one use codes`() {
        val instructor = IlluminedInviteLink(InviteRole.INSTRUCTOR, "OCIA", "ABCD-2345")
        assertEquals(instructor, IlluminedInviteLink.parse(instructor.url))
        assertEquals(InviteRole.STUDENT, IlluminedInviteLink.parse("illumined://join?role=student&classId=OCIA")?.role)
        assertEquals(InviteRole.STUDENT, IlluminedInviteLink.parse("https://illumined.net/join?role=student&classId=OCIA")?.role)
        assertNull(IlluminedInviteLink.parse("illumined://join?role=instructor&classId=OCIA"))
        assertNull(IlluminedInviteLink.parse("illumined://join?role=parish"))
        assertNull(IlluminedInviteLink.parse("https://example.com/join?role=student&classId=OCIA"))
    }
}
