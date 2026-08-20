package com.illumined.app.data

internal object RoleWritePolicy {
    fun instructorError(
        profile: UserProfile,
        authenticated: Boolean,
        signInMessage: String,
        roleMessage: String,
        classMessage: String = "Please assign your instructor profile to a class first.",
    ): String? = when {
        !authenticated -> signInMessage
        !profile.isInstructor -> roleMessage
        profile.selectedClassId.isBlank() -> classMessage
        else -> null
    }

    fun adminSetupCodeError(profile: UserProfile, authenticated: Boolean): String? = when {
        !profile.isAdmin -> "Only app admins can create parish setup codes."
        !authenticated -> "Please sign in before creating parish setup codes."
        else -> null
    }
}
