package com.illumined.app.ui

internal object InstructorDiscussionPolicy {
    fun statusText(isVisible: Boolean) = if (isVisible) "Visible to students" else "Hidden from students"

    fun canSave(title: String, prompt: String, hasSelectedLesson: Boolean, isSaving: Boolean): Boolean =
        title.isNotBlank() && prompt.isNotBlank() && hasSelectedLesson && !isSaving
}
