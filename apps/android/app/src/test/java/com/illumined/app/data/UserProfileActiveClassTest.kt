package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileActiveClassTest {
    private fun profile(classIds: List<String>, activeClassId: String = "") = UserProfile(
        displayName = "Instructor",
        classIds = classIds,
        completedLessons = emptySet(),
        isInstructor = true,
        activeClassId = activeClassId,
    )

    @Test
    fun selectedClassUsesActiveMembership() {
        assertEquals("mens-ministry", profile(listOf("ocia", "mens-ministry"), "mens-ministry").selectedClassId)
    }

    @Test
    fun selectedClassFallsBackForOlderProfilesAndInvalidValues() {
        assertEquals("ocia", profile(listOf("ocia", "mens-ministry")).selectedClassId)
        assertEquals("ocia", profile(listOf("ocia", "mens-ministry"), "not-assigned").selectedClassId)
    }

    @Test
    fun archivedClassesAreExcludedFromActiveSelection() {
        val profile = UserProfile(
            displayName = "Instructor",
            classIds = listOf("ocia", "mens-ministry"),
            completedLessons = emptySet(),
            archivedClassIds = listOf("ocia"),
            isInstructor = true,
            activeClassId = "ocia",
        )
        assertEquals(listOf("mens-ministry"), profile.activeClassIds)
        assertEquals("mens-ministry", profile.selectedClassId)
    }
}
