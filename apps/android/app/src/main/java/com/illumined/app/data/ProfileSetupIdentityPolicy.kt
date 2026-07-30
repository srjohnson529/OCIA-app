package com.illumined.app.data

internal object ProfileSetupIdentityPolicy {
    fun missingUserMessage(startingClass: Boolean) = if (startingClass) {
        "Please sign in before starting a new class."
    } else {
        "Please sign in before saving a profile."
    }
}
