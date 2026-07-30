package com.illumined.app.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppChromePresentationTest {
    @Test fun tabChromeMatchesSwiftSourceMeasurements() {
        assertEquals(22.dp, AppChromePresentation.TabIconSize)
        assertEquals(11.5.sp, AppChromePresentation.TabLabelSize)
        assertEquals(13.dp, AppChromePresentation.TabCornerRadius)
        assertEquals(listOf("Home", "Lessons", "Discussion", "Formation", "More"), AppChromePresentation.TabLabels)
    }

    @Test fun brandChromeMatchesSwiftSourceMeasurementsAndAccessibilityCopy() {
        assertEquals(46.dp, AppChromePresentation.HeaderIconSize)
        assertEquals("Illumined. Being, Truth, Goodness.", AppChromePresentation.HeaderAccessibilityLabel)
    }

    @Test fun explicitIosChromeSizesRemainStableAtLargeAndroidFontScale() {
        assertEquals(11.5f / 1.3f, AppChromePresentation.fixedFontSize(11.5f, 1.3f), 0.001f)
        assertEquals(22f / 2f, AppChromePresentation.fixedFontSize(22f, 2f), 0.001f)
    }
}
