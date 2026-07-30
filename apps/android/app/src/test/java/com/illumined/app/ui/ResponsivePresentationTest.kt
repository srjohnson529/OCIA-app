package com.illumined.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsivePresentationTest {
    @Test fun trackerKeepsIosColumnsAtNormalTextAndStacksForAccessibilityText() {
        assertFalse(ResponsivePresentation.usesStackedTracker(1f))
        assertFalse(ResponsivePresentation.usesStackedTracker(1.3f))
        assertTrue(ResponsivePresentation.usesStackedTracker(1.5f))
        assertTrue(ResponsivePresentation.usesStackedTracker(2f))
    }
}
