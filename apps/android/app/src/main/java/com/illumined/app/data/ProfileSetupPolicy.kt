package com.illumined.app.data

internal object ProfileSetupPolicy {
    fun instructorInviteClassError(inviteClassId: String?, enteredClassId: String): String? {
        if (inviteClassId.isNullOrEmpty()) return "That instructor invite code is missing its class ID."
        if (!inviteClassId.equals(enteredClassId, ignoreCase = true)) return "That invite code belongs to class $inviteClassId. Enter that class ID to continue."
        return null
    }
}
