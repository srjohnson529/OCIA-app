package com.illumined.app.ui

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.functions.FirebaseFunctionsException

internal object AccountDeletionPresentation {
    const val WebUrl = "https://illumined-account-deletion.srjohnson529.chatgpt.site"
    const val Warning = "This permanently removes your Illumined account, profile, progress, messages, discussion responses, and prayer requests. This cannot be undone."
    const val SharedContentNotice = "Class-wide materials created by an instructor may remain available to the class without the instructor’s identity."

    fun canDelete(password: String, working: Boolean): Boolean = password.isNotBlank() && !working

    fun errorMessage(problem: Throwable): String {
        val cause = problem.cause ?: problem
        return when {
            cause is FirebaseAuthException && cause.errorCode in setOf("ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL") ->
                "The password was not correct. Your account was not deleted."
            cause is FirebaseFunctionsException && cause.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                "Please sign in again, then return here to delete your account."
            cause is FirebaseFunctionsException && cause.code == FirebaseFunctionsException.Code.UNAVAILABLE ->
                "Account deletion is temporarily unavailable. Please try again."
            else -> "Your account could not be deleted. Nothing has been changed. Please try again."
        }
    }
}
