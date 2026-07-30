package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExaminationCatalogTest {
    @Test fun catalogMatchesIosSectionAndItemCounts() {
        assertEquals(30, ExaminationCatalog.sections.size)
        assertEquals(202, ExaminationCatalog.sections.sumOf { it.items.size })
        assertTrue(ExaminationCatalog.sections.all { it.title.isNotBlank() && it.items.all(String::isNotBlank) })
    }

    @Test fun examinationIncludesPrivateOpeningAndClosingPrayers() {
        assertTrue(ExaminationCatalog.preExamPrayer.startsWith("Come, Holy Spirit"))
        assertTrue(ExaminationCatalog.actOfContrition.startsWith("O my God"))
    }
}
