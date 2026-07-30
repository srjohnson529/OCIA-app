package com.illumined.app.ui

import com.illumined.app.data.Assignment
import com.illumined.app.data.AssignmentLessonLink
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscussionAssignmentMatchingTest {
    private fun assignment(id: String, vararg lessonIds: String) = Assignment(
        id, "class", id, "", "", lessonIds.map { AssignmentLessonLink(it, it) }, "", emptyList(), true, null,
    )

    @Test fun findsEveryAssignmentLinkedToPromptLesson() {
        val assignments = listOf(assignment("one", "lesson-a"), assignment("two", "lesson-b", "lesson-a"), assignment("three", "lesson-c"))
        assertEquals(listOf("one", "two"), matchingDiscussionAssignments("lesson-a", assignments).map { it.id })
    }

    @Test fun doesNotMatchLegacyPrimaryIdWithoutCanonicalLink() {
        val legacyOnly = Assignment("legacy", "class", "Legacy", "lesson-a", "Lesson A", emptyList(), "", emptyList(), true, null)
        assertEquals(emptyList<Assignment>(), matchingDiscussionAssignments("lesson-a", listOf(legacyOnly)))
    }
}
