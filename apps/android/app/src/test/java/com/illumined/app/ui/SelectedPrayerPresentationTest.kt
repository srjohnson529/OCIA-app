package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectedPrayerPresentationTest {
    private val catalogIds = listOf("our-father", "hail-mary", "glory-be")

    @Test
    fun selectedPrayersRemainInCatalogOrder() {
        assertEquals(
            listOf("our-father", "glory-be"),
            SelectedPrayerPresentation.orderedVisibleIds(catalogIds, setOf("glory-be", "our-father")),
        )
    }

    @Test
    fun cardAppearsOnlyWhenAValidPrayerIsSelected() {
        assertFalse(SelectedPrayerPresentation.shouldShowCard(catalogIds, emptySet()))
        assertFalse(SelectedPrayerPresentation.shouldShowCard(catalogIds, setOf("retired-prayer")))
        assertTrue(SelectedPrayerPresentation.shouldShowCard(catalogIds, setOf("hail-mary")))
    }
}
