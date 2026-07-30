package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessCodeCopyTest {
    @Test
    fun instructorInviteDescriptionMatchesIosCopy() {
        assertEquals(
            "Create one-use instructor codes for OCIA TOTC. Give the code to a new instructor, and they can enter it while setting up their profile. Once used, the code is automatically closed.",
            instructorInviteDescription("OCIA TOTC")
        )
    }

    @Test
    fun instructorEmptyStateMatchesIosCopy() {
        assertEquals(
            "Create a code when you need to add another instructor.",
            accessCodeEmptyDescription(parishMode = false)
        )
    }

    @Test
    fun parishEmptyStateMatchesIosCopy() {
        assertEquals(
            "Tap New Code when a new parish needs its first instructor account.",
            accessCodeEmptyDescription(parishMode = true)
        )
    }
}
