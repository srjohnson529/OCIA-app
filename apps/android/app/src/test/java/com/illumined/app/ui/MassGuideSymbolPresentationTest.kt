package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MassGuideSymbolPresentationTest {
    @Test
    fun fourRitesAndEmbeddedCommunionUseSwiftSymbolIntents() {
        assertEquals(MassGuideSymbolKind.People, massPartSymbol("introductory-rites"))
        assertEquals(MassGuideSymbolKind.Book, massPartSymbol("liturgy-word"))
        assertEquals(MassGuideSymbolKind.Eucharist, massPartSymbol("liturgy-eucharist"))
        assertEquals(MassGuideSymbolKind.Walking, massPartSymbol("concluding-rites"))
        assertEquals(MassGuideSymbolKind.HandsSparkles, massPartSymbol("communion-rite"))
    }

    @Test
    fun allCurrentPrayerOptionsResolveToTheirSwiftSymbolIntent() {
        val expected = mapOf(
            "confiteor" to MassGuideSymbolKind.PersonAlert,
            "dialogue" to MassGuideSymbolKind.TextBubble,
            "tropes" to MassGuideSymbolKind.QuoteBubble,
            "sprinkling" to MassGuideSymbolKind.Drop,
            "gloria" to MassGuideSymbolKind.Sun,
            "collect" to MassGuideSymbolKind.HandsSparkles,
            "nicene" to MassGuideSymbolKind.Scroll,
            "apostles" to MassGuideSymbolKind.Scroll,
            "universal-prayer" to MassGuideSymbolKind.PeopleTwo,
            "presentation-gifts" to MassGuideSymbolKind.Gift,
            "prayer-over-offerings" to MassGuideSymbolKind.Tray,
            "preface-dialogue" to MassGuideSymbolKind.HeartUp,
            "ep1" to MassGuideSymbolKind.Book,
            "ep2" to MassGuideSymbolKind.Book,
            "ep3" to MassGuideSymbolKind.Book,
            "ep4" to MassGuideSymbolKind.Book,
            "sanctus" to MassGuideSymbolKind.Sparkles,
            "memorial-acclamation" to MassGuideSymbolKind.Cross,
            "great-amen" to MassGuideSymbolKind.CheckSeal,
            "lords-prayer" to MassGuideSymbolKind.HandsSparkles,
            "agnus-dei" to MassGuideSymbolKind.Leaf,
            "communion-invitation" to MassGuideSymbolKind.Grid,
            "prayer-after-communion" to MassGuideSymbolKind.HeartText,
            "final-blessing" to MassGuideSymbolKind.Cross,
            "dismissal" to MassGuideSymbolKind.ForwardCircle,
        )
        assertEquals(expected.keys, MassGuideCatalog.prayers.map { it.id }.toSet())
        expected.forEach { (id, symbol) -> assertEquals(symbol, massPrayerSymbol(id)) }
    }

    @Test
    fun completeSwiftMassSymbolVocabularyHasDrawings() {
        assertEquals(24, MassGuideSymbolKind.entries.size)
    }
}
