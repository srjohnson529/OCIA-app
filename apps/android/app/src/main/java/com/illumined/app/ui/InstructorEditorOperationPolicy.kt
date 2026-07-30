package com.illumined.app.ui

internal object InstructorEditorOperationPolicy {
    fun canInteract(working: Boolean): Boolean = !working
    fun saveLabel(working: Boolean, idleLabel: String): String = if (working) "Saving..." else idleLabel
}
