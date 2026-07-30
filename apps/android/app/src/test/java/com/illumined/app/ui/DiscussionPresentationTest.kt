package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscussionPresentationTest {
    @Test
    fun unavailableAndEmptyCopyMatchesIos() {
        assertEquals("Discussions Unavailable", DiscussionPresentation.errorTitle)
        assertEquals("No Discussions Yet", DiscussionPresentation.emptyTitle)
        assertEquals("Discussion assignments will appear here after they are added.", DiscussionPresentation.emptyDescription)
    }
}
