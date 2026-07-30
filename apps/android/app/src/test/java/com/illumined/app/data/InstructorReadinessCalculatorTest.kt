package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InstructorReadinessCalculatorTest {
    private fun student(id: String, name: String) = UserProfile(name, listOf("class"), emptySet(), userId = id)
    private fun assignment(id: String) = Assignment(id, "class", id, "", "", emptyList(), "", emptyList(), true, null)

    @Test fun assignmentProgressCountsUniqueCurrentStudentsOnly() {
        val students = listOf(student("a", "Alice"), student("b", "Beth"), student("c", "Cara"))
        val completions = listOf(
            AssignmentCompletion("one", "a", "Alice", true),
            AssignmentCompletion("one", "a", "Alice", true),
            AssignmentCompletion("one", "b", "Beth", false),
            AssignmentCompletion("one", "former", "Former Student", true),
        )
        val result = InstructorReadinessCalculator.assignmentProgress("one", students, completions)
        assertEquals(1, result.completedCount)
        assertEquals(3, result.totalStudents)
        assertEquals(listOf("Beth", "Cara"), result.incompleteNames)
    }

    @Test fun classReadinessAggregatesAssignmentStudentChecks() {
        val students = listOf(student("a", "Alice"), student("b", "Beth"))
        val completions = listOf(
            AssignmentCompletion("one", "a", "Alice", true),
            AssignmentCompletion("two", "a", "Alice", true),
            AssignmentCompletion("two", "b", "Beth", true),
        )
        val result = InstructorReadinessCalculator.classReadiness(listOf(assignment("one"), assignment("two")), students, completions)
        assertEquals(3, result.completedChecks)
        assertEquals(4, result.totalChecks)
        assertEquals(75, result.percent)
    }

    @Test fun emptyRosterProducesZeroReadiness() {
        val result = InstructorReadinessCalculator.classReadiness(listOf(assignment("one")), emptyList(), emptyList())
        assertEquals(0, result.totalChecks)
        assertEquals(0, result.percent)
    }

    @Test fun parentCompletesOnlyWhenEveryReadingIsComplete() {
        val readings = listOf("one", "two", "three")
        assertEquals(false, InstructorReadinessCalculator.parentCompletedAfterReadingChange(readings, "two", true, setOf("one")))
        assertEquals(true, InstructorReadinessCalculator.parentCompletedAfterReadingChange(readings, "three", true, setOf("one", "two")))
        assertEquals(false, InstructorReadinessCalculator.parentCompletedAfterReadingChange(readings, "two", false, readings.toSet()))
    }

    @Test fun completedReadingNamesAreStudentScopedUniqueAndSorted() {
        val records = listOf(
            AssignmentCompletion("a", "student", "", true, assignmentItemTitle = "Zion", assignmentItemType = "reading"),
            AssignmentCompletion("b", "student", "", true, assignmentItemTitle = "Advent", assignmentItemType = "reading"),
            AssignmentCompletion("c", "student", "", true, assignmentItemTitle = "advent", assignmentItemType = "reading"),
            AssignmentCompletion("d", "other", "", true, assignmentItemTitle = "Other", assignmentItemType = "reading"),
            AssignmentCompletion("e", "student", "", false, assignmentItemTitle = "Incomplete", assignmentItemType = "reading"),
        )
        assertEquals(listOf("Advent", "Zion"), InstructorReadinessCalculator.completedReadingNames("student", records))
    }
}
