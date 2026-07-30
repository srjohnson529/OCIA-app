package com.illumined.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRegistrationPolicyTest {
    @Test fun signInFailureIsActionable() {
        assertEquals("Please sign in before registering notifications.", NotificationRegistrationPolicy.SIGN_IN_REQUIRED)
    }

    @Test fun blankTokenFailureIsActionable() {
        assertEquals("A valid notification token is required.", NotificationRegistrationPolicy.TOKEN_REQUIRED)
    }
}
