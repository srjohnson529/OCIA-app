package com.illumined.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class LessonReaderPolicyTest {
    @Test
    fun wrapperContainsIosTypographyAndSpacing() {
        val html = LessonReaderPolicy.wrapHtml("<h3>Grace</h3><blockquote>Gift</blockquote><ul><li>Faith</li></ul>")
        assertTrue(html.contains("font-family: Georgia, serif"))
        assertTrue(html.contains("color: #1f2933"))
        assertTrue(html.contains("font-size: 17px"))
        assertTrue(html.contains("line-height: 1.55"))
        assertTrue(html.contains("padding: 0 2px 24px"))
        assertTrue(html.contains("border-left: 4px solid #b88a44"))
        assertTrue(html.contains("li { margin-bottom: 8px; }"))
    }

    @Test
    fun wrapperPreservesLessonMarkup() {
        val content = "<p>The lesson body</p>"
        assertTrue(LessonReaderPolicy.wrapHtml(content).contains("<body>$content</body>"))
    }
}
