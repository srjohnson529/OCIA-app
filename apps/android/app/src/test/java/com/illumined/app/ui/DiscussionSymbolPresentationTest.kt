package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscussionSymbolPresentationTest {
    @Test fun `swift discussion symbols map to scalable android symbols`() {
        val expected = mapOf(
            "text.bubble" to DiscussionSymbolKind.Bubble, "text.bubble.fill" to DiscussionSymbolKind.Bubble,
            "exclamationmark.triangle" to DiscussionSymbolKind.Warning, "chevron.right" to DiscussionSymbolKind.Chevron,
            "checkmark.seal" to DiscussionSymbolKind.CheckSeal, "checkmark.seal.fill" to DiscussionSymbolKind.CheckSeal,
            "checkmark" to DiscussionSymbolKind.Check, "arrowshape.turn.up.left" to DiscussionSymbolKind.Reply,
            "pencil" to DiscussionSymbolKind.Pencil, "trash" to DiscussionSymbolKind.Trash,
            "paperplane.fill" to DiscussionSymbolKind.PaperPlane,
        )
        expected.forEach { (name, symbol) -> assertEquals(symbol, discussionSymbol(name)) }
    }

    @Test fun `unknown discussion symbol has a safe bubble fallback`() =
        assertEquals(DiscussionSymbolKind.Bubble, discussionSymbol("unknown"))
}
