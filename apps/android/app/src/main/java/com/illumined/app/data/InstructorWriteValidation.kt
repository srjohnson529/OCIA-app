package com.illumined.app.data

internal object InstructorWriteValidation {
    fun announcementError(title: String, message: String): String? =
        if (title.isBlank() || message.isBlank()) "Please add both a title and message." else null

    fun scheduleError(topic: String): String? =
        if (topic.isBlank()) "Please add a class topic." else null

    fun assignmentError(title: String): String? =
        if (title.isBlank()) "Please add an assignment title." else null

    fun discussionError(title: String, prompt: String): String? =
        if (title.isBlank() || prompt.isBlank()) "Please add a title and prompt." else null
}
