package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileSetupPolicyTest {
    @Test fun inviteMustContainClassId() {
        assertEquals("That instructor invite code is missing its class ID.", ProfileSetupPolicy.instructorInviteClassError(null, "OCIA"))
        assertEquals("That instructor invite code is missing its class ID.", ProfileSetupPolicy.instructorInviteClassError("", "OCIA"))
    }

    @Test fun inviteClassComparisonIsCaseInsensitive() {
        assertNull(ProfileSetupPolicy.instructorInviteClassError("OCIA-1", "ocia-1"))
    }

    @Test fun mismatchMessageMatchesIosAndGuidesCorrection() {
        assertEquals("That invite code belongs to class OCIA-2. Enter that class ID to continue.", ProfileSetupPolicy.instructorInviteClassError("OCIA-2", "OCIA-1"))
    }
}
