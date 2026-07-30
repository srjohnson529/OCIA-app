package com.illumined.app.ui

internal object InstructorErrorPresentation {
    const val ScheduleTitle = "Schedule Error"
    const val AssignmentTitle = "Assignment Error"
    const val StudentProgressTitle = "Student Progress Error"

    fun message(problem: Throwable, fallback: String): String =
        problem.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
}
