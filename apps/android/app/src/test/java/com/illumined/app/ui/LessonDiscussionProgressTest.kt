package com.illumined.app.ui

import com.illumined.app.data.DiscussionPrompt
import org.junit.Assert.assertEquals
import org.junit.Test

class LessonDiscussionProgressTest {
    private val prompt = DiscussionPrompt("prompt-1", "lesson-1", "Reflect", "Prompt", "Lesson", true)

    @Test fun incompleteLessonAlwaysRemainsNotCompleted() {
        assertEquals(LessonProgressStatus.NOT_COMPLETED, lessonProgressStatus("lesson-1", emptySet(), listOf(prompt), setOf("prompt-1")))
    }

    @Test fun completedLessonWithUnansweredPromptIsInProgress() {
        assertEquals(LessonProgressStatus.IN_PROGRESS, lessonProgressStatus("lesson-1", setOf("lesson-1"), listOf(prompt), emptySet()))
    }

    @Test fun completedLessonIsCompleteWithoutPromptOrAfterParticipation() {
        assertEquals(LessonProgressStatus.COMPLETED, lessonProgressStatus("lesson-1", setOf("lesson-1"), emptyList(), emptySet()))
        assertEquals(LessonProgressStatus.COMPLETED, lessonProgressStatus("lesson-1", setOf("lesson-1"), listOf(prompt), setOf("prompt-1")))
    }
}
