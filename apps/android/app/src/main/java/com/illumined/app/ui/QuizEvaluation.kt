package com.illumined.app.ui

import com.illumined.app.data.QuizQuestion

internal sealed interface QuizEvaluationResult {
    data object Incomplete : QuizEvaluationResult
    data class Incorrect(val score: Int, val total: Int, val incorrectQuestionIds: Set<String>) : QuizEvaluationResult
    data object Perfect : QuizEvaluationResult
}

internal object QuizEvaluation {
    fun evaluate(questions: List<QuizQuestion>, answers: Map<String, Int>): QuizEvaluationResult {
        if (answers.keys.count { answerId -> questions.any { it.id == answerId } } != questions.size) {
            return QuizEvaluationResult.Incomplete
        }
        val incorrect = questions.filter { answers[it.id] != it.correctAnswerIndex }.mapTo(linkedSetOf()) { it.id }
        return if (incorrect.isEmpty()) QuizEvaluationResult.Perfect
        else QuizEvaluationResult.Incorrect(questions.size - incorrect.size, questions.size, incorrect)
    }
}
