package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormationProgressWritePolicyTest {
    @Test
    fun `prayer identifiers are trimmed like iOS before array updates`() {
        assertEquals("our-father", FormationProgressWritePolicy.normalizedPrayerId("  our-father\n"))
    }

    @Test
    fun `blank prayer identifiers do not write profile data`() {
        assertNull(FormationProgressWritePolicy.normalizedPrayerId(" \n\t "))
    }

    @Test
    fun `badge array updates discard empty and duplicate identifiers like iOS`() {
        assertEquals(
            listOf("category-sacred-scripture", "graduate"),
            FormationProgressWritePolicy.normalizedBadgeIds(
                listOf("category-sacred-scripture", "", "graduate", "category-sacred-scripture"),
            ),
        )
    }
}
