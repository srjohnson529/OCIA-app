package com.illumined.app.ui

internal object ChatPresentation {
    fun canSend(draft: String, hasProfile: Boolean, isSending: Boolean) =
        draft.isNotBlank() && hasProfile && !isSending
}
