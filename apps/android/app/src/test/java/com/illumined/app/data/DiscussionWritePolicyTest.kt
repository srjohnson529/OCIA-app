package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscussionWritePolicyTest {
    @Test fun originalResponseValidationMatchesIosOrderAndCopy() {
        assertEquals("Please sign in before posting.", DiscussionWritePolicy.postError(null, "", "", true))
        assertEquals("Please join a class before posting.", DiscussionWritePolicy.postError("u1", "", "", true))
        assertEquals("Please write a response before posting.", DiscussionWritePolicy.postError("u1", "c1", "  ", true))
        assertEquals("You already posted a response for this discussion. Edit or delete your original response to post a new one.", DiscussionWritePolicy.postError("u1", "c1", "Response", true))
        assertNull(DiscussionWritePolicy.postError("u1", "c1", "Response", false))
    }

    @Test fun replyValidationMatchesIosOrderAndCopy() {
        assertEquals("Please sign in before replying.", DiscussionWritePolicy.replyError(null, "", ""))
        assertEquals("Please join a class before replying.", DiscussionWritePolicy.replyError("u1", "", ""))
        assertEquals("Please write a reply before posting.", DiscussionWritePolicy.replyError("u1", "c1", "\n"))
        assertNull(DiscussionWritePolicy.replyError("u1", "c1", "Reply"))
    }
}
