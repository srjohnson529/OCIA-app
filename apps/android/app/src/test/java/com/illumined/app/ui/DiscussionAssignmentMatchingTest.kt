package com.illumined.app.ui

import com.illumined.app.data.Assignment
import com.illumined.app.data.AssignmentLessonLink
import com.illumined.app.data.DiscussionPrompt
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscussionAssignmentMatchingTest {
    private fun assignment(id: String, vararg lessonIds: String) = Assignment(
        id, "class", id, "", "", lessonIds.map { AssignmentLessonLink(it, it) }, "", emptyList(), true, null,
    )

    @Test fun findsEveryAssignmentLinkedToPromptLesson() {
        val assignments = listOf(assignment("one", "lesson-a"), assignment("two", "lesson-b", "lesson-a"), assignment("three", "lesson-c"))
        val prompt = DiscussionPrompt("prompt", "lesson-a", "Title", "Prompt", "Lesson A", true)
        assertEquals(listOf("one", "two"), matchingDiscussionAssignments(prompt, assignments).map { it.id })
    }

    @Test fun doesNotMatchLegacyPrimaryIdWithoutCanonicalLink() {
        val legacyOnly = Assignment("legacy", "class", "Legacy", "lesson-a", "Lesson A", emptyList(), "", emptyList(), true, null)
        val prompt = DiscussionPrompt("prompt", "lesson-a", "Title", "Prompt", "Lesson A", true)
        assertEquals(emptyList<Assignment>(), matchingDiscussionAssignments(prompt, listOf(legacyOnly)))
    }

    @Test fun assignmentLinkTakesPrecedenceOverLessonLinks() {
        val assignments = listOf(assignment("one", "lesson-a"), assignment("two", "lesson-b"))
        val prompt = DiscussionPrompt("prompt", "lesson-a", "Title", "Prompt", "Lesson A", true, assignmentId = "two", assignmentTitle = "Two")
        assertEquals(listOf("two"), matchingDiscussionAssignments(prompt, assignments).map { it.id })
    }
}
