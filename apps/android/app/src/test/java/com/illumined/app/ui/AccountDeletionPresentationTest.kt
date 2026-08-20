package com.illumined.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionPresentationTest {
    @Test
    fun requiresPasswordAndIdleState() {
        assertFalse(AccountDeletionPresentation.canDelete("", false))
        assertFalse(AccountDeletionPresentation.canDelete("password", true))
        assertTrue(AccountDeletionPresentation.canDelete("password", false))
    }
}
