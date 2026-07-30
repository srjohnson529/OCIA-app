package com.illumined.app.ui

internal object MorePresentation {
    fun roleText(isInstructor: Boolean) = if (isInstructor) "Instructor" else "Student"
}
