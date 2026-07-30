package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MoreMenuSymbolPresentationTest {
    @Test fun `all current iOS More routes have distinct symbol intents`() {
        val titles = listOf("Awards", "Chat", "Account", "Notifications", "Games", "Instructor Tools", "Admin Tools")
        assertEquals(MoreMenuSymbolKind.entries, titles.map(::moreMenuSymbol))
    }
}
