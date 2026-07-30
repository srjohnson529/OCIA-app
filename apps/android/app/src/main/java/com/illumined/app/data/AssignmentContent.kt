package com.illumined.app.data

data class AssignmentContent(
    val lessonLinks: List<AssignmentLessonLink>,
    val readings: List<AssignmentReading>,
) {
    val hasPartialReading: Boolean
        get() = readings.any { it.title.isBlank() != it.text.isBlank() }

    val cleanedLessonLinks: List<AssignmentLessonLink>
        get() = lessonLinks.map { AssignmentLessonLink(it.lessonId.trim(), it.lessonTitle.trim()) }.filter { it.lessonId.isNotBlank() }

    val cleanedReadings: List<AssignmentReading>
        get() = readings.map { AssignmentReading(it.id.ifBlank { java.util.UUID.randomUUID().toString() }, it.title.trim(), it.text.trim()) }
            .filter { it.title.isNotBlank() && it.text.isNotBlank() }

    fun resolvedReadings(legacyTitle: String = "", legacyText: String = ""): List<AssignmentReading> {
        val modernReadings = cleanedReadings
        if (modernReadings.isNotEmpty()) return modernReadings
        val title = legacyTitle.trim()
        val text = legacyText.trim()
        return if (title.isNotEmpty() && text.isNotEmpty()) {
            listOf(AssignmentReading("legacy-reading", title, text))
        } else {
            emptyList()
        }
    }
}
