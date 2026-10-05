package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RitePreparationDateTest {
    @Test fun parishMidnightRespectsDaylightSaving() {
        assertEquals("2026-03-09T04:00:00Z", RitePreparationDate.expiry("2026-03-08", "America/New_York").toInstant().toString())
        assertEquals("2026-11-02T05:00:00Z", RitePreparationDate.expiry("2026-11-01", "America/New_York").toInstant().toString())
        assertEquals("2027-01-01T00:00:00Z", RitePreparationDate.expiry("2026-12-31", "UTC").toInstant().toString())
    }
    @Test(expected = Exception::class) fun rejectsInvalidDate() { RitePreparationDate.expiry("2026-02-30", "UTC") }
    @Test(expected = Exception::class) fun rejectsInvalidZone() { RitePreparationDate.expiry("2026-09-08", "invalid-zone") }
}
