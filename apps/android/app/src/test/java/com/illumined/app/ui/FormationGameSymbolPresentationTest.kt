package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormationGameSymbolPresentationTest {
    @Test
    fun menuDestinationsUseTheCurrentSwiftSymbolIntents() {
        assertEquals(FormationGameSymbolKind.SearchDocument, formationGameMenuSymbol("Match Terms"))
        assertEquals(FormationGameSymbolKind.Checklist, formationGameMenuSymbol("Name That Term"))
    }

    @Test
    fun everySwiftGameSymbolIntentHasAScalableDrawing() {
        assertEquals(
            setOf(
                FormationGameSymbolKind.Puzzle,
                FormationGameSymbolKind.SearchDocument,
                FormationGameSymbolKind.Checklist,
                FormationGameSymbolKind.EmptyCircle,
                FormationGameSymbolKind.CheckCircleFilled,
                FormationGameSymbolKind.XCircleFilled,
                FormationGameSymbolKind.ArrowCircleFilled,
            ),
            FormationGameSymbolKind.entries.toSet(),
        )
    }

    @Test
    fun androidUsesTheCompleteCurrentFormationGameDeck() {
        assertEquals(72, formationGameTermCount)
    }
}
