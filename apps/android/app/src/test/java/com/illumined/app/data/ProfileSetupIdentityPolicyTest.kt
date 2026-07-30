package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSetupIdentityPolicyTest {
    @Test
    fun missingAuthenticationErrorsMatchIosOperations() {
        assertEquals("Please sign in before saving a profile.", ProfileSetupIdentityPolicy.missingUserMessage(false))
        assertEquals("Please sign in before starting a new class.", ProfileSetupIdentityPolicy.missingUserMessage(true))
    }
}
