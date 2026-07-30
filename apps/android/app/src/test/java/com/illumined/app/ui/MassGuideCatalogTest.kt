package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MassGuideCatalogTest {
    @Test fun catalogMatchesIosInventory() {
        assertEquals(4, MassGuideCatalog.parts.size)
        assertEquals(24, MassGuideCatalog.parts.sumOf { it.rows.size })
        assertEquals(5, MassGuideCatalog.communionRite.rows.size)
        assertEquals(25, MassGuideCatalog.prayers.size)
        assertEquals(listOf("I", "II", "III", "IV"), MassGuideCatalog.parts.map { it.number })
    }

    @Test fun everyPrayerLinkResolvesAndDailyReadingsMatchIos() {
        val linkedIds = (MassGuideCatalog.parts.flatMap { it.rows } + MassGuideCatalog.communionRite.rows).flatMap { it.prayerIds }
        assertTrue(linkedIds.isNotEmpty())
        assertTrue(linkedIds.all { it in MassGuideCatalog.prayersById })
        assertEquals(linkedIds.size, linkedIds.distinct().size)
        assertEquals("https://bible.usccb.org/daily-bible-reading", MassGuideCatalog.dailyReadingsUrl)
        assertTrue(MassGuideCatalog.parts.single { it.id == "liturgy-word" }.showsDailyReadings)
    }
}
