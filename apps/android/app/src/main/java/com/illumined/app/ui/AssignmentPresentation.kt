package com.illumined.app.ui

import com.illumined.app.data.Assignment

internal fun Assignment.homeContentLabel(): String? = when {
    readings.isNotEmpty() -> "${readings.size} reading${if (readings.size == 1) "" else "s"}"
    lessonLinks.isNotEmpty() -> lessonLinks.first().lessonTitle.ifBlank { lessonLinks.first().lessonId }
    else -> null
}

internal fun homeAssignmentPreview(assignments: List<Assignment>) = assignments.take(3)
internal fun remainingHomeAssignmentCount(assignments: List<Assignment>) = (assignments.size - 3).coerceAtLeast(0)
