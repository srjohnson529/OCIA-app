package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoleWritePolicyTest {
    private fun profile(instructor: Boolean = false, admin: Boolean = false, classes: List<String> = listOf("OCIA")) =
        UserProfile("Stephen", classes, emptySet(), isInstructor = instructor, isAdmin = admin)

    @Test
    fun instructorWritesRequireAuthenticationRoleAndClass() {
        assertEquals("Sign in", RoleWritePolicy.instructorError(profile(instructor = true), false, "Sign in", "Instructor only"))
        assertEquals("Instructor only", RoleWritePolicy.instructorError(profile(), true, "Sign in", "Instructor only"))
        assertEquals("Please assign your instructor profile to a class first.", RoleWritePolicy.instructorError(profile(instructor = true, classes = emptyList()), true, "Sign in", "Instructor only"))
        assertNull(RoleWritePolicy.instructorError(profile(instructor = true), true, "Sign in", "Instructor only"))
    }

    @Test
    fun parishSetupCodesRequireAdminAndAuthentication() {
        assertEquals("Only app admins can create parish setup codes.", RoleWritePolicy.adminSetupCodeError(profile(), true))
        assertEquals("Please sign in before creating parish setup codes.", RoleWritePolicy.adminSetupCodeError(profile(admin = true), false))
        assertNull(RoleWritePolicy.adminSetupCodeError(profile(admin = true), true))
    }
}
