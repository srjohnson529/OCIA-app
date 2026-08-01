package com.illumined.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.illumined.app.BuildConfig
import com.illumined.app.R
import com.illumined.app.data.FormationOverview
import com.illumined.app.data.FormationRepository
import com.illumined.app.data.Assignment
import com.illumined.app.data.AssignmentCompletion
import com.illumined.app.data.AssignmentReading
import com.illumined.app.data.CatechismLesson
import com.illumined.app.data.DefaultClassSchedule
import com.illumined.app.data.InstructorRepository
import com.illumined.app.data.InstructorReadinessCalculator
import com.illumined.app.data.LessonCatalog
import com.illumined.app.data.PrayerRequest
import com.illumined.app.ui.theme.IlluminedTheme
import com.illumined.app.ui.theme.IlluminedThemeTokens
import com.illumined.app.notifications.NotificationRegistrar
import kotlinx.coroutines.delay

private sealed interface SessionState {
    data object SignedOut : SessionState
    data object Working : SessionState
    data class SignedIn(val userId: String, val email: String) : SessionState
    data class Error(val message: String) : SessionState
}

private enum class FormationSection(val label: String) {
    Home("Home"),
    Lessons("Lessons"),
    Discussion("Discussion"),
    Formation("Formation"),
    More("More"),
}

private class AuthController(context: Context) {
    private val auth: FirebaseAuth? = if (BuildConfig.FIREBASE_CONFIGURED) {
        FirebaseApp.initializeApp(context)?.let { FirebaseAuth.getInstance() }
    } else {
        null
    }

    fun initialState(): SessionState = auth?.currentUser?.let {
        SessionState.SignedIn(it.uid, it.email.orEmpty())
    } ?: SessionState.SignedOut

    fun signIn(email: String, password: String, update: (SessionState) -> Unit) {
        val firebaseAuth = auth ?: run {
            update(SessionState.Error("Firebase needs to be connected before sign-in can be used."))
            return
        }

        update(SessionState.Working)
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                val user = firebaseAuth.currentUser
                update(SessionState.SignedIn(user?.uid.orEmpty(), user?.email.orEmpty()))
            }
            .addOnFailureListener { update(SessionState.Error(AuthErrorPresentation.message(it))) }
    }

    fun createAccount(email: String, password: String, update: (SessionState) -> Unit) {
        val firebaseAuth = auth ?: run {
            update(SessionState.Error("Firebase needs to be connected before account creation can be used."))
            return
        }
        update(SessionState.Working)
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                val user = firebaseAuth.currentUser
                update(SessionState.SignedIn(user?.uid.orEmpty(), user?.email.orEmpty()))
            }
            .addOnFailureListener { update(SessionState.Error(AuthErrorPresentation.message(it))) }
    }

    fun resetPassword(email: String, update: (SessionState) -> Unit, done: () -> Unit) {
        val firebaseAuth = auth ?: run {
            update(SessionState.Error("Firebase needs to be connected before password reset can be used."))
            return
        }

        update(SessionState.Working)
        firebaseAuth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                update(SessionState.SignedOut)
                done()
            }
            .addOnFailureListener { update(SessionState.Error(AuthErrorPresentation.message(it))) }
    }

    fun signOut(): SessionState {
        auth?.signOut()
        return SessionState.SignedOut
    }
}

@Composable
fun IlluminedApp() {
    IlluminedTheme {
        val context = LocalContext.current
        val controller = remember { AuthController(context.applicationContext) }
        var session by remember { mutableStateOf(controller.initialState()) }
        var showBrandedLaunch by remember { mutableStateOf(true) }

        // Android 12's mandatory system splash is icon-only. This brief in-app
        // handoff recreates the full iOS launch composition, including its motto.
        LaunchedEffect(Unit) {
            delay(500)
            showBrandedLaunch = false
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            if (showBrandedLaunch) {
                BrandedLaunchScreen()
            } else {
                when (val current = session) {
                    is SessionState.SignedIn -> FormationHome(
                        userId = current.userId,
                        email = current.email,
                        onSignOut = { session = controller.signOut() },
                    )
                    else -> SignInScreen(
                        state = current,
                        isConfigured = BuildConfig.FIREBASE_CONFIGURED,
                        onSignIn = { email, password ->
                            controller.signIn(email, password) { session = it }
                        },
                        onCreateAccount = { email, password ->
                            controller.createAccount(email, password) { session = it }
                        },
                        onResetPassword = { email, done ->
                            controller.resetPassword(email, { session = it }, done)
                        },
                        onClearMessage = { if (session is SessionState.Error) session = SessionState.SignedOut },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandedLaunchScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // The iOS launch mark is 200pt. This is deliberately 50% larger.
            Image(
                painter = painterResource(R.drawable.illumined_launch_icon),
                contentDescription = "Illumined",
                modifier = Modifier.size(300.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Illumined", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.width(78.dp).height(1.dp).background(IlluminedThemeTokens.Gold.copy(.95f)))
                    Box(Modifier.size(4.dp).background(IlluminedThemeTokens.Gold.copy(.98f), CircleShape))
                    Box(Modifier.width(78.dp).height(1.dp).background(IlluminedThemeTokens.Gold.copy(.95f)))
                }
                Text("BEING • TRUTH • GOODNESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IlluminedThemeTokens.Gold.copy(.95f))
            }
        }
    }
}

@Composable
private fun SignInScreen(
    state: SessionState,
    isConfigured: Boolean,
    onSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onResetPassword: (String, () -> Unit) -> Unit,
    onClearMessage: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    val isWorking = state is SessionState.Working
    val message = (state as? SessionState.Error)?.message ?: localMessage

    if (showReset) {
        PasswordResetScreen(
            email = resetEmail,
            working = isWorking,
            message = message,
            onEmail = { resetEmail = it; localMessage = null; onClearMessage() },
            onBack = { if (!isWorking) showReset = false },
            onSend = {
                val cleaned = resetEmail.trim()
                val resetError = AuthErrorPresentation.resetEmailError(cleaned, Patterns.EMAIL_ADDRESS.matcher(cleaned).matches())
                if (resetError == null) onResetPassword(cleaned) {
                    localMessage = AuthErrorPresentation.ResetEmailSent
                    showReset = AuthPresentation.resetVisibleAfterSuccessfulSend(showReset)
                } else localMessage = resetError
            },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f))) {
        IlluminedBrandHeader()
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AuthCard {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(AuthPresentation.introTitle(isCreatingAccount), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    Text(AuthPresentation.IntroDescription, fontSize = 16.sp, color = IlluminedThemeTokens.SecondaryText)
                }
            }
            AuthCard {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Account", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; localMessage = null; onClearMessage() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                enabled = !isWorking,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; localMessage = null; onClearMessage() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                enabled = !isWorking,
            )
            Button(
                onClick = {
                    when {
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                            localMessage = "Enter a valid email address."
                        password.isBlank() -> localMessage = "Enter your password."
                        else -> if (isCreatingAccount) onCreateAccount(email, password) else onSignIn(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isWorking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IlluminedThemeTokens.Blue,
                    contentColor = Color.White,
                ),
            ) {
                if (isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isCreatingAccount) "Create Account" else "Sign In", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
                    OutlinedButton(onClick = { isCreatingAccount = !isCreatingAccount; localMessage = null; onClearMessage() }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !isWorking) { Text(if (isCreatingAccount) "Use Existing Account" else "Create New Account") }
                    if (!isCreatingAccount) TextButton(onClick = { resetEmail = email; localMessage = null; onClearMessage(); showReset = true }, modifier = Modifier.fillMaxWidth(), enabled = !isWorking) { Text("Forgot Password?") }
                }
            }
            message?.let { AuthMessageCard(it, it == AuthErrorPresentation.ResetEmailSent) }
            if (!isConfigured) Text("Developer setup: add google-services.json to the app folder.", color = Color.Red)
        }
    }
}

@Composable
private fun PasswordResetScreen(
    email: String,
    working: Boolean,
    message: String?,
    onEmail: (String) -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit,
) {
    BackHandler(enabled = !working) { onBack() }
    Column(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f),
        ),
    ) {
        IlluminedBrandHeader()
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AuthCard {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(AuthPresentation.ResetTitle, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    Text(
                        AuthPresentation.ResetDescription,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = IlluminedThemeTokens.SecondaryText,
                    )
                }
            }
            AuthCard {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmail,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        enabled = !working,
                    )
                    Button(
                        onClick = onSend,
                        enabled = email.trim().isNotEmpty() && !working,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (working) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Text("Send Reset Email", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Back to Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
            message?.let { AuthMessageCard(it, it == AuthErrorPresentation.ResetEmailSent) }
        }
    }
}

@Composable
private fun AuthCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(.94f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f)),
        content = content,
    )
}

