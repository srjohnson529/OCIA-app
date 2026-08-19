package com.illumined.app.ui

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.illumined.app.data.CatechismLesson
import com.illumined.app.data.Assignment
import com.illumined.app.data.DiscussionPrompt
import com.illumined.app.data.DiscussionRepository
import com.illumined.app.data.LessonCatalog
import com.illumined.app.data.LessonCategory
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens
import kotlinx.coroutines.delay

@Composable
fun LessonsExperience(
    userId: String,
    profile: UserProfile?,
    prompts: List<DiscussionPrompt>,
    assignments: List<Assignment>,
    completedLessonIds: Set<String>,
    onCompleteAssignment: (Assignment, () -> Unit, () -> Unit) -> Unit,
    onMarkComplete: (
        lessonId: String,
        badgeIds: List<String>,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) -> Unit,
) {
    val context = LocalContext.current
    val classId = profile?.classIds?.firstOrNull().orEmpty()
    var catalogResult by remember { mutableStateOf(LessonCatalog.load(context.applicationContext)) }
    val categories = catalogResult.getOrNull().orEmpty()
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var quizLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDiscussionId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = categories.firstOrNull { it.name == selectedCategoryName }
    val selectedLesson = selectedCategory?.lessons?.firstOrNull { it.id == selectedLessonId }
    val quizLesson = selectedCategory?.lessons?.firstOrNull { it.id == quizLessonId }
    val reviewLesson = selectedCategory?.lessons?.firstOrNull { it.id == reviewLessonId }
    val selectedDiscussion = prompts.firstOrNull { it.id == selectedDiscussionId }
    var completedPromptIds by remember { mutableStateOf(emptySet<String>()) }
    val discussionRepository = remember { DiscussionRepository() }
    DisposableEffect(classId) {
        val listener = LessonCatalog.listenForClassroom(context.applicationContext, classId) { catalogResult = it }
        onDispose { listener.close() }
    }
    DisposableEffect(classId, userId) {
        val listener = if (classId.isNotBlank()) discussionRepository.listenParticipation(classId, userId, { completedPromptIds = it }, {}) else null
        onDispose { listener?.remove() }
    }

    BackHandler(
        enabled = selectedCategoryName != null || selectedLessonId != null ||
            quizLessonId != null || reviewLessonId != null || selectedDiscussionId != null,
    ) {
        when {
            selectedDiscussionId != null -> selectedDiscussionId = null
            quizLessonId != null -> quizLessonId = null
            reviewLessonId != null -> reviewLessonId = null
            selectedLessonId != null -> selectedLessonId = null
            selectedCategoryName != null -> selectedCategoryName = null
        }
    }

    when {
        selectedDiscussion != null -> DiscussionBoard(
            prompt = selectedDiscussion!!,
            userId = userId,
            profile = profile,
            linkedAssignments = matchingDiscussionAssignments(selectedDiscussion!!.lessonId, assignments),
            onCompleteAssignment = onCompleteAssignment,
            onBack = { selectedDiscussionId = null },
        )
        catalogResult.isFailure -> LessonUnavailable(catalogResult.exceptionOrNull()?.message.orEmpty())
        reviewLesson != null -> QuizReviewScreen(lesson = reviewLesson!!, onBack = { reviewLessonId = null })
        quizLesson != null -> QuizScreen(
            lesson = quizLesson!!,
            category = selectedCategory!!,
            allCategories = categories,
            completedLessonIds = completedLessonIds,
            linkedPrompt = prompts.firstOrNull { it.lessonId == quizLesson!!.id },
            onOpenDiscussion = { selectedDiscussionId = it.id },
            onBack = { quizLessonId = null },
            onCompleted = { lessonId, badges, success, failure ->
                onMarkComplete(lessonId, badges, success, failure)
            },
        )
        selectedLesson != null -> LessonDetail(
            lesson = selectedLesson!!,
            isCompleted = selectedLesson!!.id in completedLessonIds,
            linkedPrompt = prompts.firstOrNull { it.lessonId == selectedLesson!!.id },
            isDiscussionCompleted = prompts.firstOrNull { it.lessonId == selectedLesson!!.id }?.let { it.id in completedPromptIds } == true,
            onBack = { selectedLessonId = null },
            onBeginQuiz = { quizLessonId = selectedLesson.id },
            onReviewQuiz = { reviewLessonId = selectedLesson.id },
            onOpenDiscussion = { selectedDiscussionId = it.id },
        )
        selectedCategory != null -> CategoryLessons(
            category = selectedCategory!!,
            completedLessonIds = completedLessonIds,
            prompts = prompts,
            completedPromptIds = completedPromptIds,
            onBack = { selectedCategoryName = null; selectedLessonId = null; quizLessonId = null; reviewLessonId = null },
            onLesson = { selectedLessonId = it.id },
        )
        else -> CategoryList(
            categories = categories,
            completedLessonIds = completedLessonIds,
            onCategory = { selectedCategoryName = it.name },
        )
    }
}

