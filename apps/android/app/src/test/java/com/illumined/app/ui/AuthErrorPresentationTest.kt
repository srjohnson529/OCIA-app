package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthErrorPresentationTest {
    @Test
    fun `firebase auth codes match iOS friendly messages`() {
        assertEquals("Enter a valid email address.", message("ERROR_INVALID_EMAIL"))
        assertEquals("The email or password was not correct.", message("ERROR_WRONG_PASSWORD"))
        assertEquals("The email or password was not correct.", message("ERROR_USER_NOT_FOUND"))
        assertEquals("The email or password was not correct.", message("ERROR_INVALID_CREDENTIAL"))
        assertEquals("That email already has an account. Try signing in instead.", message("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals("Use a password with at least 6 characters.", message("ERROR_WEAK_PASSWORD"))
        assertEquals("Too many attempts. Please wait a few minutes and try again.", message("ERROR_TOO_MANY_REQUESTS"))
    }

    @Test
    fun `unknown firebase error retains platform detail`() {
        assertEquals("Network unavailable", AuthErrorPresentation.messageForCode("ERROR_NETWORK_REQUEST_FAILED", "Network unavailable"))
    }

    @Test
    fun `password reset validation and success copy match iOS`() {
        assertEquals(AuthErrorPresentation.ResetEmailRequired, AuthErrorPresentation.resetEmailError(" ", false))
        assertEquals("Enter a valid email address.", AuthErrorPresentation.resetEmailError("invalid", false))
        assertNull(AuthErrorPresentation.resetEmailError("person@example.com", true))
        assertEquals("Password reset email sent. Check your inbox for a secure reset link.", AuthErrorPresentation.ResetEmailSent)
    }

    private fun message(code: String) = AuthErrorPresentation.messageForCode(code, "Fallback")
}
