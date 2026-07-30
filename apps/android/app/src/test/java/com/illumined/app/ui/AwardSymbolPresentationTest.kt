package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AwardSymbolPresentationTest {
    @Test fun `all current iOS badge symbols map without emoji`() {
        val names = listOf("cross.fill", "sparkles", "heart.fill", "hands.sparkles.fill", "graduationcap.fill", "sun.max.fill", "lightbulb.fill", "drop.fill", "crown.fill")
        assertEquals(AwardSymbolKind.entries.dropLast(1), names.map(::awardSymbolKind))
        assertEquals(AwardSymbolKind.Sparkles, awardSymbolKind(null))
    }
}