@Composable
fun AssignedLessonExperience(
    lesson: CatechismLesson,
    categories: List<LessonCategory>,
    userId: String,
    profile: UserProfile?,
    prompts: List<DiscussionPrompt>,
    assignments: List<Assignment>,
    completedLessonIds: Set<String>,
    onCompleteAssignment: (Assignment, () -> Unit, () -> Unit) -> Unit,
    onBack: () -> Unit,
    onMarkComplete: (String, List<String>, () -> Unit, () -> Unit) -> Unit,
) {
    val category = categories.firstOrNull { group -> group.lessons.any { it.id == lesson.id } }
    var showingQuiz by rememberSaveable(lesson.id) { mutableStateOf(false) }
    var reviewingQuiz by rememberSaveable(lesson.id) { mutableStateOf(false) }
    var selectedDiscussionId by rememberSaveable(lesson.id) { mutableStateOf<String?>(null) }
    val selectedDiscussion = prompts.firstOrNull { it.id == selectedDiscussionId }
    var completedPromptIds by remember(lesson.id) { mutableStateOf(emptySet<String>()) }
    val prompt = prompts.firstOrNull { it.lessonId == lesson.id }
    val repository = remember { DiscussionRepository() }
    val classId = profile?.classIds?.firstOrNull().orEmpty()
    DisposableEffect(classId, userId) {
        val listener = if (classId.isNotBlank()) repository.listenParticipation(classId, userId, { completedPromptIds = it }, {}) else null
        onDispose { listener?.remove() }
    }
    BackHandler {
        when {
            selectedDiscussionId != null -> selectedDiscussionId = null
            showingQuiz -> showingQuiz = false
            reviewingQuiz -> reviewingQuiz = false
            else -> onBack()
        }
    }
    if (selectedDiscussion != null) {
        DiscussionBoard(selectedDiscussion, userId, profile, matchingDiscussionAssignments(selectedDiscussion.lessonId, assignments), onCompleteAssignment) { selectedDiscussionId = null }
    } else if (category == null) {
        LessonUnavailable("This linked lesson is not available in the current catalog.")
    } else if (showingQuiz) {
        QuizScreen(
            lesson = lesson,
            category = category,
            allCategories = categories,
            completedLessonIds = completedLessonIds,
            linkedPrompt = prompt,
            onOpenDiscussion = { selectedDiscussionId = it.id },
            onBack = { showingQuiz = false },
            onCompleted = onMarkComplete,
        )
    } else if (reviewingQuiz) {
        QuizReviewScreen(lesson = lesson, onBack = { reviewingQuiz = false })
    } else {
        LessonDetail(
            lesson = lesson,
            isCompleted = lesson.id in completedLessonIds,
            linkedPrompt = prompt,
            isDiscussionCompleted = prompt?.let { it.id in completedPromptIds } == true,
            onBack = onBack,
            onBeginQuiz = { showingQuiz = true },
            onReviewQuiz = { reviewingQuiz = true },
            onOpenDiscussion = { selectedDiscussionId = it.id },
        )
    }
}