@Composable
private fun AuthMessageCard(message: String, success: Boolean) {
    AuthCard {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DiscussionSymbol(if(success)DiscussionSymbolKind.CheckSeal else DiscussionSymbolKind.Warning,if(success)IlluminedThemeTokens.Blue else Color.Red,Modifier.size(18.dp))
            Text(message, fontSize = 15.sp, color = if (success) IlluminedThemeTokens.Blue else Color.Red, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FormationHome(userId: String, email: String, onSignOut: () -> Unit) {
    val repository = remember { FormationRepository() }
    val notificationRegistrar = remember { NotificationRegistrar() }
    val notificationContext = LocalContext.current
    var overview by remember { mutableStateOf<FormationOverview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedAssignmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf(FormationSection.Home) }
    var selectedSectionReset by remember { mutableIntStateOf(0) }
    var completionWorking by remember { mutableStateOf(false) }
    var completionError by remember { mutableStateOf<String?>(null) }
    var profileReload by remember { mutableIntStateOf(0) }

    BackHandler(enabled = selectedAssignmentId != null) {
        if (!completionWorking) {
            selectedAssignmentId = null
            completionError = null
        }
    }

    DisposableEffect(userId, profileReload) {
        val listener = repository.listenOverview(
            userId = userId,
            onSuccess = { updated ->
                val hadOverview = overview != null
                overview = updated
                if (!hadOverview) error = null
            },
            onError = { error = "We couldn’t load your formation. Please try again." },
        )
        onDispose { listener.remove() }
    }

    if (OverviewPresentation.errorPresentation(overview != null, error) == OverviewErrorPresentation.Alert) {
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Dashboard Error") },
            text = { Text(error.orEmpty()) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }

    val notificationClassId = overview?.profile?.classIds?.firstOrNull().orEmpty()
    LaunchedEffect(userId, notificationClassId, overview?.profile?.isConfigured) {
        val permissionGranted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(notificationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (overview?.profile?.isConfigured == true && permissionGranted) notificationRegistrar.register(notificationClassId, {}, {})
    }

    val nextSession = overview?.let { DefaultClassSchedule.next(it.schedule) }
    val selectedAssignment = overview?.assignments?.firstOrNull { it.id == selectedAssignmentId }
    LaunchedEffect(selectedAssignmentId, overview?.assignments) {
        if (overview != null && selectedAssignmentId != null && selectedAssignment == null) {
            selectedAssignmentId = null
        }
    }

    if (overview?.profile?.isConfigured == false) {
        Column(Modifier.fillMaxSize().background(IlluminedThemeTokens.Cream)) {
            IlluminedBrandHeader()
            ProfileSetupExperience(onComplete = { profileReload += 1 })
        }
        return
    }

    selectedAssignment?.let { assignment ->
        val activeProfile = overview?.profile ?: return@let
        val completedReadingIds = overview?.assignmentCompletions.orEmpty()
            .filter { it.parentAssignmentId == assignment.id && it.assignmentItemType == "reading" && it.isCompleted }
            .map { it.assignmentItemId }.toSet()
        Column(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f),
            ),
        ) {
            IlluminedBrandHeader()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LessonScreen(
            assignment = assignment,
            userId = userId,
            profile = overview?.profile,
            prompts = overview?.discussionPrompts.orEmpty(),
            assignments = overview?.assignments.orEmpty(),
            isComplete = assignment.id in overview?.completedAssignmentIds.orEmpty(),
            isWorking = completionWorking,
            error = completionError,
            completedReadingIds = completedReadingIds,
            completedLessonIds = overview?.profile?.completedLessons.orEmpty(),
            onBack = { selectedAssignmentId = null; completionError = null },
            onComplete = {
                completionWorking = true
                completionError = null
                repository.setAssignmentCompleted(
                    profile = activeProfile,
                    assignment = assignment,
                    completed = assignment.id !in overview?.completedAssignmentIds.orEmpty(),
                    onSuccess = {
                        val nowCompleted = assignment.id !in overview?.completedAssignmentIds.orEmpty()
                        overview = overview?.copy(
                            completedAssignmentIds = if (nowCompleted) overview!!.completedAssignmentIds + assignment.id else overview!!.completedAssignmentIds - assignment.id,
                        )
                        completionWorking = false
                    },
                    onError = {
                        completionWorking = false
                        completionError = "We couldn’t save your progress. Please try again."
                    },
                )
            },
            onSetReadingCompleted = { reading, completed ->
                completionWorking = true
                completionError = null
                repository.setReadingCompleted(activeProfile, assignment, reading, completed, completedReadingIds, {
                    val readingKey = "${assignment.id}__reading__${reading.id}"
                    val parentCompleted = InstructorReadinessCalculator.parentCompletedAfterReadingChange(assignment.readings.map { it.id }, reading.id, completed, completedReadingIds)
                    val updatedRecords = overview?.assignmentCompletions.orEmpty().filterNot { it.assignmentId == readingKey || it.assignmentId == assignment.id } + listOf(
                        AssignmentCompletion(readingKey, userId, overview?.profile?.displayName.orEmpty(), completed, assignment.id, reading.id, reading.title, "reading"),
                        AssignmentCompletion(assignment.id, userId, overview?.profile?.displayName.orEmpty(), parentCompleted),
                    )
                    overview = overview?.copy(
                        assignmentCompletions = updatedRecords,
                        completedAssignmentIds = if (parentCompleted) overview!!.completedAssignmentIds + assignment.id else overview!!.completedAssignmentIds - assignment.id,
                    )
                    completionWorking = false
                }, {
                    completionWorking = false
                    completionError = "We couldn’t save your reading progress. Please try again."
                })
            },
            onMarkLessonComplete = { lessonId, badgeIds, success, failure ->
                repository.markLessonComplete(lessonId, badgeIds, {
                    overview = overview?.copy(profile = overview!!.profile.copy(
                        completedLessons = overview!!.profile.completedLessons + lessonId,
                        earnedBadges = overview!!.profile.earnedBadges + badgeIds,
                    ))
                    success()
                }, { failure() })
            },
            onCompleteLinkedAssignment = { linkedAssignment, success, failure ->
                repository.setAssignmentCompleted(activeProfile, linkedAssignment, true, {
                    overview = overview?.copy(completedAssignmentIds = overview!!.completedAssignmentIds + linkedAssignment.id)
                    success()
                }, { failure() })
            },
                )
            }
            FormationNavigation(selectedSection) { destination ->
                selectedAssignmentId = null
                completionError = null
                if (selectedSection == destination) selectedSectionReset += 1 else selectedSection = destination
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream),
                radius = 1600f,
            ),
        ),
    ) {
        IlluminedBrandHeader()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            key(selectedSection, selectedSectionReset) { when (selectedSection) {
                FormationSection.Home -> HomeSection(
                    userId = userId,
                    email = email,
                    repository = repository,
                    overview = overview,
                    error = error,
                    nextSessionTitle = nextSession?.topic,
                    nextSessionDate = nextSession?.date,
                    onOpenAssignment = { selectedAssignmentId = it.id },
                    onPrayerPosted = { profileReload += 1 },
                    onRetry = { error = null; profileReload += 1 },
                )
                FormationSection.Lessons -> LessonsExperience(
                    userId = userId,
                    profile = overview?.profile,
                    prompts = overview?.discussionPrompts.orEmpty(),
                    assignments = overview?.assignments.orEmpty(),
                    completedLessonIds = overview?.profile?.completedLessons.orEmpty(),
                    onCompleteAssignment = { assignment, success, failure ->
                        repository.setAssignmentCompleted(
                            profile = overview!!.profile,
                            assignment = assignment,
                            completed = true,
                            onSuccess = { overview = overview?.copy(completedAssignmentIds = overview!!.completedAssignmentIds + assignment.id); success() },
                            onError = { failure() },
                        )
                    },
                    onMarkComplete = { lessonId, badgeIds, success, failure ->
                        repository.markLessonComplete(
                            lessonId = lessonId,
                            badgeIds = badgeIds,
                            onSuccess = {
                                overview = overview?.copy(
                                    profile = overview!!.profile.copy(
                                        completedLessons = overview!!.profile.completedLessons + lessonId,
                                    ),
                                )
                                success()
                            },
                            onError = { failure() },
                        )
                    },
                )
                FormationSection.Discussion -> DiscussionExperience(
                    userId = userId,
                    profile = overview?.profile,
                    prompts = overview?.discussionPrompts.orEmpty(),
                    assignments = overview?.assignments.orEmpty(),
                    loadError = OverviewPresentation.sectionLoadError(overview != null, error),
                    onCompleteAssignment = { assignment, success, failure ->
                        repository.setAssignmentCompleted(
                            profile = overview!!.profile,
                            assignment = assignment,
                            completed = true,
                            onSuccess = {
                                overview = overview?.copy(
                                    completedAssignmentIds = overview!!.completedAssignmentIds + assignment.id,
                                )
                                success()
                            },
                            onError = { failure() },
                        )
                    },
                )
                FormationSection.Formation -> SpiritualFormationExperience(
                    memorizedPrayerIds = overview?.profile?.memorizedPrayerIds.orEmpty(),
                    selectedPrayerIds = overview?.profile?.selectedPrayerIds.orEmpty(),
                    completedMysteryIds = overview?.profile?.completedMysteries.orEmpty(),
                    onSetPrayerMemorized = { prayerId, memorized, success, failure ->
                        repository.setPrayerMemorized(
                            prayerId = prayerId,
                            memorized = memorized,
                            onSuccess = {
                                overview = overview?.copy(
                                    profile = overview!!.profile.copy(
                                        memorizedPrayerIds = if (memorized) {
                                            overview!!.profile.memorizedPrayerIds + prayerId
                                        } else {
                                            overview!!.profile.memorizedPrayerIds - prayerId
                                        },
                                    ),
                                )
                                success()
                            },
                            onError = { failure() },
                        )
                    },
                    onSetPrayerSelected = { prayerId, selected, success, failure ->
                        repository.setPrayerSelected(
                            prayerId = prayerId,
                            selected = selected,
                            onSuccess = {
                                overview = overview?.copy(
                                    profile = overview!!.profile.copy(
                                        selectedPrayerIds = if (selected) {
                                            overview!!.profile.selectedPrayerIds + prayerId
                                        } else {
                                            overview!!.profile.selectedPrayerIds - prayerId
                                        },
                                    ),
                                )
                                success()
                            },
                            onError = { failure() },
                        )
                    },
                    onCompleteMystery = { mysteryId, success, failure ->
                        repository.markRosaryMysteryComplete(
                            mysteryId = mysteryId,
                            onSuccess = {
                                overview = overview?.copy(
                                    profile = overview!!.profile.copy(
                                        completedMysteries = overview!!.profile.completedMysteries + mysteryId,
                                        earnedBadges = overview!!.profile.earnedBadges + "rosary-$mysteryId",
                                    ),
                                )
                                success()
                            },
                            onError = { failure() },
                        )
                    },
                )
                FormationSection.More -> MoreExperience(
                    userId = userId,
                    email = email,
                    profile = overview?.profile,
                    schedule = overview?.schedule.orEmpty(),
                    assignments = overview?.assignments.orEmpty(),
                    prompts = overview?.discussionPrompts.orEmpty(),
                    onSignOut = onSignOut,
                )
            } }
        }
        FormationNavigation(selectedSection) {
            if (selectedSection == it) selectedSectionReset += 1 else selectedSection = it
        }
    }
}

