package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryPresentationTest {
    @Test
    fun canonicalCategoriesMapToIosSymbolIntent() {
        assertEquals(LessonCategoryIcon.CROSS, CategoryPresentation.icon("Profession of Faith"))
        assertEquals(LessonCategoryIcon.SPARKLES, CategoryPresentation.icon("Celebration of the Christian Mysteries"))
        assertEquals(LessonCategoryIcon.HEART, CategoryPresentation.icon("Life in Christ"))
        assertEquals(LessonCategoryIcon.PRAYING_HANDS, CategoryPresentation.icon("Christian Prayer"))
    }

    @Test
    fun unknownCategoryUsesBookFallback() {
        assertEquals(LessonCategoryIcon.BOOK, CategoryPresentation.icon("Other"))
    }
}
