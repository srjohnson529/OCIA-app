package com.illumined.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LessonCatalogCategoryTest {
    @Test
    fun classroomCategoryUsesTheClassId() {
        assertEquals("OCIA TOTC Lessons", classroomCategoryName("OCIA TOTC"))
        assertEquals("STM OCIA Lessons", classroomCategoryName("  STM OCIA  "))
    }

    @Test
    fun classroomCategoryHasALegacyFallback() {
        assertEquals("Classroom Lessons", classroomCategoryName("   "))
    }
}