@Composable
private fun CommunitySection(overview: FormationOverview?, error: String?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 28.dp, end = 28.dp, top = 44.dp, bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Discussion", color = IlluminedThemeTokens.Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            Text("Discuss, reflect, and pray with your OCIA class", color = IlluminedThemeTokens.SecondaryText)
            Spacer(modifier = Modifier.height(12.dp))
        }
        when {
            error != null -> item { Text(error, color = Color(0xFFFFB4AB)) }
            overview == null -> item { LoadingFormation() }
            else -> {
                item {
                    Text("DISCUSSIONS", color = IlluminedThemeTokens.Gold, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                }
                if (overview.discussionPrompts.isEmpty()) {
                    item { FormationCard("DISCUSSIONS", "No active prompts", "Your instructor’s prompts will appear here.") }
                } else {
                    items(overview.discussionPrompts, key = { "prompt-${it.id}" }) { prompt ->
                        FormationCard(
                            eyebrow = if (prompt.requiredForAssignment) "REQUIRED DISCUSSION" else "DISCUSSION",
                            title = prompt.title,
                            detail = prompt.prompt,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSection(
    userId: String,
    email: String,
    repository: FormationRepository,
    overview: FormationOverview?,
    error: String?,
    nextSessionTitle: String?,
    nextSessionDate: java.util.Date?,
    onOpenAssignment: (Assignment) -> Unit,
    onPrayerPosted: () -> Unit,
    onRetry: () -> Unit,
) {
    val usesStackedTracker = ResponsivePresentation.usesStackedTracker(LocalDensity.current.fontScale)
    val announcementRepository = remember { InstructorRepository() }
    var announcements by remember { mutableStateOf(emptyList<com.illumined.app.data.Announcement>()) }
    var prayerComposer by rememberSaveable { mutableStateOf(false) }
    var selectedPrayerId by rememberSaveable { mutableStateOf<String?>(null) }
    var assignmentsOpen by rememberSaveable { mutableStateOf(false) }
    var prayerTitle by rememberSaveable { mutableStateOf("") }
    var prayerDetails by rememberSaveable { mutableStateOf("") }
    var prayerWorking by remember { mutableStateOf(false) }
    var prayerError by remember { mutableStateOf<String?>(null) }
    val classId = overview?.profile?.classIds?.firstOrNull().orEmpty()
    val selectedPrayer = overview?.prayerRequests?.firstOrNull { it.id == selectedPrayerId }

    BackHandler(enabled = selectedPrayerId != null || prayerComposer || assignmentsOpen) {
        when {
            selectedPrayerId != null -> selectedPrayerId = null
            prayerComposer && !prayerWorking -> prayerComposer = false
            assignmentsOpen -> assignmentsOpen = false
        }
    }
    val homeContext = LocalContext.current
    val allLessons = remember { LessonCatalog.load(homeContext.applicationContext).getOrNull().orEmpty().flatMap { it.lessons } }
    val lessonCompleteCount = overview?.profile?.completedLessons?.count { completed -> allLessons.any { it.id == completed } } ?: 0
    val lessonRemaining = (allLessons.size - lessonCompleteCount).coerceAtLeast(0)
    DisposableEffect(classId) {
        val listener = if (classId.isNotBlank()) announcementRepository.listenAnnouncements(
            classId, { announcements = it.filter { item -> item.isActive } }, {},
        ) else null
        onDispose { listener?.remove() }
    }
    selectedPrayer?.let { request ->
        PrayerRequestDetail(request) { selectedPrayerId = null }
        return
    }
    if (prayerComposer) {
        PrayerRequestComposer(
            title = prayerTitle,
            details = prayerDetails,
            working = prayerWorking,
            error = prayerError,
            onTitle = { prayerTitle = it },
            onDetails = { prayerDetails = it },
            onCancel = { if (!prayerWorking) prayerComposer = false },
            onPost = {
                val profile = overview?.profile ?: return@PrayerRequestComposer
                prayerWorking = true; prayerError = null
                repository.createPrayerRequest(profile,prayerTitle,prayerDetails,{
                    prayerWorking=false;prayerComposer=false;prayerTitle="";prayerDetails="";onPrayerPosted()
                }, { throwable ->
                    prayerWorking = false
                    prayerError = throwable.message ?: "Prayer request could not be posted."
                })
            },
        )
        return
    }
    if (assignmentsOpen && overview != null) {
        HomeAssignmentsList(
            assignments = overview.assignments,
            completedIds = overview.completedAssignmentIds,
            onBack = { assignmentsOpen = false },
            onOpen = onOpenAssignment,
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 44.dp),
    ) {
        when {
            OverviewPresentation.errorPresentation(overview != null, error) == OverviewErrorPresentation.Blocking ->
                FormationLoadUnavailable(error.orEmpty(), onRetry)
            overview == null -> LoadingFormation()
            else -> {
                HomeWelcomeCard(overview.profile)
                Spacer(modifier = Modifier.height(14.dp))
                NextClassTopicCard(nextSessionTitle, nextSessionDate)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
                    color = Color.White.copy(.94f), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (usesStackedTracker) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Lesson Tracker", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Text("$lessonCompleteCount/${allLessons.size}", color = IlluminedThemeTokens.Blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row { Text("Lesson Tracker", fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("$lessonCompleteCount/${allLessons.size}", color = IlluminedThemeTokens.Blue, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                        }
                        LinearProgressIndicator(progress = { if (allLessons.isEmpty()) 0f else lessonCompleteCount.toFloat() / allLessons.size }, modifier = Modifier.fillMaxWidth(), color = IlluminedThemeTokens.Gold)
                        if (usesStackedTracker) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrackerStat("Completed", "$lessonCompleteCount", IlluminedThemeTokens.Blue, Modifier.fillMaxWidth())
                                TrackerStat("Uncompleted", "$lessonRemaining", IlluminedThemeTokens.Gold, Modifier.fillMaxWidth())
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrackerStat("Completed", "$lessonCompleteCount", IlluminedThemeTokens.Blue, Modifier.weight(1f))
                                TrackerStat("Uncompleted", "$lessonRemaining", IlluminedThemeTokens.Gold, Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                HomeAnnouncementsCard(announcements.take(3))
                Spacer(modifier = Modifier.height(14.dp))
                HomeAssignmentsCard(
                    assignments = overview.assignments,
                    completedIds = overview.completedAssignmentIds,
                    onClick = { assignmentsOpen = true },
                )
                Spacer(modifier = Modifier.height(14.dp))
                HomePrayerRequestsCard(
                    requests = overview.prayerRequests.take(5),
                    canPost = classId.isNotBlank(),
                    onNewRequest = { prayerComposer = true },
                    onOpen = { selectedPrayerId = it.id },
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun HomeWelcomeCard(profile: com.illumined.app.data.UserProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
        color = Color.White.copy(.94f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Welcome, ${profile.displayName}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeSymbol(HomeSymbolKind.ClassMembers, IlluminedThemeTokens.SecondaryText, Modifier.size(22.dp))
                Text(profile.classIds.firstOrNull().orEmpty().ifBlank { "No class assigned" }, color = IlluminedThemeTokens.SecondaryText)
            }
        }
    }
}

@Composable
private fun HomePrayerRequestsCard(
    requests: List<PrayerRequest>,
    canPost: Boolean,
    onNewRequest: () -> Unit,
    onOpen: (PrayerRequest) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
        color = Color.White.copy(.94f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Prayer Requests", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("The 5 most recent active requests", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
            }
            Button(
                onClick = onNewRequest,
                enabled = canPost,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){InstructorSymbol(InstructorSymbolKind.PlusCircle,Color.White,Modifier.size(18.dp));Text("New Prayer Request",fontSize=15.sp,fontWeight=FontWeight.SemiBold)} }
            if (requests.isEmpty()) {
                Text(
                    "No active prayer requests yet. Be the first to invite the class to pray.",
                    color = IlluminedThemeTokens.SecondaryText,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else requests.forEach { request ->
                Surface(
                    onClick = { onOpen(request) },
                    color = IlluminedThemeTokens.Blue.copy(.07f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                        MassGuideSymbol(MassGuideSymbolKind.HandsSparkles,IlluminedThemeTokens.Gold,Modifier.size(width=28.dp,height=22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(request.requesterName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                            Text(request.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            val cleaned = request.details.trim()
                            Text(
                                if (cleaned.isEmpty()) "No additional details." else cleaned.let { if (it.length <= 50) it else "${it.take(50)}..." },
                                fontSize = 12.sp,
                                color = IlluminedThemeTokens.SecondaryText,
                                maxLines = 2,
                            )
                        }
                        LessonSymbol(LessonSymbolKind.ChevronRight,IlluminedThemeTokens.SecondaryText,Modifier.size(10.dp,16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NextClassTopicCard(topic: String?, date: java.util.Date?) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
        color = Color.White.copy(.94f), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(44.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                HomeSymbol(HomeSymbolKind.CalendarBadgeClock, IlluminedThemeTokens.Gold, Modifier.size(27.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Next Class Topic", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                Text(topic ?: "TBD", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                date?.let { Text(java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(it), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText) }
            }
        }
    }
}

@Composable
private fun HomeAnnouncementsCard(announcements: List<com.illumined.app.data.Announcement>) {
    Surface(modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), color = Color.White.copy(.94f), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Announcements", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    Text("Updates from your instructor", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                }
                HomeSymbol(HomeSymbolKind.Megaphone, IlluminedThemeTokens.Gold, Modifier.size(23.dp))
            }
            if (announcements.isEmpty()) Text("No announcements yet.", fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText, modifier = Modifier.padding(vertical = 8.dp))
            else announcements.forEach { announcement ->
                Column(Modifier.fillMaxWidth().background(IlluminedThemeTokens.Gold.copy(.09f), RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(announcement.title, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue, maxLines = 2)
                        Spacer(Modifier.width(10.dp)); Text(announcement.updatedAt?.toDate()?.let { java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it) }.orEmpty(), fontSize = 11.sp, color = IlluminedThemeTokens.SecondaryText)
                    }
                    Text(announcement.message, fontSize = 14.sp, color = IlluminedThemeTokens.Ink, maxLines = 3)
                }
            }
        }
    }
}

@Composable
private fun HomeAssignmentsCard(assignments: List<Assignment>, completedIds: Set<String>, onClick: () -> Unit) {
    val visible = homeAssignmentPreview(assignments)
    val remaining = remainingHomeAssignmentCount(assignments)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), color = Color.White.copy(.94f), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Assignments", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    Text(if (visible.isEmpty()) "No active assignments yet" else "${assignments.size} active assignment${if (assignments.size == 1) "" else "s"}", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                }
                HomeSymbol(HomeSymbolKind.Checklist, IlluminedThemeTokens.Gold, Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); HomeSymbol(HomeSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
            }
            if (visible.isEmpty()) Text("Tap here when your instructor posts assignments.", fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText, modifier = Modifier.padding(vertical = 8.dp))
            else visible.forEach { assignment -> AssignmentSummaryRow(assignment, assignment.id in completedIds) }
            if (remaining > 0) Text("+ $remaining more assignment${if (remaining == 1) "" else "s"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.SecondaryText)
        }
    }
}

@Composable
private fun AssignmentSummaryRow(assignment: Assignment, completed: Boolean) {
    Row(Modifier.fillMaxWidth().background(IlluminedThemeTokens.Blue.copy(.07f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
        LessonSymbol(if (completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff, if (completed) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(assignment.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue, maxLines = 2)
            Text(assignment.dueAt?.toDate()?.let { "Due ${java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it)}" } ?: "Due date not set", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.SecondaryText)
            assignment.homeContentLabel()?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LessonSymbol(if (assignment.readings.isNotEmpty()) LessonSymbolKind.DocumentText else LessonSymbolKind.BookClosed, IlluminedThemeTokens.Gold, Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(it, fontSize = 12.sp, color = IlluminedThemeTokens.Gold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HomeAssignmentsList(assignments: List<Assignment>, completedIds: Set<String>, onBack: () -> Unit, onOpen: (Assignment) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onBack) { Text("‹ Back") } }
        item { AssignmentDetailCard { Text("Assignments", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text("Select an assignment to open the full details, readings, lesson links, and completion check.", fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText) } }
        if (assignments.isEmpty()) item { FormationCard("ASSIGNMENTS", "No Assignments", "Your instructor has not posted active assignments yet.") }
        else items(assignments, key = { it.id }) { assignment ->
            Surface(onClick = { onOpen(assignment) }, modifier = Modifier.border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
                    val completed = assignment.id in completedIds
                    LessonSymbol(
                        if (completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff,
                        if (completed) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,
                        Modifier.size(22.dp).semantics { contentDescription = if (completed) "Completed" else "Not completed" },
                    )
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(assignment.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink, maxLines = 2)
                        Text(assignment.dueAt?.toDate()?.let { "Due ${java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it)}" } ?: "Due date not set", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (assignment.readings.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                                LessonSymbol(LessonSymbolKind.DocumentText, IlluminedThemeTokens.Gold, Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)); Text("${assignment.readings.size} reading${if (assignment.readings.size == 1) "" else "s"}", fontSize = 12.sp, color = IlluminedThemeTokens.Gold)
                            }
                            if (assignment.lessonLinks.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                                LessonSymbol(LessonSymbolKind.BookClosed, IlluminedThemeTokens.Gold, Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)); Text("${assignment.lessonLinks.size} lesson${if (assignment.lessonLinks.size == 1) "" else "s"}", fontSize = 12.sp, color = IlluminedThemeTokens.Gold)
                            }
                        }
                        if (assignment.instructions.isNotBlank()) Text(assignment.instructions, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText, maxLines = 2)
                    }; LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PrayerRequestDetail(request: PrayerRequest, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f))
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Spacer(Modifier.weight(1f))
            Text("Prayer Request", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(60.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
                .border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
            color = Color.White.copy(.94f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(request.requesterName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text(request.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IlluminedThemeTokens.Ink)
                androidx.compose.material3.HorizontalDivider()
                val cleanedDetails = request.details.trim()
                Text(
                    cleanedDetails.ifEmpty { "No additional details were added." },
                    fontSize = if (cleanedDetails.isEmpty()) 16.sp else 17.sp,
                    lineHeight = if (cleanedDetails.isEmpty()) 22.sp else 27.sp,
                    color = if (cleanedDetails.isEmpty()) IlluminedThemeTokens.SecondaryText else IlluminedThemeTokens.Ink,
                )
            }
        }
    }
}

@Composable
private fun PrayerRequestComposer(
    title: String,
    details: String,
    working: Boolean,
    error: String?,
    onTitle: (String) -> Unit,
    onDetails: (String) -> Unit,
    onCancel: () -> Unit,
    onPost: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f),
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel, enabled = !working, modifier = Modifier.size(width = 72.dp, height = 48.dp)) {
                    Text("‹ Back", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onPost, enabled = title.isNotBlank() && !working) {
                    Text(if (working) "Posting..." else "Post", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            AssignmentDetailCard {
                Text("New Prayer Request", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Share a request with your class so they can pray with you.",
                    fontSize = 15.sp,
                    color = IlluminedThemeTokens.SecondaryText,
                )
            }
        }
        item {
            AssignmentDetailCard {
                Text("Prayer Request", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true,
                    enabled = !working,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = onDetails,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Optional details") },
                    minLines = 4,
                    maxLines = 8,
                    enabled = !working,
                )
            }
        }
        item {
            AssignmentDetailCard {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LessonSymbol(LessonSymbolKind.Clock,IlluminedThemeTokens.Gold,Modifier.size(22.dp))
                    Text(
                        "Requests stay visible for 3 days and then expire from the board.",
                        fontSize = 15.sp,
                        color = IlluminedThemeTokens.SecondaryText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        error?.let { message ->
            item {
                AssignmentDetailCard {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DiscussionSymbol(DiscussionSymbolKind.Warning,Color.Red,Modifier.size(18.dp))
                        Text(message, fontSize = 15.sp, color = Color.Red, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonsSection(
    overview: FormationOverview?,
    error: String?,
    onOpenAssignment: (Assignment) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 28.dp, end = 28.dp, top = 44.dp, bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Lessons", color = IlluminedThemeTokens.Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            Text("Your assignments and formation readings", color = IlluminedThemeTokens.SecondaryText)
            Spacer(modifier = Modifier.height(12.dp))
        }
        when {
            error != null -> item { Text(error, color = Color(0xFFFFB4AB)) }
            overview == null -> item { LoadingFormation() }
            overview.assignments.isEmpty() -> item {
                FormationCard("ASSIGNMENTS", "Nothing assigned yet", "New lessons will appear here.")
            }
            else -> items(overview.assignments, key = { it.id }) { assignment ->
                val completed = assignment.id in overview.completedAssignmentIds
                FormationCard(
                    eyebrow = if (completed) "COMPLETED" else "TO DO",
                    title = assignment.title,
                    detail = assignment.dueAt?.toDate()?.let {
                        buildString {
                            append("Due ${java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it)}")
                            if (assignment.lessonLinks.isNotEmpty()) append("  •  ${assignment.lessonLinks.size} lesson${if (assignment.lessonLinks.size == 1) "" else "s"}")
                            if (assignment.readings.isNotEmpty()) append("  •  ${assignment.readings.size} reading${if (assignment.readings.size == 1) "" else "s"}")
                        }
                    } ?: assignment.instructions,
                    onClick = { onOpenAssignment(assignment) },
                )
            }
        }
    }
}

@Composable
private fun ScheduleSection(overview: FormationOverview?, error: String?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 28.dp, end = 28.dp, top = 44.dp, bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Formation", color = IlluminedThemeTokens.Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            Text("Your spiritual formation", color = IlluminedThemeTokens.SecondaryText)
            Spacer(modifier = Modifier.height(12.dp))
        }
        when {
            error != null -> item { Text(error, color = Color(0xFFFFB4AB)) }
            overview == null -> item { LoadingFormation() }
            overview.schedule.isEmpty() -> item {
                FormationCard("SCHEDULE", "No sessions scheduled", "Check back with your instructor.")
            }
            else -> items(overview.schedule, key = { it.id }) { session ->
                FormationCard(
                    eyebrow = session.date?.toDate()?.let {
                        java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it).uppercase()
                    } ?: "SESSION",
                    title = session.topic,
                    detail = session.details,
                )
            }
        }
    }
}

@Composable
private fun ProfileSection(overview: FormationOverview?, email: String, onSignOut: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 44.dp)) {
        Text("More", color = IlluminedThemeTokens.Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))
        FormationCard(
            eyebrow = "PARTICIPANT",
            title = overview?.profile?.displayName ?: "Illumined participant",
            detail = email,
        )
        Spacer(modifier = Modifier.height(14.dp))
        FormationCard(
            eyebrow = "OCIA CLASS",
            title = overview?.profile?.classIds?.joinToString().orEmpty().ifBlank { "Not assigned" },
            detail = "Your parish formation group",
        )
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}

@Composable
private fun LoadingFormation() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Text("Loading your journey…", color = IlluminedThemeTokens.SecondaryText)
    }
}

@Composable
private fun FormationLoadUnavailable(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
            color = Color.White.copy(.94f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DiscussionSymbol(DiscussionSymbolKind.Warning,IlluminedThemeTokens.Gold,Modifier.size(34.dp))
                Text("Formation Unavailable", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                Text(message, color = IlluminedThemeTokens.SecondaryText, textAlign = TextAlign.Center, lineHeight = 21.sp)
                Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) { Text("Try Again") }
            }
        }
    }
}

@Composable
private fun FormationNavigation(
    selected: FormationSection,
    onSelected: (FormationSection) -> Unit,
) {
    val chromeFontScale = LocalDensity.current.fontScale
    Surface(color = Color.White.copy(alpha = 0.95f)) {
        Column(Modifier.navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(IlluminedThemeTokens.Ink.copy(alpha = 0.08f)))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                FormationSection.entries.forEach { section ->
                    TextButton(
                        onClick = { onSelected(section) },
                        modifier = Modifier.weight(1f)
                            .background(if (selected == section) IlluminedThemeTokens.Blue.copy(.08f) else Color.Transparent, RoundedCornerShape(AppChromePresentation.TabCornerRadius))
                            .semantics { this.selected = selected == section },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 7.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Image(
                                painter = painterResource(when (section) {
                                    FormationSection.Home -> R.drawable.ic_tab_home
                                    FormationSection.Lessons -> R.drawable.ic_tab_lessons
                                    FormationSection.Discussion -> R.drawable.ic_tab_discussion
                                    FormationSection.Formation -> R.drawable.ic_tab_formation
                                    FormationSection.More -> R.drawable.ic_tab_more
                                }),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(if (selected == section) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Ink),
                                modifier = Modifier.size(AppChromePresentation.TabIconSize),
                            )
                            Text(
                                section.label,
                                color = if (selected == section) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Ink,
                                fontSize = AppChromePresentation.fixedFontSize(AppChromePresentation.TabLabelSize.value, chromeFontScale).sp,
                                fontWeight = if (selected == section) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormationCard(
    eyebrow: String,
    title: String,
    detail: String,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth().border(
            1.dp, IlluminedThemeTokens.Gold.copy(alpha = 0.22f), RoundedCornerShape(16.dp),
        ),
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                eyebrow,
                color = IlluminedThemeTokens.Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                color = IlluminedThemeTokens.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(detail, color = IlluminedThemeTokens.SecondaryText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TrackerStat(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(color.copy(alpha = 0.07f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(title, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
    }
}

@Composable
private fun LessonScreen(
    assignment: Assignment,
    userId: String,
    profile: com.illumined.app.data.UserProfile?,
    prompts: List<com.illumined.app.data.DiscussionPrompt>,
    assignments: List<Assignment>,
    isComplete: Boolean,
    isWorking: Boolean,
    error: String?,
    completedReadingIds: Set<String>,
    completedLessonIds: Set<String>,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onSetReadingCompleted: (AssignmentReading, Boolean) -> Unit,
    onMarkLessonComplete: (String, List<String>, () -> Unit, () -> Unit) -> Unit,
    onCompleteLinkedAssignment: (Assignment, () -> Unit, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val lessonCategories = remember { LessonCatalog.load(context.applicationContext).getOrNull().orEmpty() }
    val linkedLessons = remember(assignment.id, lessonCategories) {
        val ids = assignment.lessonLinks.map { it.lessonId }.toSet()
        lessonCategories.flatMap { it.lessons }.filter { it.id in ids }
    }
    var selectedReading by remember(assignment.id) { mutableStateOf<AssignmentReading?>(null) }
    var selectedLesson by remember(assignment.id) { mutableStateOf<CatechismLesson?>(null) }
    BackHandler {
        if (!isWorking) {
            when {
                selectedLesson != null -> selectedLesson = null
                selectedReading != null -> selectedReading = null
                else -> onBack()
            }
        }
    }
    selectedLesson?.let { lesson ->
        AssignedLessonExperience(
            lesson = lesson,
            categories = lessonCategories,
            userId = userId,
            profile = profile,
            prompts = prompts,
            assignments = assignments,
            completedLessonIds = completedLessonIds,
            onCompleteAssignment = onCompleteLinkedAssignment,
            onBack = { selectedLesson = null },
            onMarkComplete = onMarkLessonComplete,
        )
        return
    }
    selectedReading?.let { reading ->
        AssignmentReadingScreen(
            assignment = assignment,
            reading = reading,
            completed = reading.id in completedReadingIds,
            isWorking = isWorking,
            error = error,
            onBack = { selectedReading = null },
            onToggle = { onSetReadingCompleted(reading, reading.id !in completedReadingIds) },
        )
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TextButton(onClick = onBack, enabled = !isWorking) { Text("‹ Back") }
        AssignmentDetailCard {
            Text(assignment.title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
            Text(
                assignment.dueAt?.toDate()?.let {
                    "Due ${java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(it)}"
                } ?: "Due date not set",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = IlluminedThemeTokens.SecondaryText,
            )
        }
        if (assignment.instructions.isNotBlank()) AssignmentDetailCard {
            Text("Instructions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(assignment.instructions, fontSize = 16.sp, lineHeight = 23.sp)
        }
        if (assignment.readings.isNotEmpty()) AssignmentDetailCard {
            Text("Assigned Readings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            assignment.readings.forEach { reading ->
                Surface(
                    onClick = { selectedReading = reading },
                    color = Color.White.copy(.72f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                            LessonSymbol(LessonSymbolKind.DocumentText, IlluminedThemeTokens.Gold, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(reading.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                reading.text.trim().replace(Regex("\\s+"), " ").let { if (it.length <= 25) it else "${it.take(25)}..." },
                                fontSize = 12.sp,
                                color = IlluminedThemeTokens.SecondaryText,
                                maxLines = 1,
                            )
                        }
                        LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
                    }
                }
            }
        }
        if (linkedLessons.isNotEmpty()) AssignmentDetailCard {
            Text("Lesson Links", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            linkedLessons.forEach { lesson ->
                val category = lessonCategories.firstOrNull { group -> group.lessons.any { it.id == lesson.id } }
                Surface(onClick = { selectedLesson = lesson }, color = Color.White.copy(.72f), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                            LessonSymbol(LessonSymbolKind.BookClosed, IlluminedThemeTokens.Gold, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(lesson.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            category?.let { Text(it.name, fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText) }
                        }
                        LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
                    }
                }
            }
        }
        error?.let { Text(it, color = Color.Red, modifier = Modifier.padding(horizontal = 4.dp)) }
        if (assignment.readings.isEmpty()) Button(
            onClick = onComplete,
            enabled = !isWorking,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            when {
                isWorking -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                isComplete -> { LessonSymbol(LessonSymbolKind.CheckCircle, Color.White, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Mark Assignment Incomplete", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                else -> { LessonSymbol(LessonSymbolKind.RadioOff, Color.White, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Mark Assignment Completed", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun AssignmentDetailCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
        color = Color.White.copy(.94f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun AssignmentReadingScreen(
    assignment: Assignment,
    reading: AssignmentReading,
    completed: Boolean,
    isWorking: Boolean,
    error: String?,
    onBack: () -> Unit,
    onToggle: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f))
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TextButton(onClick = onBack, enabled = !isWorking) { Text("‹ Back") }
        Surface(Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), color=Color.White.copy(.94f), shape=RoundedCornerShape(16.dp), shadowElevation=6.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(reading.title.trim(),fontSize=24.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)
                Text(assignment.title,fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.SecondaryText)
            }
        }
        Surface(Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), color=Color.White.copy(.94f), shape=RoundedCornerShape(16.dp), shadowElevation=6.dp) {
            Text(reading.text.trim(),Modifier.padding(20.dp),fontSize=17.sp,lineHeight=27.sp,color=IlluminedThemeTokens.Ink)
        }
        error?.let { Text(it,color=Color.Red) }
        Button(onClick=onToggle,enabled=!isWorking,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=IlluminedThemeTokens.Blue),shape=RoundedCornerShape(14.dp)) {
            if(isWorking) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp,color=Color.White)
            else {
                LessonSymbol(if(completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff, Color.White, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if(completed)"Mark Reading Incomplete" else "Mark Reading Completed",fontWeight=FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun BrandMark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(14.dp).background(Color(0xFFFFD77A), CircleShape))
        Text(
            "ILLUMINED",
            color = Color(0xFFF8F2FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun IlluminedBrandHeader() {
    val chromeFontScale = LocalDensity.current.fontScale
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // The logo asset is composed on the base Illumined blue. Keeping
            // the toolbar solid prevents its square artwork from showing a
            // different blue from the surrounding header.
            .background(IlluminedThemeTokens.Blue)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(AppChromePresentation.HeaderContentHeight)
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .semantics(mergeDescendants = true) { contentDescription = AppChromePresentation.HeaderAccessibilityLabel },
    ) {
        // The iOS toolbar centers this 230pt brand group, rather than pinning the mark to the screen edge.
        Box(modifier = Modifier.align(Alignment.Center).width(230.dp)) {
            Image(
                painter = painterResource(R.drawable.illumined_launch_icon),
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterStart).size(AppChromePresentation.HeaderIconSize).clip(RoundedCornerShape(7.dp)),
            )
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Illumined",
                    color = Color.White,
                    fontSize = AppChromePresentation.fixedFontSize(22f, chromeFontScale).sp,
                    lineHeight = AppChromePresentation.fixedFontSize(27f, chromeFontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(54.dp).height(0.5.dp).background(IlluminedThemeTokens.Gold.copy(.9f)))
                    Box(Modifier.padding(horizontal = 4.dp).width(2.5.dp).height(3.5.dp).background(IlluminedThemeTokens.Gold.copy(.95f), CircleShape))
                    Box(Modifier.width(54.dp).height(0.5.dp).background(IlluminedThemeTokens.Gold.copy(.9f)))
                }
                Text(
                    "BEING • TRUTH • GOODNESS",
                    color = IlluminedThemeTokens.Gold.copy(.95f),
                    fontSize = AppChromePresentation.fixedFontSize(8f, chromeFontScale).sp,
                    lineHeight = AppChromePresentation.fixedFontSize(10f, chromeFontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignInPreview() {
    IlluminedTheme {
        SignInScreen(
            state = SessionState.SignedOut,
            isConfigured = true,
            onSignIn = { _, _ -> },
            onCreateAccount = { _, _ -> },
            onResetPassword = { _, _ -> },
            onClearMessage = {},
        )
    }
}
