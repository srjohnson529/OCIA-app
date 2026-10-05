package com.illumined.app.ui
import androidx.compose.foundation.border

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.illumined.app.data.CatechismLesson
import com.illumined.app.data.Assignment
import com.illumined.app.data.DiscussionPrompt
import com.illumined.app.data.DiscussionRepository
import com.illumined.app.data.LessonCatalog
import com.illumined.app.data.LessonCategory
import com.illumined.app.data.QuizPolicy
import com.illumined.app.data.UserProfile
import com.illumined.app.R
import com.illumined.app.ui.theme.IlluminedThemeTokens
import kotlinx.coroutines.delay
import java.util.Locale

private fun lessonT(english: String, spanish: String) =
    if (Locale.getDefault().language == "es") spanish else english

private fun lessonCountText(count: Int): String = when {
    Locale.getDefault().language == "es" && count == 1 -> "$count lección"
    Locale.getDefault().language == "es" -> "$count lecciones"
    count == 1 -> "$count lesson"
    else -> "$count lessons"
}

private fun questionCountText(count: Int): String = when {
    Locale.getDefault().language == "es" && count == 1 -> "$count pregunta"
    Locale.getDefault().language == "es" -> "$count preguntas"
    count == 1 -> "$count question"
    else -> "$count questions"
}

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
    val classId = profile?.selectedClassId.orEmpty()
    var catalogResult by remember { mutableStateOf(LessonCatalog.load(context.applicationContext)) }
    val categories = catalogResult.getOrNull().orEmpty()
    var selectedCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var quizLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewLessonId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDiscussionId by rememberSaveable { mutableStateOf<String?>(null) }
    val tour = LocalInstructorWalkthrough.current
    LaunchedEffect(tour?.screen, tour?.active, categories) {
        if(tour?.active == true && tour.page == "lessons") {
            quizLessonId = null; reviewLessonId = null; selectedDiscussionId = null
            when(tour.screen) {
                "categories" -> { selectedCategoryName = null; selectedLessonId = null }
                "category", "detail" -> {
                    val category = categories.firstOrNull { it.name == selectedCategoryName && it.lessons.isNotEmpty() } ?: categories.firstOrNull { it.lessons.isNotEmpty() }
                    selectedCategoryName = category?.name
                    selectedLessonId = if(tour.screen == "detail") category?.lessons?.firstOrNull { it.id == selectedLessonId }?.id ?: category?.lessons?.firstOrNull()?.id else null
                    category?.lessons?.firstOrNull { it.id == selectedLessonId }?.let { lesson ->
                        val sections = walkthroughLessonParts(lesson.localizedContentHtml).filter { it.second.isNotBlank() }.mapIndexed { index, part ->
                            walkthroughLessonStep(index, part.second)
                        }
                        tour.configureLesson(sections, lessonVideoDetails(lesson.videoUrl) != null)
                    }
                }
            }
        }
    }
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
            linkedAssignments = matchingDiscussionAssignments(selectedDiscussion!!, assignments),
            onCompleteAssignment = onCompleteAssignment,
            onBack = { selectedDiscussionId = null },
        )
        catalogResult.isFailure -> LessonUnavailable(catalogResult.exceptionOrNull()?.message.orEmpty())
        reviewLesson != null -> QuizReviewScreen(lesson = reviewLesson!!, onBack = { reviewLessonId = null })
        quizLesson != null -> QuizScreen(
            userId = userId,
            classId = profile?.selectedClassId.orEmpty(),
            lesson = quizLesson!!,
            category = selectedCategory!!,
            allCategories = categories,
            completedLessonIds = completedLessonIds,
            linkedPrompt = prompts.firstOrNull { it.assignmentId.isBlank() && it.lessonId == quizLesson!!.id },
            onOpenDiscussion = { selectedDiscussionId = it.id },
            onBack = { quizLessonId = null },
            onCompleted = { lessonId, badges, success, failure ->
                onMarkComplete(lessonId, badges, success, failure)
            },
        )
        selectedLesson != null -> LessonDetail(
            userId = userId,
            classId = profile?.selectedClassId.orEmpty(),
            lesson = selectedLesson!!,
            isCompleted = selectedLesson!!.id in completedLessonIds,
            linkedPrompt = prompts.firstOrNull { it.assignmentId.isBlank() && it.lessonId == selectedLesson!!.id },
            isDiscussionCompleted = prompts.firstOrNull { it.assignmentId.isBlank() && it.lessonId == selectedLesson!!.id }?.let { it.id in completedPromptIds } == true,
            onBack = { selectedLessonId = null },
            onBeginQuiz = { quizLessonId = selectedLesson.id },
            onCompleteWithoutQuiz = {
                onMarkComplete(selectedLesson.id, lessonCompletionBadges(selectedLesson, selectedCategory!!, categories, completedLessonIds), {}, {})
            },
            onReviewQuiz = { reviewLessonId = selectedLesson.id },
            onOpenDiscussion = { selectedDiscussionId = it.id },
        )
        selectedCategory != null -> CategoryLessons(
            category = selectedCategory!!,
            completedLessonIds = completedLessonIds,
            prompts = prompts,
            completedPromptIds = completedPromptIds,
            onBack = { selectedCategoryName = null; selectedLessonId = null; quizLessonId = null; reviewLessonId = null },
            onLesson = { selectedLessonId = it.id; if(tour?.active == true) tour.go("lesson-title") },
        )
        else -> CategoryList(
            categories = categories,
            completedLessonIds = completedLessonIds,
            tracker = {
                val remaining = categories.flatMap { it.lessons }.filter { it.id !in completedLessonIds }
                val resumeKey = org.json.JSONArray(listOf(userId, classId)).toString()
                val resumeId = context.getSharedPreferences("illumined.lessonResume", android.content.Context.MODE_PRIVATE).getString(resumeKey, null)
                val next = remaining.firstOrNull { it.id == resumeId } ?: remaining.firstOrNull { hasLessonDraft(context, userId, classId, it) } ?: remaining.firstOrNull()
                LessonTrackerCard(categories.flatMap { it.lessons }.size, categories.flatMap { it.lessons }.count { it.id in completedLessonIds }, next?.localizedTitle) {
                    if(tour?.active != true && next != null) {
                        selectedCategoryName = categories.first { group -> group.lessons.any { it.id == next.id } }.name
                        selectedLessonId = next.id
                    }
                }
            },
            onCategory = { selectedCategoryName = it.name; if(tour?.active == true) tour.go("category") },
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
    val prompt = prompts.firstOrNull { it.assignmentId.isBlank() && it.lessonId == lesson.id }
    val repository = remember { DiscussionRepository() }
    val classId = profile?.selectedClassId.orEmpty()
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
        DiscussionBoard(selectedDiscussion, userId, profile, matchingDiscussionAssignments(selectedDiscussion, assignments), onCompleteAssignment) { selectedDiscussionId = null }
    } else if (category == null) {
        LessonUnavailable("This linked lesson is not available in the current catalog.")
    } else if (showingQuiz) {
        QuizScreen(
            lesson = lesson,
            userId = userId,
            classId = profile?.selectedClassId.orEmpty(),
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
            userId = userId,
            classId = profile?.selectedClassId.orEmpty(),
            lesson = lesson,
            isCompleted = lesson.id in completedLessonIds,
            linkedPrompt = prompt,
            isDiscussionCompleted = prompt?.let { it.id in completedPromptIds } == true,
            onBack = onBack,
            onBeginQuiz = { showingQuiz = true },
            onCompleteWithoutQuiz = {
                onMarkComplete(lesson.id, lessonCompletionBadges(lesson, category, categories, completedLessonIds), {}, {})
            },
            onReviewQuiz = { reviewingQuiz = true },
            onOpenDiscussion = { selectedDiscussionId = it.id },
        )
    }
}

