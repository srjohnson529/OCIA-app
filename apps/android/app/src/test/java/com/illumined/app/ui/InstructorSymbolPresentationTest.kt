package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InstructorSymbolPresentationTest {
    @Test fun `swift instructor symbols map to scalable android symbols`() {
        val expected=mapOf("person.text.rectangle" to InstructorSymbolKind.Tools,"megaphone" to InstructorSymbolKind.Megaphone,"calendar.badge.clock" to InstructorSymbolKind.CalendarClock,"calendar" to InstructorSymbolKind.Calendar,"checklist" to InstructorSymbolKind.Checklist,"text.bubble" to InstructorSymbolKind.Bubble,"chart.bar" to InstructorSymbolKind.Chart,"key" to InstructorSymbolKind.Key,"plus.circle.fill" to InstructorSymbolKind.PlusCircle,"checkmark.circle.fill" to InstructorSymbolKind.Active,"pause.circle.fill" to InstructorSymbolKind.Paused,"chevron.right" to InstructorSymbolKind.Chevron,"book.closed" to InstructorSymbolKind.Book,"doc.text" to InstructorSymbolKind.Document,"person.crop.circle.fill" to InstructorSymbolKind.Person,"person.3" to InstructorSymbolKind.People,"rosette" to InstructorSymbolKind.Rosette,"circle.grid.cross" to InstructorSymbolKind.Rosary,"eye.slash.fill" to InstructorSymbolKind.EyeSlash,"chevron.right.circle" to InstructorSymbolKind.Expand,"chevron.down.circle.fill" to InstructorSymbolKind.Collapse,"trash" to InstructorSymbolKind.Trash)
        expected.forEach{(name,symbol)->assertEquals(symbol,instructorSymbol(name))}
    }
    @Test fun `all six instructor destinations have explicit symbols`() {
        assertEquals(InstructorToolPresentation.items.size,InstructorToolPresentation.items.map{instructorToolSymbol(it.key)}.distinct().size)
    }
}
