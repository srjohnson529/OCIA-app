package com.illumined.app.ui

import java.util.Locale

internal data class InstructorToolItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val symbolName: String,
)

internal object InstructorToolPresentation {
    const val Status = "Open"

    val items = listOf(
        InstructorToolItem("progress", "Student Details", "Review student progress and roster.", "person.3"),
        InstructorToolItem("invites", "Classroom Codes", "Invite students and co-instructors.", "key"),
        InstructorToolItem("announcements", "Announcements", "Create and edit dashboard announcements.", "megaphone"),
        InstructorToolItem("assignments", "Assignments", "Post lesson assignments for students.", "checklist"),
        InstructorToolItem("discussions", "Discussion Boards", "Create assignment-linked discussion prompts.", "text.bubble"),
        InstructorToolItem("schedule", "Class Schedule", "Update the next class date and topic.", "calendar.badge.clock"),
        InstructorToolItem("daily-formation", "Daily Formation", "Create and schedule liturgical facts, saints, and notes.", "calendar.badge.clock"),
        InstructorToolItem("rite-preparation", "Rite and Sacrament Preparation", "Publish preparation guides and review acknowledgments.", "calendar.badge.clock"),
        InstructorToolItem("classes", "Classroom Management", "Manage students, invitations, requests, photos, and classroom settings.", "person.3"),
        InstructorToolItem("updates", "From Illumined", "App news and guidance from Illumined.", "megaphone"),
    )

    fun localizedTitle(item: InstructorToolItem): String = if (Locale.getDefault().language == "es") when (item.key) {
        "updates" -> "De Illumined"
        "rite-preparation" -> "Preparación para ritos y sacramentos"
        "announcements" -> "Anuncios"
        "assignments" -> "Tareas"
        "discussions" -> "Foros de discusión"
        "progress" -> "Detalles de estudiantes"
        "schedule" -> "Calendario de clases"
        "daily-formation" -> "Formación diaria"
        "classes" -> "Administración del aula"
        "invites" -> "Códigos del aula"
        else -> item.title
    } else item.title

    fun localizedSubtitle(item: InstructorToolItem): String = if (Locale.getDefault().language == "es") when (item.key) {
        "updates" -> "Noticias y orientación de Illumined."
        "rite-preparation" -> "Publica guías y revisa las confirmaciones de lectura."
        "announcements" -> "Crea y edita anuncios para el inicio."
        "assignments" -> "Publica tareas de lecciones para los estudiantes."
        "discussions" -> "Crea consignas de discusión vinculadas a las tareas."
        "progress" -> "Consulta el progreso y administra la lista de estudiantes."
        "schedule" -> "Actualiza la fecha y el tema de la próxima clase."
        "daily-formation" -> "Crea y programa datos litúrgicos, santos y notas."
        "classes" -> "Crea, cambia, archiva y restaura tus clases."
        "invites" -> "Administra los códigos de invitación para estudiantes e instructores."
        else -> item.subtitle
    } else item.subtitle

    fun localizedStatus(): String = if (Locale.getDefault().language == "es") "Abrir" else Status

    fun managerAction(title: String): String? = when (title) {
        "Classes", "Clases", "Classroom Management", "Administración del aula" -> if (Locale.getDefault().language == "es") "Nueva clase" else "New Class"
        "Announcements", "Anuncios" -> if (Locale.getDefault().language == "es") "Nuevo anuncio" else "New Announcement"
        "Assignments", "Tareas" -> if (Locale.getDefault().language == "es") "Nueva tarea" else "New Assignment"
        "Discussion Boards", "Foros de discusión" -> if (Locale.getDefault().language == "es") "Nueva discusión" else "New Discussion"
        "Daily Formation", "Formación diaria" -> if (Locale.getDefault().language == "es") "Nueva entrada" else "New Entry"
        else -> null
    }

    fun managerSymbol(title: String): InstructorSymbolKind = instructorSymbol(
        items.firstOrNull { it.title == title || localizedTitle(it) == title }?.symbolName ?: "person.text.rectangle"
    )
}