@Composable
private fun CategoryList(
    tracker: @Composable () -> Unit,
    categories: List<LessonCategory>,
    completedLessonIds: Set<String>,
    onCategory: (LessonCategory) -> Unit,
) {
    IlluminedPage {
        item { tracker() }
        item {
            Box(Modifier.walkthroughAnchor("content-lessons")) { IlluminedPageTitle(stringResource(R.string.lesson_categories)) }
            Spacer(Modifier.height(4.dp))
        }
        items(categories, key = { it.name }) { category ->
            val completed = category.lessons.count { it.id in completedLessonIds }
            val categoryDisplayName = localizedLessonCategoryName(category.name)
            IosCard(onClick = { onCategory(category) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryCircleBadge(CategoryPresentation.icon(category.name))
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(categoryDisplayName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(lessonCountText(category.lessons.size), fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
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
private fun localizedLessonCategoryName(categoryName: String): String = when (categoryName) {
    "Profession of Faith" -> stringResource(R.string.lesson_category_profession_of_faith)
    "Celebration of the Christian Mysteries" -> stringResource(R.string.lesson_category_celebration_of_mysteries)
    "Life in Christ" -> stringResource(R.string.lesson_category_life_in_christ)
    "Christian Prayer" -> stringResource(R.string.lesson_category_christian_prayer)
    else -> categoryName
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
            Box(Modifier.walkthroughAnchor("category")) { IlluminedPageTitle(localizedLessonCategoryName(category.name)) }
        }
        items(category.lessons, key = { it.id }) { lesson ->
            val status = lessonProgressStatus(lesson.id, completedLessonIds, prompts, completedPromptIds)
            IosCard(onClick = { onLesson(lesson) }) {
                Row(verticalAlignment = Alignment.Top) {
                    LessonStatusBadge(status)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(lesson.localizedTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when (status) {
                                LessonProgressStatus.COMPLETED -> "${questionCountText(lesson.localizedQuiz.size)}  •  ${lessonT("Completed", "Completada")}"
                                LessonProgressStatus.IN_PROGRESS -> "${questionCountText(lesson.localizedQuiz.size)}  •  ${lessonT("In Progress", "En curso")}"
                                LessonProgressStatus.NOT_COMPLETED -> questionCountText(lesson.localizedQuiz.size)
                            },
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
    userId: String,
    classId: String,
    lesson: CatechismLesson,
    isCompleted: Boolean,
    linkedPrompt: DiscussionPrompt? = null,
    isDiscussionCompleted: Boolean = false,
    onBack: () -> Unit,
    onBeginQuiz: () -> Unit,
    onCompleteWithoutQuiz: () -> Unit,
    onReviewQuiz: () -> Unit = {},
    onOpenDiscussion: (DiscussionPrompt) -> Unit = {},
) {
    val context = LocalContext.current
    val touring = LocalInstructorWalkthrough.current?.active == true
    LaunchedEffect(userId, classId, lesson.id, isCompleted, touring) {
        if(!touring && !isCompleted && userId.isNotBlank() && classId.isNotBlank()) {
            context.getSharedPreferences("illumined.lessonResume", android.content.Context.MODE_PRIVATE).edit()
                .putString(org.json.JSONArray(listOf(userId, classId)).toString(), lesson.id).apply()
        }
    }
    val hasSavedQuiz = !isCompleted && hasLessonDraft(context, userId, classId, lesson)
    val video = remember(lesson.videoUrl) { lessonVideoDetails(lesson.videoUrl) }
    var htmlHeight by remember(lesson.id) { mutableStateOf(500.dp) }
    Column(
        modifier = Modifier.fillMaxSize().background(parchmentBrush())
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BackRow(onBack)
        Text(lesson.localizedTitle, modifier = Modifier.walkthroughAnchor("lesson-title"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
        if(LocalInstructorWalkthrough.current?.active == true) {
            WalkthroughLessonContent(lesson.localizedContentHtml)
        } else IosCard {
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
                    if (webView.tag != lesson.localizedContentHtml) {
                        webView.tag = lesson.localizedContentHtml
                        webView.loadDataWithBaseURL(null, LessonReaderPolicy.wrapHtml(lesson.localizedContentHtml), "text/html", "UTF-8", null)
                    }
                },
            )
        }
        video?.let { details ->
            Box(Modifier.walkthroughAnchor("lesson-video")) { IosCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    details.videoId?.let { videoId -> LessonYouTubePlayer(videoId) }
                    TextButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(details.externalUrl))) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (details.videoId == null) lessonT("Open video", "Abrir video") else lessonT("Open on YouTube", "Abrir en YouTube"))
                    }
                }
            }
        } }
        Spacer(Modifier.height(1.dp).walkthroughAnchor("lesson-actions"))
        when {
            isCompleted -> {
                Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    LessonSymbol(LessonSymbolKind.CheckCircle, Color(0xFF2E8B57), Modifier.size(20.dp))
                    Spacer(Modifier.size(7.dp))
                    Text(lessonT("Lesson completed", "Lección completada"), color = Color(0xFF2E8B57), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                if (lesson.localizedQuiz.isNotEmpty() && lesson.quizPolicy != QuizPolicy.HIDDEN) {
                    Button(
                        onClick = onReviewQuiz,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        LessonSymbol(LessonSymbolKind.CheckCircle, Color.White, Modifier.size(21.dp), IlluminedThemeTokens.Blue)
                        Spacer(Modifier.size(8.dp))
                        Text(lessonT("Review Completed Quiz", "Revisar cuestionario completado"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                linkedPrompt?.let { LessonDiscussionProgressCard(it, isDiscussionCompleted, onOpenDiscussion) }
            }
            lesson.localizedQuiz.isEmpty() && lesson.quizPolicy == QuizPolicy.REQUIRED -> Text(lessonT("No Quiz Available", "No hay cuestionario disponible"), color = IlluminedThemeTokens.SecondaryText,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            else -> {
                if (lesson.quizPolicy != QuizPolicy.HIDDEN && lesson.localizedQuiz.isNotEmpty()) {
                    Button(
                        onClick = onBeginQuiz,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        LessonSymbol(LessonSymbolKind.PlayCircle, Color.White, Modifier.size(21.dp), IlluminedThemeTokens.Blue)
                        Spacer(Modifier.size(8.dp))
                        Text(if (hasSavedQuiz) lessonT("Continue Quiz", "Continuar cuestionario") else lessonT("Begin Quiz", "Comenzar cuestionario"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (lesson.quizPolicy != QuizPolicy.REQUIRED) {
                    Button(
                        onClick = onCompleteWithoutQuiz,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(lessonT("Mark Lesson Completed", "Marcar lección como completada"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LessonYouTubePlayer(videoId: String) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var loading by remember(videoId) { mutableStateOf(true) }
    var failed by remember(videoId) { mutableStateOf(false) }
    var fullscreenView by remember(videoId) { mutableStateOf<View?>(null) }
    var exitFullscreen by remember(videoId) { mutableStateOf<(() -> Unit)?>(null) }

    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF222222))) {
        if (!failed) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { playerContext ->
                    YouTubePlayerView(playerContext).apply {
                        lifecycleOwner.lifecycle.addObserver(this)
                        enableAutomaticInitialization = false
                        addFullscreenListener(object : FullscreenListener {
                            override fun onEnterFullscreen(fullscreenPlayerView: View, exitFullscreenAction: () -> Unit) {
                                fullscreenView = fullscreenPlayerView
                                exitFullscreen = exitFullscreenAction
                            }

                            override fun onExitFullscreen() {
                                fullscreenView = null
                                exitFullscreen = null
                            }
                        })
                        val listener = object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                loading = false
                                youTubePlayer.cueVideo(videoId, 0f)
                            }

                            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                loading = false
                                failed = true
                            }
                        }
                        val options = IFramePlayerOptions.Builder(playerContext)
                            .controls(1)
                            .fullscreen(1)
                            .build()
                        initialize(listener, true, options)
                    }
                },
                onRelease = { playerView ->
                    lifecycleOwner.lifecycle.removeObserver(playerView)
                    playerView.release()
                },
            )
        }
        if (loading && !failed) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
        if (failed) {
            Column(
                Modifier.align(Alignment.Center).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(lessonT("Video preview unavailable", "Vista previa del video no disponible"), color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(lessonT("Use Open on YouTube below.", "Usa Abrir en YouTube abajo."), color = Color.White.copy(alpha = .8f))
            }
        }
    }

    fullscreenView?.let { playerView ->
        Dialog(
            onDismissRequest = { exitFullscreen?.invoke() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = { dialogContext ->
                    FrameLayout(dialogContext).apply {
                        (playerView.parent as? ViewGroup)?.removeView(playerView)
                        addView(
                            playerView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                },
                onRelease = { container ->
                    if (playerView.parent === container) container.removeView(playerView)
                },
            )
        }
    }
}

private data class LessonVideoDetails(val videoId: String?, val externalUrl: String)

private fun lessonVideoDetails(rawValue: String?): LessonVideoDetails? {
    val value = rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val iframeSource = Regex("""<iframe\b[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        .find(value)?.groupValues?.getOrNull(1)
    val candidate = (iframeSource ?: value).replace("&amp;", "&")
    val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() != "https" || uri.host.isNullOrBlank()) return null

    val host = uri.host.orEmpty().lowercase().removePrefix("www.")
    val parts = uri.pathSegments.filter { it.isNotBlank() }
    val videoId = when {
        host == "youtu.be" -> parts.firstOrNull()
        host in setOf("youtube.com", "m.youtube.com", "youtube-nocookie.com") && uri.path == "/watch" -> uri.getQueryParameter("v")
        host in setOf("youtube.com", "m.youtube.com", "youtube-nocookie.com") && parts.firstOrNull() in setOf("embed", "shorts", "live") -> parts.getOrNull(1)
        else -> null
    }

    return if (videoId?.matches(Regex("^[A-Za-z0-9_-]{11}$")) == true) {
        LessonVideoDetails(
            videoId = videoId,
            externalUrl = "https://www.youtube.com/watch?v=$videoId",
        )
    } else {
        LessonVideoDetails(videoId = null, externalUrl = uri.toString())
    }
}

internal enum class LessonProgressStatus { NOT_COMPLETED, IN_PROGRESS, COMPLETED }

internal fun lessonProgressStatus(lessonId: String, completedLessonIds: Set<String>, prompts: List<DiscussionPrompt>, completedPromptIds: Set<String>): LessonProgressStatus {
    if (lessonId !in completedLessonIds) return LessonProgressStatus.NOT_COMPLETED
    val prompt = prompts.firstOrNull { it.assignmentId.isBlank() && it.lessonId == lessonId } ?: return LessonProgressStatus.COMPLETED
    return if (prompt.id in completedPromptIds) LessonProgressStatus.COMPLETED else LessonProgressStatus.IN_PROGRESS
}

@Composable
private fun LessonDiscussionProgressCard(prompt: DiscussionPrompt, completed: Boolean, onOpen: (DiscussionPrompt) -> Unit) {
    IosCard {
        val statusColor = if (completed) Color(0xFF2E8B57) else IlluminedThemeTokens.Blue
        Row(verticalAlignment = Alignment.CenterVertically) {
            LessonSymbol(if (completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.Clock, statusColor, Modifier.size(20.dp))
            Spacer(Modifier.size(7.dp))
            Text(if (completed) lessonT("Discussion completed", "Discusión completada") else lessonT("Discussion in progress", "Discusión en curso"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
        }
        Spacer(Modifier.height(12.dp))
        Text(if (completed) lessonT("You have posted your response. You can return to read or reply to the discussion.", "Has publicado tu respuesta. Puedes volver para leer o responder en la discusión.") else lessonT("Your lesson is complete. Finish the linked discussion post when you are ready.", "Has completado la lección. Cuando estés listo, termina la publicación de discusión vinculada."), fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText, lineHeight = 21.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onOpen(prompt) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue), shape = RoundedCornerShape(14.dp)) { Text(if (completed) lessonT("View Discussion", "Ver discusión") else lessonT("Continue Discussion", "Continuar discusión"), fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun QuizReviewScreen(lesson: CatechismLesson, onBack: () -> Unit) {
    IlluminedPage {
        item { BackRow(onBack) }
        item {
            IosCard {
                Text(lessonT("Completed Quiz", "Cuestionario completado"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    lessonT("Review each question and the correct answer for this completed lesson.", "Revisa cada pregunta y la respuesta correcta de esta lección completada."),
                    color = IlluminedThemeTokens.SecondaryText,
                )
            }
        }
        items(lesson.localizedQuiz, key = { it.id }) { question ->
            val number = lesson.localizedQuiz.indexOf(question) + 1
            IosCard {
                Text(
                    lessonT("QUESTION $number", "PREGUNTA $number"),
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
                                if (isCorrect) Text(lessonT("Correct answer", "Respuesta correcta"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E8B57))
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
    userId: String,
    classId: String,
    lesson: CatechismLesson,
    category: LessonCategory,
    allCategories: List<LessonCategory>,
    completedLessonIds: Set<String>,
    linkedPrompt: DiscussionPrompt? = null,
    onOpenDiscussion: (DiscussionPrompt) -> Unit = {},
    onBack: () -> Unit,
    onCompleted: (String, List<String>, () -> Unit, () -> Unit) -> Unit,
) {
    val draftPreferences = LocalContext.current.getSharedPreferences("illumined.quizDrafts.v1", android.content.Context.MODE_PRIVATE)
    val draftKey = if (userId.isNotBlank() && classId.isNotBlank()) org.json.JSONArray(listOf(userId, classId, lesson.id)).toString() else null
    val draftSignature = org.json.JSONArray().apply { lesson.localizedQuiz.forEach { q -> put(org.json.JSONArray().put(q.question).put(org.json.JSONArray(q.options)).put(q.correctAnswerIndex)) } }.toString()
    val answers = remember(draftKey, draftSignature) {
        mutableStateMapOf<String, Int>().apply {
            if (draftKey != null) {
                try {
                    val saved = org.json.JSONObject(draftPreferences.getString(draftKey, null) ?: "{}")
                    if (lesson.id !in completedLessonIds && saved.optString("signature") == draftSignature) {
                        val values = saved.optJSONArray("answers")
                        lesson.localizedQuiz.forEachIndexed { i, q ->
                            val answer = values?.optInt(i, -1) ?: -1
                            if (answer in q.options.indices) put(q.id, answer)
                        }
                    } else draftPreferences.edit().remove(draftKey).apply()
                } catch (_: Exception) { draftPreferences.edit().remove(draftKey).apply() }
            }
        }
    }
    var draftNotice by remember(draftKey, draftSignature) { mutableStateOf(if (answers.isNotEmpty()) lessonT("Saved answers restored on this device.", "Respuestas guardadas restauradas en este dispositivo.") else "") }
    fun saveDraft() {
        if (draftKey == null) return
        try {
            val values = org.json.JSONArray(lesson.localizedQuiz.map { answers[it.id] ?: -1 })
            draftPreferences.edit().putString(draftKey, org.json.JSONObject().put("signature", draftSignature).put("answers", values).toString()).apply()
            draftNotice = lessonT("Answers saved on this device.", "Respuestas guardadas en este dispositivo.")
        } catch (_: Exception) { draftNotice = lessonT("Answers could not be saved. Keep this quiz open.", "No se pudieron guardar las respuestas. Mantén abierto este cuestionario.") }
    }
    val quizScrollState = rememberLazyListState()
    val quizScrollScope = rememberCoroutineScope()
    var incorrectIds by remember { mutableStateOf(emptySet<String>()) }
    var reviewingAnswers by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var pendingHandoff by remember(lesson.id) { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            // Wait for the feedback to be laid out before revealing the results card.
            androidx.compose.runtime.withFrameNanos { }
            val firstIncorrect = lesson.localizedQuiz.indexOfFirst { it.id in incorrectIds }
            quizScrollState.animateScrollToItem(if (firstIncorrect >= 0) firstIncorrect + 2 else lesson.localizedQuiz.size + 2)
        }
    }

    LaunchedEffect(pendingHandoff) {
        if (pendingHandoff) {
            delay(700)
            pendingHandoff = false
            if (linkedPrompt != null) onOpenDiscussion(linkedPrompt) else onBack()
        }
    }

    IlluminedPage(state = quizScrollState) {
        item { BackRow(onBack) }
        item {
            IosCard {
                Text(lessonT("Quiz", "Cuestionario"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                if (draftNotice.isNotEmpty()) Text(draftNotice, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                Spacer(Modifier.height(8.dp))
                Text(lessonT("Score 100% to complete this lesson.", "Obtén el 100 % para completar esta lección."), color = IlluminedThemeTokens.SecondaryText)
            }
        }
        items(lesson.localizedQuiz, key = { it.id }) { question ->
            val number = lesson.localizedQuiz.indexOf(question) + 1
            IosCard {
                Text(lessonT("QUESTION $number", "PREGUNTA $number"), color = IlluminedThemeTokens.Gold, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
                Spacer(Modifier.height(12.dp))
                Text(question.question, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (question.id in incorrectIds) Text(lessonT("Incorrect. Try again.", "Incorrecto. Inténtalo de nuevo."), color = Color(0xFFB3261E), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                question.options.forEachIndexed { index, option ->
                    val selected = answers[question.id] == index
                    Surface(
                        onClick = {
                            answers[question.id] = index
                            saveDraft()
                            message = null
                            if (reviewingAnswers) {
                                incorrectIds = lesson.localizedQuiz.filter { answers[it.id] != it.correctAnswerIndex }.map { it.id }.toSet()
                                if (index == question.correctAnswerIndex) {
                                    val pending = lesson.localizedQuiz.indices.filter { lesson.localizedQuiz[it].id in incorrectIds }
                                    val next = pending.firstOrNull { it > number - 1 } ?: pending.firstOrNull() ?: lesson.localizedQuiz.size
                                    quizScrollScope.launch { quizScrollState.animateScrollToItem(next + 2) }
                                }
                            } else if (number < lesson.localizedQuiz.size) {
                                // The back row and quiz introduction occupy the first two items.
                                quizScrollScope.launch { quizScrollState.animateScrollToItem(number + 2) }
                            }
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
                        when (val result = QuizEvaluation.evaluate(lesson.localizedQuiz, answers)) {
                            QuizEvaluationResult.Incomplete -> message = lessonT("Please answer every question before submitting.", "Responde todas las preguntas antes de enviar el cuestionario.")
                            is QuizEvaluationResult.Incorrect -> {
                                reviewingAnswers = true
                                incorrectIds = result.incorrectQuestionIds
                                message = lessonT("You scored ${result.score}/${result.total}.", "Obtuviste ${result.score}/${result.total}.")
                            }
                            QuizEvaluationResult.Perfect -> {
                            saving = true
                            incorrectIds = emptySet()
                            message = lessonT("Correct! Saving lesson completion...", "¡Correcto! Guardando la finalización de la lección...")
                            val completed = completedLessonIds + lesson.id
                            val badges = buildList {
                                if (category.lessons.all { it.id in completed }) {
                                    categoryBadge(category.name)?.let(::add)
                                }
                                if (allCategories.flatMap { it.lessons }.all { it.id in completed }) add("illumined-graduate")
                            }
                            onCompleted(lesson.id, badges, {
                                if (draftKey != null) draftPreferences.edit().remove(draftKey).apply()
                                saving = false
                                message = if (linkedPrompt == null) lessonT("Correct! You scored 100% and completed this lesson.", "¡Correcto! Obtuviste el 100 % y completaste esta lección.") else lessonT("Correct! You scored 100%. Opening the discussion assignment...", "¡Correcto! Obtuviste el 100 %. Abriendo la discusión de la tarea...")
                                pendingHandoff = true
                            }, {
                                saving = false
                                message = lessonT("Your score was correct, but progress could not be saved. Please try again.", "Tu puntuación fue correcta, pero no se pudo guardar el progreso. Inténtalo de nuevo.")
                            })
                            }
                        }
                    },
                    enabled = answers.size == lesson.localizedQuiz.size && !saving,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(lessonT("Submit Quiz", "Enviar cuestionario"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                message?.let {
                    Spacer(Modifier.height(12.dp))
                    if (incorrectIds.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LessonSymbol(LessonSymbolKind.WarningCircle, Color(0xFFB3261E), Modifier.size(19.dp))
                            Spacer(Modifier.size(7.dp))
                            Text(it, color = Color(0xFFB3261E), fontWeight = FontWeight.SemiBold)
                        }
                    } else Text(it, color = if (it.startsWith(lessonT("Correct", "¡Correcto"))) Color(0xFF2E8B57) else Color(0xFFB3261E))
                }
            }
        }
    }
}

@Composable
private fun IlluminedPage(
    state: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(parchmentBrush())) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
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
        if (icon == LessonCategoryIcon.PRAYING_HANDS) {
            Icon(
                painter = painterResource(R.drawable.ic_praying_hands),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(25.dp),
            )
        } else Canvas(Modifier.size(25.dp)) {
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
                LessonCategoryIcon.PRAYING_HANDS -> Unit
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
    TextButton(onClick = onBack) { Text(lessonT("‹ Back", "‹ Atrás"), color = IlluminedThemeTokens.Blue, fontSize = 16.sp) }
}

@Composable
private fun IlluminedPageTitle(title: String) {
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
}

@Composable
private fun LessonUnavailable(message: String) {
    Box(Modifier.fillMaxSize().background(parchmentBrush()), contentAlignment = Alignment.Center) {
        Text("${lessonT("Lessons Unavailable", "Lecciones no disponibles")}\n$message", color = Color(0xFFB3261E), modifier = Modifier.padding(24.dp))
    }
}

private fun categoryBadge(name: String): String? = when (name) {
    "Profession of Faith" -> "foundations-complete"
    "Celebration of the Christian Mysteries" -> "celebration-complete"
    "Life in Christ" -> "life-in-christ-complete"
    "Christian Prayer" -> "prayer-complete"
    else -> null
}

private fun lessonCompletionBadges(
    lesson: CatechismLesson,
    category: LessonCategory,
    allCategories: List<LessonCategory>,
    completedLessonIds: Set<String>,
): List<String> {
    val completed = completedLessonIds + lesson.id
    return buildList {
        if (category.lessons.all { it.id in completed }) categoryBadge(category.name)?.let(::add)
        if (allCategories.flatMap { it.lessons }.isNotEmpty() && allCategories.flatMap { it.lessons }.all { it.id in completed }) add("illumined-graduate")
    }
}

private fun parchmentBrush(): Brush = Brush.radialGradient(
    colors = listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream),
    radius = 1500f,
)

private fun hasLessonDraft(context: android.content.Context, userId: String, classId: String, lesson: CatechismLesson): Boolean = userId.isNotBlank() && classId.isNotBlank() && runCatching {
        val key = org.json.JSONArray(listOf(userId, classId, lesson.id)).toString()
        val saved = org.json.JSONObject(context.getSharedPreferences("illumined.quizDrafts.v1", android.content.Context.MODE_PRIVATE).getString(key, null) ?: "{}")
        val signature = org.json.JSONArray().apply { lesson.localizedQuiz.forEach { q -> put(org.json.JSONArray().put(q.question).put(org.json.JSONArray(q.options)).put(q.correctAnswerIndex)) } }.toString()
        val answers = saved.optJSONArray("answers")
        saved.optString("signature") == signature && lesson.localizedQuiz.withIndex().any { (i, q) -> (answers?.optInt(i, -1) ?: -1) in q.options.indices }
    }.getOrDefault(false)


@Composable
private fun LessonTrackerCard(total: Int, completed: Int, nextTitle: String?, onContinue: () -> Unit) {
    val usesStackedTracker = ResponsivePresentation.usesStackedTracker(androidx.compose.ui.platform.LocalDensity.current.fontScale)
                Surface(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().walkthroughAnchor("tracker").border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
                    color = Color.White.copy(.94f), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (usesStackedTracker) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.home_lesson_tracker), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Text("$completed/${total}", color = IlluminedThemeTokens.Blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row { Text(stringResource(R.string.home_lesson_tracker), fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("$completed/${total}", color = IlluminedThemeTokens.Blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                        }
                        Text(nextTitle ?: lessonT("All lessons completed.", "Todas las lecciones completadas."), color = IlluminedThemeTokens.Blue)
                        LinearProgressIndicator(progress = { if (total == 0) 0f else completed.toFloat() / total }, modifier = Modifier.fillMaxWidth(), color = IlluminedThemeTokens.Gold)
                        if (usesStackedTracker) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrackerStat(stringResource(R.string.home_completed), "$completed", IlluminedThemeTokens.Blue, Modifier.fillMaxWidth())
                                TrackerStat(stringResource(R.string.home_uncompleted), (total - completed).toString(), IlluminedThemeTokens.Gold, Modifier.fillMaxWidth())
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrackerStat(stringResource(R.string.home_completed), "$completed", IlluminedThemeTokens.Blue, Modifier.weight(1f))
                                TrackerStat(stringResource(R.string.home_uncompleted), (total - completed).toString(), IlluminedThemeTokens.Gold, Modifier.weight(1f))
                            }
                        }
                    }
                }

}
