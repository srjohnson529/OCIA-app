package com.illumined.app.ui

internal data class FormationRouteState(val kind: String, val id: String = "", val back: String = FormationRoute.MENU)

internal object FormationRoute {
    const val MENU = "menu"
    const val PRAYER_HUB = "prayer-hub"
    const val COMMON_PRAYERS = "common-prayers"
    const val SELECTED_PRAYERS = "selected-prayers"
    const val ROSARY = "rosary"
    const val PRACTICES = "practices"
    const val EXAMINATION = "examination"
    const val MASS_GUIDE = "mass-guide"
    const val PRAYER = "prayer"
    const val HTML = "html"
    const val MYSTERY = "mystery"
    private val roots = setOf(MENU, PRAYER_HUB, COMMON_PRAYERS, SELECTED_PRAYERS, ROSARY, PRACTICES, EXAMINATION, MASS_GUIDE)

    fun detail(kind: String, id: String, back: String) = listOf(kind, id, back).joinToString("|")

    fun parse(route: String): FormationRouteState {
        if (route in roots) return FormationRouteState(route)
        val pieces = route.split('|', limit = 3)
        return if (pieces.size == 3 && pieces[0] in setOf(PRAYER, HTML, MYSTERY) && pieces[1].isNotBlank() && pieces[2] in roots)
            FormationRouteState(pieces[0], pieces[1], pieces[2]) else FormationRouteState(MENU)
    }
}
