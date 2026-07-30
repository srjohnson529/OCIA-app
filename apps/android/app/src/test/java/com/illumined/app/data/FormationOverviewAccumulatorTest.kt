package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FormationOverviewAccumulatorTest {
    @Test
    fun `overview is unavailable until profile arrives`() {
        val accumulator = FormationOverviewAccumulator()
        assertNull(accumulator.overview())

        accumulator.updateSchedule(listOf(session("Early session")))
        assertNull(accumulator.overview())

        accumulator.updateProfile(profile())
        assertNotNull(accumulator.overview())
        assertEquals("Early session", accumulator.overview()!!.schedule.single().topic)
    }

    @Test
    fun `independent source updates preserve previously loaded sections`() {
        val accumulator = FormationOverviewAccumulator()
        accumulator.updateProfile(profile())
        accumulator.updateSchedule(listOf(session("The Creed")))
        accumulator.updateCompletions(listOf(completion("assignment-1", true)))

        accumulator.updatePrompts(
            listOf(DiscussionPrompt("prompt-1", "lesson-1", "Reflect", "What stood out?", "Faith", true)),
        )

        val overview = accumulator.overview()!!
        assertEquals("The Creed", overview.schedule.single().topic)
        assertEquals(setOf("assignment-1"), overview.completedAssignmentIds)
        assertEquals("Reflect", overview.discussionPrompts.single().title)
    }

    @Test
    fun `class change clears class data but preserves user completions`() {
        val accumulator = FormationOverviewAccumulator()
        accumulator.updateProfile(profile())
        accumulator.updateSchedule(listOf(session("Old class")))
        accumulator.updatePrompts(listOf(DiscussionPrompt("p", "l", "Old", "Prompt", "Lesson", false)))
        accumulator.updateCompletions(listOf(completion("assignment-1", true)))

        accumulator.resetClassData()

        val overview = accumulator.overview()!!
        assertEquals(emptyList<ScheduleItem>(), overview.schedule)
        assertEquals(emptyList<DiscussionPrompt>(), overview.discussionPrompts)
        assertEquals(setOf("assignment-1"), overview.completedAssignmentIds)
    }

    @Test
    fun `incomplete records remain available without entering completed id set`() {
        val accumulator = FormationOverviewAccumulator()
        accumulator.updateProfile(profile())
        accumulator.updateCompletions(listOf(completion("assignment-1", false)))

        val overview = accumulator.overview()!!
        assertEquals(1, overview.assignmentCompletions.size)
        assertEquals(emptySet<String>(), overview.completedAssignmentIds)
    }

    private fun profile() = UserProfile("Stephen", listOf("OCIA"), emptySet())

    private fun session(topic: String) = ScheduleItem("session", "OCIA", topic, "", null)

    private fun completion(id: String, completed: Boolean) = AssignmentCompletion(
        assignmentId = id,
        userId = "user-1",
        studentName = "Stephen",
        isCompleted = completed,
    )
}
