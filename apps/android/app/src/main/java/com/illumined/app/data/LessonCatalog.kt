package com.illumined.app.data

import android.content.Context
import com.illumined.app.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
        grouped(bundledLessons(context))
    }

    fun listenForClassroom(
        context: Context,
        classId: String,
        onUpdate: (Result<List<LessonCategory>>) -> Unit,
    ): AutoCloseable {
        val canonical = runCatching { bundledLessons(context) }.getOrElse {
            onUpdate(Result.failure(it))
            return AutoCloseable { }
        }
        if (classId.isBlank()) {
            onUpdate(Result.success(grouped(canonical)))
            return AutoCloseable { }
        }

        val classroom = FirebaseFirestore.getInstance().collection("classrooms").document(classId)
        var overrides: Map<String, Map<String, Any>>? = null
        var customLessons: List<Pair<String, Map<String, Any>>>? = null
        var hiddenCategories: Set<String>? = null
        var showClassroomLessons: Boolean? = null

        fun publish() {
            val overrideData = overrides ?: return
            val customData = customLessons ?: return
            val hidden = hiddenCategories ?: return
            val showCustom = showClassroomLessons ?: return
            val merged = canonical.filterNot { it.category in hidden }.map { lesson ->
                overrideData[lesson.id]?.toLesson(lesson.id, lesson) ?: lesson
            }.toMutableList()
            if (showCustom) customData.mapNotNullTo(merged) { (id, data) ->
                data.toLesson(id, null)?.copy(category = classroomCategoryName(classId))
            }
            onUpdate(Result.success(grouped(merged)))
        }

        fun fail(error: Exception?) {
            onUpdate(Result.failure(error ?: IllegalStateException("Classroom lessons could not be loaded.")))
        }

        val listeners = mutableListOf<ListenerRegistration>()
        listeners += classroom.collection("lessonOverrides").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener fail(error)
            overrides = snapshot?.documents?.associate { it.id to it.data.orEmpty() }.orEmpty()
            publish()
        }
        listeners += classroom.collection("customLessons").whereEqualTo("isPublished", true).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener fail(error)
            customLessons = snapshot?.documents?.map { it.id to it.data.orEmpty() }.orEmpty()
            publish()
        }
        listeners += classroom.collection("settings").document("lessonLibrary").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener fail(error)
            hiddenCategories = (snapshot?.get("hiddenCanonicalCategories") as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            showClassroomLessons = snapshot?.getBoolean("showClassroomLessons") ?: true
            publish()
        }
        return AutoCloseable { listeners.forEach(ListenerRegistration::remove) }
    }

    private fun bundledLessons(context: Context): List<CatechismLesson> {
        val json = context.resources.openRawResource(R.raw.lessons)
            .bufferedReader()
            .use { it.readText() }
        val lessonsArray = JSONObject(json).getJSONArray("lessons")
        val lessons = buildList {
            for (index in 0 until lessonsArray.length()) {
                add(lessonsArray.getJSONObject(index).toLesson())
            }
        }

        return lessons
    }

    private fun grouped(lessons: List<CatechismLesson>): List<LessonCategory> {
        val order = categoryOrder + lessons.map { it.category }.filterNot { it in categoryOrder }.distinct()
        return order.mapNotNull { categoryName ->
            lessons.filter { it.category == categoryName }
                .takeIf { it.isNotEmpty() }
                ?.let { LessonCategory(categoryName, it) }
        }
    }
}

internal fun classroomCategoryName(classId: String): String {
    val name = classId.trim()
    return if (name.isEmpty()) "Classroom Lessons" else "$name Lessons"
}

private fun Map<String, Any>.toLesson(id: String, fallback: CatechismLesson?): CatechismLesson? {
    val title = this["title"] as? String ?: fallback?.title ?: return null
    val category = this["category"] as? String ?: fallback?.category ?: "Classroom Lessons"
    val content = this["content"] as? String ?: this["contentHTML"] as? String ?: fallback?.contentHtml.orEmpty()
    val quizData = this["quiz"] as? List<*>
    val quiz = if (quizData == null) fallback?.quiz.orEmpty() else quizData.mapIndexedNotNull { index, value ->
        val item = value as? Map<*, *> ?: return@mapIndexedNotNull null
        val question = item["question"] as? String ?: return@mapIndexedNotNull null
        val options = (item["options"] as? List<*>)?.filterIsInstance<String>() ?: return@mapIndexedNotNull null
        QuizQuestion(
            id = item["id"] as? String ?: "$index-${question.hashCode()}",
            question = question,
            options = options,
            correctAnswerIndex = (item["correct"] as? Number)?.toInt()
                ?: (item["correctAnswerIndex"] as? Number)?.toInt()
                ?: 0,
        )
    }
    return CatechismLesson(
        id = id,
        title = title,
        category = category,
        contentHtml = content,
        videoUrl = this["videoUrl"] as? String ?: fallback?.videoUrl,
        quiz = quiz,
    )
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
