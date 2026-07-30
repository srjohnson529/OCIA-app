package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstructorEditorRestorationTest {
    private data class Record(val id: String, val title: String)

    @Test fun resolvesLiveRecordFromStableSavedId() {
        val values = listOf(Record("a", "Old"), Record("b", "Current"))
        assertEquals(values[1], restoreEditedRecord(values, "b", Record::id))
    }

    @Test fun missingOrClearedIdDoesNotRestoreStaleObject() {
        assertNull(restoreEditedRecord(listOf(Record("a", "Only")), "removed", Record::id))
        assertNull(restoreEditedRecord(listOf(Record("a", "Only")), null, Record::id))
    }
}
