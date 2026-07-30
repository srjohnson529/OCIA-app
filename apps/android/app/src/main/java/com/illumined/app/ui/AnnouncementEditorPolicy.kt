package com.illumined.app.ui

internal object AnnouncementEditorPolicy {
    fun statusText(isActive: Boolean) = if (isActive) "Active" else "Hidden"

    fun canSave(title: String, message: String, isSaving: Boolean) =
        title.isNotBlank() && message.isNotBlank() && !isSaving
}
