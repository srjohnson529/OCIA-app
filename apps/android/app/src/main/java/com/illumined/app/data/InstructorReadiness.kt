package com.illumined.app.data

import kotlin.math.roundToInt

data class AssignmentCompletion(
    val assignmentId: String,
    val userId: String,
    val studentName: String,
    val isCompleted: Boolean,
    val parentAssignmentId: String = "",
    val assignmentItemId: String = "",
    val assignmentItemTitle: String = "",
    val assignmentItemType: String = "",
    val classId: String = "",
)

data class AssignmentProgress(
    val completedCount: Int,
    val totalStudents: Int,
    val incompleteNames: List<String>,
) {
    val fraction: Float get() = if (totalStudents == 0) 0f else (completedCount.toFloat() / totalStudents).coerceIn(0f, 1f)
}

data class ClassReadiness(
    val completedChecks: Int,
    val totalChecks: Int,
) {
    val fraction: Float get() = if (totalChecks == 0) 0f else (completedChecks.toFloat() / totalChecks).coerceIn(0f, 1f)
    val percent: Int get() = (fraction * 100).roundToInt()
}

object InstructorReadinessCalculator {
    fun completedReadingNames(userId: String, completions: List<AssignmentCompletion>): List<String> = completions
        .filter { it.userId == userId && it.isCompleted && it.assignmentItemType == "reading" && it.assignmentItemTitle.isNotBlank() }
        .map { it.assignmentItemTitle.trim() }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

    fun parentCompletedAfterReadingChange(readingIds: List<String>, changedReadingId: String, completed: Boolean, previouslyCompletedIds: Set<String>): Boolean {
        if (!completed || changedReadingId !in readingIds) return false
        return readingIds.all { it == changedReadingId || it in previouslyCompletedIds }
    }

    fun assignmentProgress(assignmentId: String, students: List<UserProfile>, completions: List<AssignmentCompletion>): AssignmentProgress {
        val studentIds = students.map { it.userId }.filter { it.isNotBlank() }.toSet()
        val completedIds = completions.filter { it.assignmentId == assignmentId && it.isCompleted && it.userId in studentIds }.map { it.userId }.toSet()
        return AssignmentProgress(
            completedCount = completedIds.size,
            totalStudents = students.size,
            incompleteNames = students.filter { it.userId !in completedIds }.map { it.displayName }.sortedBy { it.lowercase() },
        )
    }

    fun classReadiness(assignments: List<Assignment>, students: List<UserProfile>, completions: List<AssignmentCompletion>): ClassReadiness {
        val progress = assignments.map { assignmentProgress(it.id, students, completions) }
        return ClassReadiness(progress.sumOf { it.completedCount }, assignments.size * students.size)
    }
}
