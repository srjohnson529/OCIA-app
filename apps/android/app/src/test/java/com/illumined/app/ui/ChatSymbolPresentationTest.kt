package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatSymbolPresentationTest {
    @Test fun `chat covers current iOS message and send symbols`() {
        assertEquals(listOf("Message", "MessageBadge", "PaperPlane"), ChatSymbolKind.entries.map { it.name })
    }
}
