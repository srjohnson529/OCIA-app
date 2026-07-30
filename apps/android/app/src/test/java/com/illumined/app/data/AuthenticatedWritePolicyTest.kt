package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticatedWritePolicyTest {
    private fun profile(classes: List<String>) = UserProfile("Stephen", classes, emptySet())

    @Test
    fun writesUseProfilesPrimaryClass() {
        assertEquals("OCIA TOTC", AuthenticatedWritePolicy.primaryClassId(profile(listOf("OCIA TOTC", "Other"))))
        assertTrue(AuthenticatedWritePolicy.hasWritableClass(profile(listOf("OCIA TOTC"))))
    }

    @Test
    fun missingOrBlankClassIsNotWritable() {
        assertFalse(AuthenticatedWritePolicy.hasWritableClass(profile(emptyList())))
        assertFalse(AuthenticatedWritePolicy.hasWritableClass(profile(listOf(""))))
    }
}
