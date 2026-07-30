package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MorePresentationTest {
    @Test
    fun roleLabelsMatchIos() {
        assertEquals("Instructor", MorePresentation.roleText(true))
        assertEquals("Student", MorePresentation.roleText(false))
    }
}
