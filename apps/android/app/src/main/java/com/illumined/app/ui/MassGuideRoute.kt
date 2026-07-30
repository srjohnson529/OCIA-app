package com.illumined.app.ui

internal sealed interface MassGuideDestination {
    data object Parts : MassGuideDestination
    data class Part(val value: MassGuidePart) : MassGuideDestination
    data class Prayer(val value: MassPrayerOption) : MassGuideDestination
}

internal object MassGuideRoute {
    const val PARTS = "parts"
    fun part(id: String) = "part:$id"
    fun prayer(id: String) = "prayer:$id"

    fun resolve(route: String): MassGuideDestination = when {
        route == PARTS -> MassGuideDestination.Parts
        route.startsWith("part:") -> MassGuideCatalog.parts.firstOrNull { it.id == route.removePrefix("part:") }
            ?.let(MassGuideDestination::Part) ?: MassGuideDestination.Parts
        route.startsWith("prayer:") -> MassGuideCatalog.prayersById[route.removePrefix("prayer:")]
            ?.let(MassGuideDestination::Prayer) ?: MassGuideDestination.Parts
        else -> MassGuideDestination.Parts
    }
}
