package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementProgressTest {
    private val badges = listOf(
        Badge("one", "One", "First", "cross.fill"),
        Badge("two", "Two", "Second", "sparkles"),
    )

    @Test fun countsOnlyCatalogBadgesLikeIos() {
        assertEquals(1, knownEarnedBadgeCount(badges, setOf("one", "legacy-unknown")))
    }

    @Test fun duplicateAndUnknownProfileIdsCannotInflateProgress() {
        assertEquals(2, knownEarnedBadgeCount(badges, setOf("one", "two", "future")))
        assertEquals(0, knownEarnedBadgeCount(badges, emptySet()))
    }
}
