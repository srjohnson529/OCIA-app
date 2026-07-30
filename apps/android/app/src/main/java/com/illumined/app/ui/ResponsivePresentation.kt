package com.illumined.app.ui

internal object ResponsivePresentation {
    const val StackedTrackerFontScale = 1.5f
    fun usesStackedTracker(fontScale: Float): Boolean = fontScale >= StackedTrackerFontScale
}
