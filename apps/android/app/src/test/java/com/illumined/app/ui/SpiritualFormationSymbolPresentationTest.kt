package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SpiritualFormationSymbolPresentationTest {
    @Test
    fun breviaryLinksUseAllEightCurrentSwiftSymbolIntents() {
        val expected = listOf(
            SpiritualFormationSymbolKind.Book,
            SpiritualFormationSymbolKind.TextBook,
            SpiritualFormationSymbolKind.Sunrise,
            SpiritualFormationSymbolKind.Sun,
            SpiritualFormationSymbolKind.Sunset,
            SpiritualFormationSymbolKind.MoonStars,
            SpiritualFormationSymbolKind.Speaker,
            SpiritualFormationSymbolKind.MusicTv,
        )
        assertEquals(expected, breviaryLinks.map { breviarySymbol(it.symbol) })
    }

    @Test
    fun completeCurrentFormationSymbolVocabularyHasScalableDrawings() {
        assertEquals(22, SpiritualFormationSymbolKind.entries.size)
    }

    @Test
    fun unknownBreviarySymbolFallsBackToBookWithoutAPlatformGlyph() {
        assertEquals(SpiritualFormationSymbolKind.Book, breviarySymbol("unknown"))
    }
}
