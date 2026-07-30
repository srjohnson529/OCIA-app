package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverviewPresentationTest {
    @Test
    fun `error blocks only before any overview has loaded`() {
        assertEquals(OverviewErrorPresentation.Blocking, OverviewPresentation.errorPresentation(false, "Offline"))
        assertEquals(OverviewErrorPresentation.Alert, OverviewPresentation.errorPresentation(true, "Offline"))
    }

    @Test
    fun `missing and blank errors have no presentation`() {
        assertEquals(OverviewErrorPresentation.None, OverviewPresentation.errorPresentation(false, null))
        assertEquals(OverviewErrorPresentation.None, OverviewPresentation.errorPresentation(true, " "))
    }

    @Test
    fun `cached overview prevents unrelated sections from becoming unavailable`() {
        assertNull(OverviewPresentation.sectionLoadError(true, "Assignment listener failed"))
        assertEquals("Could not load", OverviewPresentation.sectionLoadError(false, "Could not load"))
    }
}
