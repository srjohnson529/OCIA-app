package com.illumined.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscussionInteractionPolicyTest {
    @Test
    fun editAndReplySubmissionRequireContentAndIdleState() {
        assertTrue(DiscussionInteractionPolicy.canSubmit("A thoughtful response", false))
        assertFalse(DiscussionInteractionPolicy.canSubmit(" \n", false))
        assertFalse(DiscussionInteractionPolicy.canSubmit("A thoughtful response", true))
    }

    @Test
    fun ownershipRequiresAuthenticatedAuthorMatch() {
        assertTrue(DiscussionInteractionPolicy.ownsPost("user-1", "user-1"))
        assertFalse(DiscussionInteractionPolicy.ownsPost("user-2", "user-1"))
        assertFalse(DiscussionInteractionPolicy.ownsPost(null, "user-1"))
    }
}
