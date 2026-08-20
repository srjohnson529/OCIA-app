package com.illumined.app.data

import com.illumined.app.ui.formationGameTermCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogIntegrityTest {
    private fun raw(name: String): String {
        val candidates = listOf(
            File("src/main/res/raw/$name.json"),
            File("app/src/main/res/raw/$name.json"),
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun count(pattern: String, text: String) = Regex(pattern).findAll(text).count()

    private fun between(text: String, start: String, end: String): String =
        text.substringAfter(start).substringBefore(end)

    @Test fun lessonCatalogMatchesIosInventory() {
        val lessons = raw("lessons")
        // Each lesson and each quiz question has an ID: 73 + 805.
        assertEquals(878, count("\\\"id\\\"\\s*:", lessons))
        assertEquals(73, count("\\\"title\\\"\\s*:", lessons))
        assertEquals(32, count("\\\"category\\\"\\s*:\\s*\\\"Profession of Faith\\\"", lessons))
        assertEquals(12, count("\\\"category\\\"\\s*:\\s*\\\"Celebration of the Christian Mysteries\\\"", lessons))
        assertEquals(24, count("\\\"category\\\"\\s*:\\s*\\\"Life in Christ\\\"", lessons))
        assertEquals(5, count("\\\"category\\\"\\s*:\\s*\\\"Christian Prayer\\\"", lessons))
        assertEquals(805, count("\\\"question\\\"\\s*:", lessons))
        assertTrue(lessons.contains("\"lessons\""))
    }

    @Test fun formationAndAwardCatalogsMatchIosInventory() {
        val formation = raw("spiritual_formation")
        val commonPrayers = between(formation, "\"commonPrayers\"", "\"rosary\"")
        val rosary = between(formation, "\"rosary\"", "\"spiritualPractices\"")
        val practices = formation.substringAfter("\"spiritualPractices\"")
        assertEquals(27, count("\\\"id\\\"\\s*:", commonPrayers))
        assertEquals(4, count("\\\"name\\\"\\s*:\\s*\\\"The [^\\\"]+ Mysteries\\\"", rosary))
        assertEquals(6, count("\\\"id\\\"\\s*:", practices))
        assertEquals(9, count("\\\"id\\\"\\s*:", raw("achievements")))
        assertEquals(72, formationGameTermCount)
    }
}