@Composable
private fun CategoryList(
    categories: List<LessonCategory>,
    completedLessonIds: Set<String>,
    onCategory: (LessonCategory) -> Unit,
) {
    IlluminedPage {
        item {
            IlluminedPageTitle("Lesson Categories")
            Spacer(Modifier.height(4.dp))
        }
        items(categories, key = { it.name }) { category ->
            val completed = category.lessons.count { it.id in completedLessonIds }
            IosCard(onClick = { onCategory(category) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryCircleBadge(CategoryPresentation.icon(category.name))
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(category.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("${category.lessons.size} lessons", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                        LinearProgressIndicator(
                            progress = { if (category.lessons.isEmpty()) 0f else completed.toFloat() / category.lessons.size },
                            modifier = Modifier.fillMaxWidth(),
                            color = IlluminedThemeTokens.Gold,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$completed/${category.lessons.size}", color = IlluminedThemeTokens.Blue,
                            fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryLessons(
    category: LessonCategory,
    completedLessonIds: Set<String>,
    prompts: List<DiscussionPrompt>,
    completedPromptIds: Set<String>,
    onBack: () -> Unit,
    onLesson: (CatechismLesson) -> Unit,
) {
    IlluminedPage {
        item {
            BackRow(onBack)
            IlluminedPageTitle(category.name)
        }
        items(category.lessons, key = { it.id }) { lesson ->
            val status = lessonProgressStatus(lesson.id, completedLessonIds, prompts, completedPromptIds)
            IosCard(onClick = { onLesson(lesson) }) {
                Row(verticalAlignment = Alignment.Top) {
                    LessonStatusBadge(status)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(lesson.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when(status){LessonProgressStatus.COMPLETED->"${lesson.quiz.size} questions  •  Completed";LessonProgressStatus.IN_PROGRESS->"${lesson.quiz.size} questions  •  In Progress";LessonProgressStatus.NOT_COMPLETED->"${lesson.quiz.size} questions"},
                            fontSize = 12.sp,
                            color = IlluminedThemeTokens.SecondaryText,
                        )
                    }
                    LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp).padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun LessonDetail(
    lesson: CatechismLesson,
    isCompleted: Boolean,
    linkedPrompt: DiscussionPrompt? = null,
    isDiscussionCompleted: Boolean = false,
    onBack: () -> Unit,
    onBeginQuiz: () -> Unit,
    onReviewQuiz: () -> Unit = {},
    onOpenDiscussion: (DiscussionPrompt) -> Unit = {},
) {
    var htmlHeight by remember(lesson.id) { mutableStateOf(500.dp) }
    Column(
        modifier = Modifier.fillMaxSize().background(parchmentBrush())
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BackRow(onBack)
        Text(lesson.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
        IosCard {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(htmlHeight),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        overScrollMode = android.view.View.OVER_SCROLL_NEVER
                        isNestedScrollingEnabled = false
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                view.evaluateJavascript("document.body.scrollHeight") { value ->
                                    value?.trim('"')?.toFloatOrNull()?.let { htmlHeight = (it + 24f).coerceAtLeast(1f).dp }
                                }
                            }
                        }
                    }
                },
                update = { webView ->
                    if (webView.tag != lesson.contentHtml) {
                        webView.tag = lesson.contentHtml
                        webView.loadDataWithBaseURL(null, LessonReaderPolicy.wrapHtml(lesson.contentHtml), "text/html", "UTF-8", null)
                    }
                },
            )
        }
        when {
            isCompleted -> {
                Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    LessonSymbol(LessonSymbolKind.CheckCircle, Color(0xFF2E8B57), Modifier.size(20.dp))
                    Spacer(Modifier.size(7.dp))
                    Text("Lesson completed", color = Color(0xFF2E8B57), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                if (lesson.quiz.isNotEmpty()) {
                    Button(
                        onClick = onReviewQuiz,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        LessonSymbol(LessonSymbolKind.CheckCircle, Color.White, Modifier.size(21.dp), IlluminedThemeTokens.Blue)
                        Spacer(Modifier.size(8.dp))
                        Text("Review Completed Quiz", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                linkedPrompt?.let { LessonDiscussionProgressCard(it, isDiscussionCompleted, onOpenDiscussion) }
            }
            lesson.quiz.isEmpty() -> Text("No Quiz Available", color = IlluminedThemeTokens.SecondaryText,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            else -> Button(
                onClick = onBeginQuiz,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue),
                shape = RoundedCornerShape(14.dp),
            ) {
                LessonSymbol(LessonSymbolKind.PlayCircle, Color.White, Modifier.size(21.dp), IlluminedThemeTokens.Blue)
                Spacer(Modifier.size(8.dp))
                Text("Begin Quiz", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

internal enum class LessonProgressStatus { NOT_COMPLETED, IN_PROGRESS, COMPLETED }

internal fun lessonProgressStatus(lessonId: String, completedLessonIds: Set<String>, prompts: List<DiscussionPrompt>, completedPromptIds: Set<String>): LessonProgressStatus {
    if (lessonId !in completedLessonIds) return LessonProgressStatus.NOT_COMPLETED
    val prompt = prompts.firstOrNull { it.lessonId == lessonId } ?: return LessonProgressStatus.COMPLETED
    return if (prompt.id in completedPromptIds) LessonProgressStatus.COMPLETED else LessonProgressStatus.IN_PROGRESS
}

@Composable
private fun LessonDiscussionProgressCard(prompt: DiscussionPrompt, completed: Boolean, onOpen: (DiscussionPrompt) -> Unit) {
    IosCard {
        val statusColor = if (completed) Color(0xFF2E8B57) else IlluminedThemeTokens.Blue
        Row(verticalAlignment = Alignment.CenterVertically) {
            LessonSymbol(if (completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.Clock, statusColor, Modifier.size(20.dp))
            Spacer(Modifier.size(7.dp))
            Text(if (completed) "Discussion completed" else "Discussion in progress", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
        }
        Spacer(Modifier.height(12.dp))
        Text(if (completed) "You have posted your response. You can return to read or reply to the discussion." else "Your lesson is complete. Finish the linked discussion post when you are ready.", fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText, lineHeight = 21.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onOpen(prompt) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue), shape = RoundedCornerShape(14.dp)) { Text(if (completed) "View Discussion" else "Continue Discussion", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun QuizReviewScreen(lesson: CatechismLesson, onBack: () -> Unit) {
    IlluminedPage {
        item { BackRow(onBack) }
        item {
            IosCard {
                Text("Completed Quiz", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Review each question and the correct answer for this completed lesson.",
                    color = IlluminedThemeTokens.SecondaryText,
                )
            }
        }
        items(lesson.quiz, key = { it.id }) { question ->
            val number = lesson.quiz.indexOf(question) + 1
            IosCard {
                Text(
                    "QUESTION $number",
                    color = IlluminedThemeTokens.Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(question.question, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                question.options.forEachIndexed { optionIndex, option ->
                    val isCorrect = optionIndex == question.correctAnswerIndex
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        color = if (isCorrect) Color(0xFF2E8B57).copy(alpha = 0.10f) else IlluminedThemeTokens.Cream,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCorrect) Color(0xFF2E8B57).copy(alpha = 0.35f) else IlluminedThemeTokens.Gold.copy(alpha = 0.18f),
                        ),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            LessonSymbol(
                                if (isCorrect) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff,
                                if (isCorrect) Color(0xFF2E8B57) else IlluminedThemeTokens.SecondaryText,
                                Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(option, fontSize = 15.sp)
                                if (isCorrect) Text("Correct answer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E8B57))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizScreen(
    lesson: CatechismLesson,
    category: LessonCategory,
    allCategories: List<LessonCategory>,
    completedLessonIds: Set<String>,
    linkedPrompt: DiscussionPrompt? = null,
    onOpenDiscussion: (DiscussionPrompt) -> Unit = {},
    onBack: () -> Unit,
    onCompleted: (String, List<String>, () -> Unit, () -> Unit) -> Unit,
) {
    val answers = remember(lesson.id) { mutableStateMapOf<String, Int>() }
    var incorrectIds by remember { mutableStateOf(emptySet<String>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var pendingHandoff by remember(lesson.id) { mutableStateOf(false) }

    LaunchedEffect(pendingHandoff) {
        if (pendingHandoff) {
            delay(700)
            pendingHandoff = false
            if (linkedPrompt != null) onOpenDiscussion(linkedPrompt) else onBack()
        }
    }

    IlluminedPage {
        item { BackRow(onBack) }
        item {
            IosCard {
                Text("Quiz", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Score 100% to complete this lesson.", color = IlluminedThemeTokens.SecondaryText)
            }
        }
        items(lesson.quiz, key = { it.id }) { question ->
            val number = lesson.quiz.indexOf(question) + 1
            IosCard {
                Text("QUESTION $number", color = IlluminedThemeTokens.Gold, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
                Spacer(Modifier.height(12.dp))
                Text(question.question, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                question.options.forEachIndexed { index, option ->
                    val selected = answers[question.id] == index
                    Surface(
                        onClick = {
                            answers[question.id] = index
                            incorrectIds = emptySet()
                            message = null
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).semantics {
                            role = Role.RadioButton
                            this.selected = selected
                        },
                        color = if (selected) IlluminedThemeTokens.Blue.copy(alpha = 0.10f) else IlluminedThemeTokens.Cream,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) IlluminedThemeTokens.Blue.copy(alpha = 0.35f) else IlluminedThemeTokens.Gold.copy(alpha = 0.18f),
                        ),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            LessonSymbol(
                                if (selected) LessonSymbolKind.RadioOn else LessonSymbolKind.RadioOff,
                                if (selected) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Gold,
                                Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(12.dp))
                            Text(option, modifier = Modifier.weight(1f), fontSize = 15.sp)
                        }
                    }
                }
            }
        }
        item {
            IosCard {
                Button(
                    onClick = {
                        when (val result = QuizEvaluation.evaluate(lesson.quiz, answers)) {
                            QuizEvaluationResult.Incomplete -> message = "Please answer every question before submitting."
                            is QuizEvaluationResult.Incorrect -> {
                                incorrectIds = result.incorrectQuestionIds
                                message = "You scored ${result.score}/${result.total}."
                            }
                            QuizEvaluationResult.Perfect -> {
                            saving = true
                            incorrectIds = emptySet()
                            message = "Correct! Saving lesson completion..."
                            val completed = completedLessonIds + lesson.id
                            val badges = buildList {
                                if (category.lessons.all { it.id in completed }) {
                                    categoryBadge(category.name)?.let(::add)
                                }
                                if (allCategories.flatMap { it.lessons }.all { it.id in completed }) add("illumined-graduate")
                            }
                            onCompleted(lesson.id, badges, {
                                saving = false
                                message = if (linkedPrompt == null) "Correct! You scored 100% and completed this lesson." else "Correct! You scored 100%. Opening the discussion assignment..."
                                pendingHandoff = true
                            }, {
                                saving = false
                                message = "Your score was correct, but progress could not be saved. Please try again."
                            })
                            }
                        }
                    },
                    enabled = answers.size == lesson.quiz.size && !saving,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Submit Quiz", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                message?.let {
                    Spacer(Modifier.height(12.dp))
                    if (incorrectIds.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LessonSymbol(LessonSymbolKind.WarningCircle, Color(0xFFB3261E), Modifier.size(19.dp))
                            Spacer(Modifier.size(7.dp))
                            Text(it, color = Color(0xFFB3261E), fontWeight = FontWeight.SemiBold)
                        }
                    } else Text(it, color = if (it.startsWith("Correct")) Color(0xFF2E8B57) else Color(0xFFB3261E))
                }
                if (incorrectIds.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("These questions were marked incorrectly:")
                    lesson.quiz.forEachIndexed { index, question ->
                        if (question.id in incorrectIds) Text("Question ${index + 1}: ${question.question}",
                            color = IlluminedThemeTokens.SecondaryText, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun IlluminedPage(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(parchmentBrush())) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun IosCard(
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(0.10f)),
        color = IlluminedThemeTokens.Card.copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(alpha = 0.22f)),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun LessonStatusBadge(status: LessonProgressStatus) {
    val color = when (status) {
        LessonProgressStatus.COMPLETED -> Color(0xFF2E8B57)
        LessonProgressStatus.IN_PROGRESS -> IlluminedThemeTokens.Blue
        LessonProgressStatus.NOT_COMPLETED -> IlluminedThemeTokens.Gold
    }
    val symbol = when (status) {
        LessonProgressStatus.COMPLETED -> LessonSymbolKind.CheckCircle
        LessonProgressStatus.IN_PROGRESS -> LessonSymbolKind.Clock
        LessonProgressStatus.NOT_COMPLETED -> LessonSymbolKind.BookClosed
    }
    Box(Modifier.size(34.dp).background(color.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
        LessonSymbol(symbol, color, Modifier.size(20.dp))
    }
}

@Composable
private fun CategoryCircleBadge(icon: LessonCategoryIcon) {
    val color = IlluminedThemeTokens.Gold
    Box(Modifier.size(44.dp).background(color.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(25.dp)) {
            val stroke = Stroke(width = size.minDimension * .085f, cap = StrokeCap.Round)
            when (icon) {
                LessonCategoryIcon.CROSS -> {
                    drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .10f), end = androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .90f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width * .20f, size.height * .38f), end = androidx.compose.ui.geometry.Offset(size.width * .80f, size.height * .38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                LessonCategoryIcon.SPARKLES -> {
                    fun sparkle(cx: Float, cy: Float, radius: Float) { val path = Path().apply { moveTo(cx, cy-radius); lineTo(cx+radius*.28f, cy-radius*.28f); lineTo(cx+radius, cy); lineTo(cx+radius*.28f, cy+radius*.28f); lineTo(cx, cy+radius); lineTo(cx-radius*.28f, cy+radius*.28f); lineTo(cx-radius, cy); lineTo(cx-radius*.28f, cy-radius*.28f); close() }; drawPath(path, color) }
                    sparkle(size.width*.43f, size.height*.56f, size.minDimension*.30f); sparkle(size.width*.76f, size.height*.24f, size.minDimension*.13f); sparkle(size.width*.78f, size.height*.78f, size.minDimension*.09f)
                }
                LessonCategoryIcon.HEART -> {
                    val path = Path().apply { moveTo(size.width*.5f,size.height*.88f); cubicTo(size.width*.08f,size.height*.62f,size.width*.05f,size.height*.28f,size.width*.28f,size.height*.20f); cubicTo(size.width*.42f,size.height*.15f,size.width*.5f,size.height*.27f,size.width*.5f,size.height*.34f); cubicTo(size.width*.5f,size.height*.27f,size.width*.58f,size.height*.15f,size.width*.72f,size.height*.20f); cubicTo(size.width*.95f,size.height*.28f,size.width*.92f,size.height*.62f,size.width*.5f,size.height*.88f); close() }
                    drawPath(path, color)
                }
                LessonCategoryIcon.PRAYING_HANDS -> {
                    val left = Path().apply { moveTo(size.width*.45f,size.height*.84f); cubicTo(size.width*.31f,size.height*.71f,size.width*.19f,size.height*.54f,size.width*.17f,size.height*.32f); cubicTo(size.width*.16f,size.height*.20f,size.width*.27f,size.height*.18f,size.width*.32f,size.height*.29f); lineTo(size.width*.51f,size.height*.69f) }
                    val right = Path().apply { moveTo(size.width*.55f,size.height*.84f); cubicTo(size.width*.69f,size.height*.71f,size.width*.81f,size.height*.54f,size.width*.83f,size.height*.32f); cubicTo(size.width*.84f,size.height*.20f,size.width*.73f,size.height*.18f,size.width*.68f,size.height*.29f); lineTo(size.width*.49f,size.height*.69f) }
                    drawPath(left,color,style=stroke); drawPath(right,color,style=stroke); drawLine(color,androidx.compose.ui.geometry.Offset(size.width*.38f,size.height*.89f),androidx.compose.ui.geometry.Offset(size.width*.62f,size.height*.89f),stroke.width,StrokeCap.Round)
                }
                LessonCategoryIcon.BOOK -> {
                    val left = Path().apply { moveTo(size.width*.48f,size.height*.22f); cubicTo(size.width*.34f,size.height*.14f,size.width*.18f,size.height*.15f,size.width*.13f,size.height*.22f); lineTo(size.width*.13f,size.height*.78f); cubicTo(size.width*.25f,size.height*.72f,size.width*.37f,size.height*.73f,size.width*.48f,size.height*.82f); close() }
                    val right = Path().apply { moveTo(size.width*.52f,size.height*.22f); cubicTo(size.width*.66f,size.height*.14f,size.width*.82f,size.height*.15f,size.width*.87f,size.height*.22f); lineTo(size.width*.87f,size.height*.78f); cubicTo(size.width*.75f,size.height*.72f,size.width*.63f,size.height*.73f,size.width*.52f,size.height*.82f); close() }
                    drawPath(left,color,style=stroke);drawPath(right,color,style=stroke)
                }
            }
        }
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    TextButton(onClick = onBack) { Text("‹ Back", color = IlluminedThemeTokens.Blue, fontSize = 16.sp) }
}

@Composable
private fun IlluminedPageTitle(title: String) {
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
}

@Composable
private fun LessonUnavailable(message: String) {
    Box(Modifier.fillMaxSize().background(parchmentBrush()), contentAlignment = Alignment.Center) {
        Text("Lessons Unavailable\n$message", color = Color(0xFFB3261E), modifier = Modifier.padding(24.dp))
    }
}

private fun categoryBadge(name: String): String? = when (name) {
    "Profession of Faith" -> "foundations-complete"
    "Celebration of the Christian Mysteries" -> "celebration-complete"
    "Life in Christ" -> "life-in-christ-complete"
    "Christian Prayer" -> "prayer-complete"
    else -> null
}

private fun parchmentBrush(): Brush = Brush.radialGradient(
    colors = listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream),
    radius = 1500f,
)
