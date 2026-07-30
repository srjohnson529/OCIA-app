package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSymbolPresentationTest {
    @Test
    fun `home uses the current iOS symbol intents`() {
        assertEquals(
            listOf(HomeSymbolKind.ClassMembers, HomeSymbolKind.CalendarBadgeClock, HomeSymbolKind.Megaphone, HomeSymbolKind.Checklist, HomeSymbolKind.ChevronRight),
            HomeSymbolKind.entries,
        )
        assertEquals(0.085f, HomeSymbolPresentation.StrokeFraction)
    }
}
