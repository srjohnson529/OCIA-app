package com.illumined.app.ui

internal enum class OverviewErrorPresentation {
    None,
    Blocking,
    Alert,
}

internal object OverviewPresentation {
    fun errorPresentation(hasOverview: Boolean, error: String?): OverviewErrorPresentation = when {
        error.isNullOrBlank() -> OverviewErrorPresentation.None
        hasOverview -> OverviewErrorPresentation.Alert
        else -> OverviewErrorPresentation.Blocking
    }

    fun sectionLoadError(hasOverview: Boolean, error: String?): String? =
        error?.takeUnless { hasOverview }
}
