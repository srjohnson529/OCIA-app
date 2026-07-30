package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewListenerScopesTest {
    @Test
    fun `configured learner observes every live iOS overview source`() {
        assertEquals(
            listOf(
                OverviewListenerScope("assignmentCompletions", "userId", "user-1"),
                OverviewListenerScope("assignments", "classId", "class-1"),
                OverviewListenerScope("classSchedule", "classId", "class-1"),
                OverviewListenerScope("discussionPrompts", "classId", "class-1", activeOnly = true),
                OverviewListenerScope("prayerRequests", "classId", "class-1"),
            ),
            overviewListenerScopes("user-1", "class-1"),
        )
    }

    @Test
    fun `unconfigured learner observes only their completion records`() {
        assertEquals(
            listOf(OverviewListenerScope("assignmentCompletions", "userId", "user-1")),
            overviewListenerScopes("user-1", null),
        )
    }
}
