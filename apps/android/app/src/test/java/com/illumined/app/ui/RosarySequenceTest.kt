package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RosarySequenceTest {
    private val prayers = RosaryPrayers("cross", "creed", "father", "hail", "glory", "fatima", "queen", "conclusion")
    private val set = RosarySet("joyful", "Joyful", "", (1..5).map { RosaryMystery("Mystery $it", "Scripture $it") })

    @Test fun guidedRosaryMatchesIosEightyOneStepSequence() {
        val steps = buildRosarySequence(prayers, set)
        assertEquals(81, steps.size)
        assertEquals("Sign of the Cross", steps.first().title)
        assertEquals("Rosary Completed", steps.last().title)
        assertEquals(50, steps.count { it.decadeCount != null })
    }

    @Test fun eachDecadeCountsOneThroughTen() {
        val counts = buildRosarySequence(prayers, set).mapNotNull { it.decadeCount }
        assertEquals(List(5) { (1..10).toList() }.flatten(), counts)
    }
}
