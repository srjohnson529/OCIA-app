package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LessonSymbolPresentationTest {
    @Test
    fun `lesson and quiz symbols cover the current iOS intents`() {
        assertEquals(
            listOf("BookClosed", "DocumentText", "Clock", "CheckCircle", "ChevronRight", "PlayCircle", "RadioOff", "RadioOn", "WarningCircle"),
            LessonSymbolKind.entries.map { it.name },
        )
        assertEquals(0.085f, LessonSymbolPresentation.StrokeFraction)
    }
}
