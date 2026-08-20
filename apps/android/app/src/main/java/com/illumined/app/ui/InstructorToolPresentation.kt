package com.illumined.app.ui

internal data class InstructorToolItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val symbolName: String,
)

internal object InstructorToolPresentation {
    const val Status = "Open"

    val items = listOf(
        InstructorToolItem("announcements", "Announcements", "Create and edit dashboard announcements.", "megaphone"),
        InstructorToolItem("assignments", "Assignments", "Post lesson assignments for students.", "checklist"),
        InstructorToolItem("discussions", "Discussion Boards", "Create lesson-linked discussion prompts.", "text.bubble"),
        InstructorToolItem("progress", "Student Progress", "Review lesson completion by student.", "chart.bar"),
        InstructorToolItem("schedule", "Class Schedule", "Update the next class date and topic.", "calendar.badge.clock"),
        InstructorToolItem("classes", "Classes", "Create, switch, archive, and restore your classes.", "person.3"),
        InstructorToolItem("invites", "Instructor Invites", "Create one-use codes for new instructors.", "key"),
    )

    fun managerAction(title: String): String? = when (title) {
        "Classes" -> "New Class"
        "Announcements" -> "New Announcement"
        "Assignments" -> "New Assignment"
        "Discussion Boards" -> "New Discussion"
        else -> null
    }

    fun managerSymbol(title: String): InstructorSymbolKind =
        instructorSymbol(items.firstOrNull { it.title == title }?.symbolName ?: "person.text.rectangle")
}
