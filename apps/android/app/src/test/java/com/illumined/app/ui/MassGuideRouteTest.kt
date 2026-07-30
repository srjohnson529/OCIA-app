package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MassGuideRouteTest {
    @Test fun stableRoutesRestoreKnownPartsAndPrayers() {
        val part = MassGuideCatalog.parts.first()
        val prayer = MassGuideCatalog.prayersById.values.first()
        assertEquals(part, (MassGuideRoute.resolve(MassGuideRoute.part(part.id)) as MassGuideDestination.Part).value)
        assertEquals(prayer, (MassGuideRoute.resolve(MassGuideRoute.prayer(prayer.id)) as MassGuideDestination.Prayer).value)
    }

    @Test fun staleOrMalformedRoutesSafelyReturnToRoot() {
        assertTrue(MassGuideRoute.resolve("part:missing") is MassGuideDestination.Parts)
        assertTrue(MassGuideRoute.resolve("unknown") is MassGuideDestination.Parts)
    }
}
