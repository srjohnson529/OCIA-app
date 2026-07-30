package com.illumined.app.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object AppChromePresentation {
    val HeaderContentHeight = 56.dp
    val HeaderIconSize = 46.dp
    val TabIconSize = 22.dp
    val TabLabelSize = 11.5.sp
    val TabCornerRadius = 13.dp
    const val HeaderAccessibilityLabel = "Illumined. Being, Truth, Goodness."
    val TabLabels = listOf("Home", "Lessons", "Discussion", "Formation", "More")

    fun fixedFontSize(value: Float, fontScale: Float): Float = value / fontScale.coerceAtLeast(0.1f)
}
