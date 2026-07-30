package com.illumined.app.ui

import com.illumined.app.data.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizEvaluationTest {
    private val questions = listOf(
        QuizQuestion("q1", "First?", listOf("A", "B"), 0),
        QuizQuestion("q2", "Second?", listOf("A", "B"), 1),
    )

    @Test
    fun incompleteRequiresEveryCatalogQuestion() {
        assertEquals(QuizEvaluationResult.Incomplete, QuizEvaluation.evaluate(questions, mapOf("q1" to 0)))
        assertEquals(QuizEvaluationResult.Incomplete, QuizEvaluation.evaluate(questions, mapOf("unrelated" to 0, "q1" to 0)))
    }

    @Test
    fun incorrectReturnsScoreAndQuestionIds() {
        val result = QuizEvaluation.evaluate(questions, mapOf("q1" to 1, "q2" to 1))
        assertEquals(QuizEvaluationResult.Incorrect(1, 2, setOf("q1")), result)
    }

    @Test
    fun allCorrectIsPerfect() {
        assertTrue(QuizEvaluation.evaluate(questions, mapOf("q1" to 0, "q2" to 1)) is QuizEvaluationResult.Perfect)
    }
}
