package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormationRouteTest {
    @Test fun stableDetailRouteRoundTrips() {
        assertEquals(FormationRouteState(FormationRoute.PRAYER, "hail-mary", FormationRoute.COMMON_PRAYERS),
            FormationRoute.parse(FormationRoute.detail(FormationRoute.PRAYER, "hail-mary", FormationRoute.COMMON_PRAYERS)))
    }

    @Test fun malformedOrUnsupportedRouteFallsBackToMenu() {
        assertEquals(FormationRouteState(FormationRoute.MENU), FormationRoute.parse("prayer||bad-back"))
        assertEquals(FormationRouteState(FormationRoute.MENU), FormationRoute.parse("unknown"))
    }

    @Test fun selectedPrayerDetailsReturnToSelectedPrayers() {
        assertEquals(
            FormationRouteState(FormationRoute.PRAYER, "hail-mary", FormationRoute.SELECTED_PRAYERS),
            FormationRoute.parse(FormationRoute.detail(FormationRoute.PRAYER, "hail-mary", FormationRoute.SELECTED_PRAYERS)),
        )
    }
}
