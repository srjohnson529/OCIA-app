package com.illumined.app.ui

internal object DiscussionInteractionPolicy {
    fun canSubmit(text: String, isWorking: Boolean) = text.isNotBlank() && !isWorking

    fun ownsPost(authenticatedUserId: String?, authorId: String) =
        !authenticatedUserId.isNullOrBlank() && authenticatedUserId == authorId
}
