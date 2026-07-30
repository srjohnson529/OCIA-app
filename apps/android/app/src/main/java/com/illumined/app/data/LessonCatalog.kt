package com.illumined.app.data

import android.content.Context
import com.illumined.app.R
import org.json.JSONArray
import org.json.JSONObject

data class LessonCategory(
    val name: String,
    val lessons: List<CatechismLesson>,
)

data class CatechismLesson(
    val id: String,
    val title: String,
    val category: String,
    val contentHtml: String,
    val videoUrl: String?,
    val quiz: List<QuizQuestion>,
)

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
)

object LessonCatalog {
    private val categoryOrder = listOf(
        "Profession of Faith",
        "Celebration of the Christian Mysteries",
        "Life in Christ",
        "Christian Prayer",
    )

    fun load(context: Context): Result<List<LessonCategory>> = runCatching {
        val json = context.resources.openRawResource(R.raw.lessons)
            .bufferedReader()
            .use { it.readText() }
        val lessonsArray = JSONObject(json).getJSONArray("lessons")
        val lessons = buildList {
            for (index in 0 until lessonsArray.length()) {
                add(lessonsArray.getJSONObject(index).toLesson())
            }
        }

        categoryOrder.mapNotNull { categoryName ->
            lessons.filter { it.category == categoryName }
                .takeIf { it.isNotEmpty() }
                ?.let { LessonCategory(categoryName, it) }
        }
    }
}

private fun JSONObject.toLesson(): CatechismLesson {
    val quizValue = opt("quiz")
    val questions = when (quizValue) {
        is JSONArray -> quizValue
        is JSONObject -> quizValue.optJSONArray("questions") ?: JSONArray()
        else -> JSONArray()
    }

    return CatechismLesson(
        id = getString("id"),
        title = getString("title"),
        category = getString("category"),
        contentHtml = getString("contentHTML"),
        videoUrl = optString("videoUrl").takeIf { it.isNotBlank() },
        quiz = buildList {
            for (index in 0 until questions.length()) {
                val question = questions.getJSONObject(index)
                val options = question.getJSONArray("options")
                add(
                    QuizQuestion(
                        id = question.optString("id", "$index-${question.getString("question").hashCode()}"),
                        question = question.getString("question"),
                        options = buildList {
                            for (optionIndex in 0 until options.length()) add(options.getString(optionIndex))
                        },
                        correctAnswerIndex = if (question.has("correct")) {
                            question.getInt("correct")
                        } else {
                            question.getInt("correctAnswerIndex")
                        },
                    ),
                )
            }
        },
    )
}
