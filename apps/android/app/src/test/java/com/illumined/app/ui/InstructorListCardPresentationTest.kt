package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InstructorListCardPresentationTest {
    @Test
    fun `populated manager cards use the shared iOS surface tokens`() {
        assertEquals(16, InstructorListCardPresentation.CornerRadius)
        assertEquals(0.22f, InstructorListCardPresentation.GoldBorderAlpha)
        assertEquals(6, InstructorListCardPresentation.ShadowElevation)
    }
}
