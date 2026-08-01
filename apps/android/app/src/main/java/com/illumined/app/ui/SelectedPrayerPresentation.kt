package com.illumined.app.ui

internal object SelectedPrayerPresentation {
    fun orderedVisibleIds(catalogIds: List<String>, selectedIds: Set<String>): List<String> =
        catalogIds.filter(selectedIds::contains)

    fun shouldShowCard(catalogIds: List<String>, selectedIds: Set<String>): Boolean =
        orderedVisibleIds(catalogIds, selectedIds).isNotEmpty()
}
