package com.illumined.app.ui

import org.junit.Assert.*
import org.junit.Test

class InstructorWalkthroughTest {
    @Test fun stepsHaveUniqueStableIdsAndBilingualCopy() {
        assertEquals(walkthroughSteps.size, walkthroughSteps.map { it.target }.toSet().size)
        walkthroughSteps.forEach {
            assertTrue(it.title.isNotBlank()); assertTrue(it.titleEs.isNotBlank())
            assertTrue(it.body.isNotBlank()); assertTrue(it.bodyEs.isNotBlank())
            assertTrue(it.title.length <= 100); assertTrue(it.body.length <= 700)
            assertTrue(it.titleEs.length <= 100); assertTrue(it.bodyEs.length <= 700)
        }
    }

    @Test fun skippingRetainsEntryAndDismissalRemovesIt() {
        val tour = InstructorWalkthroughState()
        tour.defer()
        assertTrue(tour.showsToolEntry)
        tour.start()
        assertFalse(tour.showsToolEntry)
        tour.dismiss()
        assertEquals("dismissed", tour.onboarding)
        assertFalse(tour.showsToolEntry)
    }

    @Test fun completionHidesEntryAndPreviewDoesNotChangeOnboarding() {
        val tour = InstructorWalkthroughState()
        tour.defer(); tour.start(); tour.step = tour.steps.lastIndex; tour.next()
        assertEquals("completed", tour.onboarding)
        assertFalse(tour.active)
        tour.preview(emptyMap(), "tools-guides")
        assertEquals("tools-guides", tour.target)
        tour.dismiss()
        assertEquals("completed", tour.onboarding)
    }

    @Test fun adminAndVideoStepsAreConditionalAndSectionsFollowTitle() {
        val tour = InstructorWalkthroughState()
        assertFalse(tour.steps.any { it.target == "more-admin" || it.target == "lesson-video" })
        tour.admin = true; tour.lessonHasVideo = true
        tour.lessonSections = listOf(WalkthroughStep("lesson-part-0", "lessons", "detail", "Definition", "Read", "Definición", "Leer"))
        assertTrue(tour.steps.any { it.target == "more-admin" })
        assertTrue(tour.steps.any { it.target == "lesson-video" })
        val title = tour.steps.indexOfFirst { it.target == "lesson-title" }
        assertEquals("lesson-part-0", tour.steps[title + 1].target)
        assertEquals("codes-instructor-status", tour.steps.last().target)
    }

    @Test fun invalidRemoteTextFallsBackAndPreviewRestoreIgnoresPublishedText() {
        val valid = mapOf("title" to "Hello", "body" to "Body", "titleEs" to "Hola", "bodyEs" to "Texto")
        assertEquals(mapOf("welcome" to valid), decodeWalkthroughText(mapOf("welcome" to valid)))
        assertTrue(decodeWalkthroughText(mapOf("welcome" to valid.minus("bodyEs"))).isEmpty())
        assertTrue(decodeWalkthroughText(mapOf("welcome" to valid.plus("title" to "x".repeat(101)))).isEmpty())
        val tour = InstructorWalkthroughState()
        tour.published = mapOf("welcome" to valid)
        tour.start(); assertEquals("Hello", tour.text()["title"])
        tour.preview(emptyMap(), "welcome")
        assertEquals(walkthroughSteps.first().title, tour.text()["title"])
    }
}
