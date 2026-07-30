package com.illumined.app.ui

import com.google.firebase.auth.FirebaseAuthException

internal object AuthErrorPresentation {
    const val ResetEmailRequired = "Enter your email address first, then request a password reset."
    const val ResetEmailSent = "Password reset email sent. Check your inbox for a secure reset link."

    fun message(problem: Throwable): String = messageForCode(
        code = (problem as? FirebaseAuthException)?.errorCode,
        fallback = problem.localizedMessage?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again.",
    )

    fun messageForCode(code: String?, fallback: String): String = when (code) {
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" ->
                "The email or password was not correct."
            "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "That email already has an account. Try signing in instead."
            "ERROR_WEAK_PASSWORD" -> "Use a password with at least 6 characters."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a few minutes and try again."
            else -> fallback
    }

    fun resetEmailError(email: String, isValid: Boolean): String? = when {
        email.trim().isEmpty() -> ResetEmailRequired
        !isValid -> "Enter a valid email address."
        else -> null
    }
}
