package com.illumined.app.data

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class AccountDeletionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    fun deleteAccount(password: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val user = auth.currentUser
            ?: return onError(IllegalStateException("Please sign in before deleting your account."))
        val email = user.email
            ?: return onError(IllegalStateException("This account does not have an email address."))
        if (password.isBlank()) return onError(IllegalArgumentException("Enter your password to continue."))

        user.reauthenticate(EmailAuthProvider.getCredential(email, password))
            .continueWithTask { authentication ->
                authentication.exception?.let { throw it }
                functions.getHttpsCallable("deleteOwnAccount").call()
            }
            .addOnSuccessListener {
                auth.signOut()
                onSuccess()
            }
            .addOnFailureListener(onError)
    }
}
