package com.illumined.app.ui

internal enum class LessonCategoryIcon { CROSS, SPARKLES, HEART, PRAYING_HANDS, BOOK }

internal object CategoryPresentation {
    fun icon(name: String) = when (name) {
        "Profession of Faith" -> LessonCategoryIcon.CROSS
        "Celebration of the Christian Mysteries" -> LessonCategoryIcon.SPARKLES
        "Life in Christ" -> LessonCategoryIcon.HEART
        "Christian Prayer" -> LessonCategoryIcon.PRAYING_HANDS
        else -> LessonCategoryIcon.BOOK
    }
}
