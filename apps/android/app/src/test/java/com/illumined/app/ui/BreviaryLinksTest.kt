package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreviaryLinksTest {
    @Test
    fun `matches the eight iOS liturgy of the hours destinations`() {
        assertEquals(
            listOf(
                "iBreviary",
                "Office of Readings",
                "Morning Prayer",
                "Daytime Prayer",
                "Evening Prayer",
                "Night Prayer",
                "Divine Office Audio",
                "Sing the Hours",
            ),
            breviaryLinks.map { it.title },
        )
        assertEquals(8, breviaryLinks.map { it.url }.distinct().size)
        assertTrue(breviaryLinks.all { it.url.startsWith("https://") })
    }
}
