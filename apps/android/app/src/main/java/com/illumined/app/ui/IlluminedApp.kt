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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.illumined.app.DailyFormationOpenRequest
import com.illumined.app.R
import com.illumined.app.data.FormationOverview
import com.illumined.app.data.FormationRepository
import com.illumined.app.data.DailyFormationEntry
import com.illumined.app.data.Assignment
import com.illumined.app.data.AssignmentCompletion
import com.illumined.app.data.AssignmentReading
import com.illumined.app.data.CatechismLesson
import com.illumined.app.data.ClassScheduleDay
import com.illumined.app.data.ClassScheduleSelection
import com.illumined.app.data.DiscussionRepository
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

private enum class FormationSection(val labelResource: Int) {
    Home(R.string.nav_home),
    Lessons(R.string.nav_lessons),
    Discussion(R.string.nav_discussion),
    Formation(R.string.nav_formation),
    More(R.string.nav_more),
}

private class AuthController(context: Context) {
    private val auth: FirebaseAuth? = if (BuildConfig.FIREBASE_CONFIGURED) {
        FirebaseApp.initializeApp(context)?.let { FirebaseAuth.getInstance() }
    } else {
        null
    }

    fun initialState(): SessionState = when {
        auth?.currentUser != null -> SessionState.Working
        else -> SessionState.SignedOut
    }

    fun validateCachedSession(update: (SessionState) -> Unit) {
        val firebaseAuth = auth ?: return
        val cachedUser = firebaseAuth.currentUser ?: run {
            update(SessionState.SignedOut)
            return
        }

        cachedUser.reload()
            .addOnSuccessListener {
                val currentUser = firebaseAuth.currentUser
                update(
                    currentUser?.let { SessionState.SignedIn(it.uid, it.email.orEmpty()) }
                        ?: SessionState.SignedOut,
                )
            }
            .addOnFailureListener { problem ->
                if (AuthErrorPresentation.isInvalidCachedSession(problem)) {
                    firebaseAuth.signOut()
                    update(SessionState.SignedOut)
                } else {
                    // Keep a valid cached session during a temporary network outage.
                    val currentUser = firebaseAuth.currentUser
                    update(
                        currentUser?.let { SessionState.SignedIn(it.uid, it.email.orEmpty()) }
                            ?: SessionState.SignedOut,
                    )
                }
            }
    }

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
fun IlluminedApp(inviteUri: String? = null, dailyFormationOpenRequest: DailyFormationOpenRequest? = null) {
    IlluminedTheme {
        val context = LocalContext.current
        val controller = remember { AuthController(context.applicationContext) }
        val inviteStore = remember { PendingInviteStore(context.applicationContext) }
        var pendingInvite by remember { mutableStateOf(inviteStore.load()) }
        var session by remember { mutableStateOf(controller.initialState()) }
        var showBrandedLaunch by remember { mutableStateOf(true) }

        LaunchedEffect(controller) {
            controller.validateCachedSession { session = it }
        }

        LaunchedEffect(inviteUri) {
            IlluminedInviteLink.parse(inviteUri)?.let { invite ->
                pendingInvite = invite
                inviteStore.save(invite)
            }
        }

        // Android 12's mandatory system splash is icon-only. This brief in-app
        // handoff recreates the full iOS launch composition, including its motto.
        LaunchedEffect(Unit) {
            delay(500)
            showBrandedLaunch = false
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            if (showBrandedLaunch && session is SessionState.SignedIn) {
                BrandedLaunchScreen()
            } else {
                when (val current = session) {
                    is SessionState.SignedIn -> FormationHome(
                        userId = current.userId,
                        email = current.email,
                        inviteLink = pendingInvite,
                        onInviteConsumed = {
                            pendingInvite = null
                            inviteStore.clear()
                            ClassroomChoiceStore(context).clear()
                        },
                        onSignOut = {
                            // Reset only local onboarding/navigation; a pending server request remains intact.
                            pendingInvite = null
                            inviteStore.clear()
                            ClassroomChoiceStore(context).clear()
                            session = controller.signOut()
                        },
                        dailyFormationOpenRequest = dailyFormationOpenRequest,
                    )
                    else -> SignInScreen(
                        state = current,
                        reveal = !showBrandedLaunch,
                        onStudentCode = { code ->
                            val invite = IlluminedInviteLink(InviteRole.STUDENT, code = code)
                            inviteStore.save(invite)
                            pendingInvite = invite
                        },
                        inviteLink = pendingInvite,
                        isConfigured = BuildConfig.FIREBASE_CONFIGURED,
                        onClassroomSelected = { pendingInvite = null; inviteStore.clear() },
                        onInviteScanned = { invite -> inviteStore.save(invite); pendingInvite = invite },
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
    Box(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue), contentAlignment = Alignment.Center) {
        AuthLaunchBrand(Modifier.offset(y = (-12).dp))
    }
}

@Composable
private fun SignInScreen(
    state: SessionState,
    reveal: Boolean = true,
    onStudentCode: (String) -> Unit = {},
    onClassroomSelected: () -> Unit = {},
    onInviteScanned: (IlluminedInviteLink) -> Unit = {},
    inviteLink: IlluminedInviteLink?,
    isConfigured: Boolean,
    onSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onResetPassword: (String, () -> Unit) -> Unit,
    onClearMessage: () -> Unit,
) {
    val context = LocalContext.current
    val classroomStore = remember { ClassroomChoiceStore(context) }
    var selectedClassroom by remember { mutableStateOf(classroomStore.load()) }
    var findingClassroom by rememberSaveable { mutableStateOf(inviteLink == null && selectedClassroom == null) }
    var searchingClassroom by rememberSaveable { mutableStateOf(false) }
    var enteringCode by rememberSaveable { mutableStateOf(false) }
    var instructorEntry by rememberSaveable { mutableStateOf(false) }
    var enteringParishCode by rememberSaveable { mutableStateOf(false) }
    var parishStartupCode by rememberSaveable { mutableStateOf("") }
    var contactUnavailable by remember { mutableStateOf(false) }
    var classroomCode by rememberSaveable { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var isCreatingAccount by rememberSaveable { mutableStateOf(inviteLink != null || selectedClassroom != null) }
    var showReset by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    val isWorking = state is SessionState.Working
    val message = ((state as? SessionState.Error)?.message ?: localMessage)?.let(::localizedUserMessage)
    val invalidEmailMessage = stringResource(R.string.auth_invalid_email)
    val passwordRequiredMessage = stringResource(R.string.auth_password_required)

    LaunchedEffect(inviteLink) {
        if (inviteLink != null) { isCreatingAccount = true; findingClassroom = false; selectedClassroom = null; classroomStore.clear() }
    }

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

    val welcome = findingClassroom && !searchingClassroom && !enteringCode && !enteringParishCode
    val welcomeLinkColor = Color(0xFFD7EDFF)
    AnimatedAuthShell(reveal = reveal, welcome = welcome) {
        AuthCard(cornerRadius = 26.dp, transparent = welcome) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if(findingClassroom) {
                    if (enteringParishCode) {
                        TextButton(onClick = { enteringParishCode = false }) { Text(appT("‹ Back", "‹ Atrás")) }
                        Text(appT("Start Your Parish Classroom", "Crea el aula de tu parroquia"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                        Text(appT("First, enter the parish startup code provided by Illumined. Next, create an account or sign in, then enter your name, parish name, and city.", "Primero, introduce el código de inicio de Illumined. Después, crea una cuenta o inicia sesión y completa tu nombre, parroquia y ciudad."), color = IlluminedThemeTokens.SecondaryText)
                        OutlinedTextField(parishStartupCode, { parishStartupCode = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text(appT("Parish Startup Code", "Código de inicio de la parroquia")) }, singleLine = true)
                        Button(onClick = {
                            onInviteScanned(IlluminedInviteLink(InviteRole.PARISH, code = parishStartupCode.trim().uppercase()))
                            enteringParishCode = false; findingClassroom = false; isCreatingAccount = true; instructorEntry = true
                        }, enabled = parishStartupCode.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(appT("Continue to Account Setup", "Continuar a la cuenta")) }
                        Text(appT("Your one-use code will be verified when you create the classroom.", "Tu código de un solo uso se verificará al crear el aula."), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(IlluminedThemeTokens.Gold.copy(alpha = .35f)))
                        Text(appT("Need a parish startup code?", "¿Necesitas un código de inicio?"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                        Text(appT("Contact Illumined with your name, parish, and city to request a code. No account is needed.", "Contacta a Illumined con tu nombre, parroquia y ciudad para solicitar un código. No necesitas una cuenta."), fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText)
                        OutlinedButton(onClick = {
                            contactUnavailable = false
                            val subject = appT("Parish startup code request", "Solicitud de código de inicio parroquial")
                            val body = appT("Hello Illumined,\n\nI would like to request a parish startup code.\n\nMy name: \nParish name: \nCity: \nMy role at the parish: \n\nThank you!", "Hola Illumined:\n\nQuisiera solicitar un código de inicio para mi parroquia.\n\nMi nombre: \nNombre de la parroquia: \nCiudad: \nMi función en la parroquia: \n\n¡Gracias!")
                            val uri = android.net.Uri.parse("mailto:stephen.johnson@illumined.net?subject=" + android.net.Uri.encode(subject) + "&body=" + android.net.Uri.encode(body))
                            try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO, uri)) }
                            catch (_: android.content.ActivityNotFoundException) { contactUnavailable = true }
                            catch (_: SecurityException) { contactUnavailable = true }
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(alpha = .35f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = IlluminedThemeTokens.Blue)) {
                            Text(appT("Contact Illumined", "Contactar a Illumined"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                        androidx.compose.foundation.text.selection.SelectionContainer { Text("stephen.johnson@illumined.net", fontSize = 14.sp, color = IlluminedThemeTokens.Blue) }
                        if (contactUnavailable) Text(appT("An email app could not be opened. Copy the address above and email us from your preferred service.", "No se pudo abrir una aplicación de correo. Copia la dirección y escríbenos desde tu servicio preferido."), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                    } else if (searchingClassroom) {
                        TextButton(onClick = { searchingClassroom = false }) { Text(appT("‹ Back", "‹ Encontrar mi aula")) }
                        ClassroomSearch { room ->
                            classroomStore.save(room); selectedClassroom = room; onClassroomSelected()
                            findingClassroom = false; isCreatingAccount = true; instructorEntry = false
                        }
                    } else if (enteringCode) {
                    TextButton(onClick = { enteringCode = false }) { Text(appT("‹ Back", "‹ Encontrar mi aula")) }
                    Text(appT("Your classroom starts here.", "Tu aula comienza aquí."), fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                    Text(appT("Enter the student invitation code shared by your instructor. You’ll sign in or create an account before joining.", "Introduce el código de invitación de estudiante que te dio tu instructor. Iniciarás sesión o crearás una cuenta antes de unirte."), fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText)
                    OutlinedTextField(classroomCode, { classroomCode = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(appT("Student invitation code", "Código de invitación")) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters, imeAction = ImeAction.Done))
                    Button(onClick = {
                        onStudentCode(classroomCode.trim().uppercase())
                        findingClassroom = false; isCreatingAccount = true
                    }, enabled = classroomCode.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text(appT("Continue to Account Setup", "Continuar a la cuenta"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(appT("No code yet? Ask your instructor for an invitation. Codes are checked securely during profile setup.", "¿Aún no tienes código? Pide una invitación a tu instructor. Los códigos se verifican al configurar el perfil."), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                    } else {
                        Button(onClick = { searchingClassroom = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFE8C775), IlluminedThemeTokens.Gold, Color(0xFFA87A33))), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = .20f), RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = IlluminedThemeTokens.Ink)) {
                            Text(appT("Find My Class", "Encontrar mi aula"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f)) { ClassroomQRButton(transparent = true) { invite -> onInviteScanned(invite); findingClassroom = false; isCreatingAccount = true } }
                            Box(Modifier.width(1.dp).height(24.dp).background(welcomeLinkColor.copy(alpha = .65f)))
                            TextButton(onClick = { findingClassroom = false; isCreatingAccount = false; instructorEntry = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = welcomeLinkColor)) {
                                Text(appT("Sign In", "Iniciar sesión"), modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(onClick = { enteringCode = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = welcomeLinkColor)) { Text(appT("Enter an invitation code", "Introducir código de invitación"), fontSize = 14.sp) }
                        TextButton(onClick = {
                            instructorEntry = true; enteringParishCode = true; classroomStore.clear(); selectedClassroom = null; onClassroomSelected()
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = welcomeLinkColor)) { Text(appT("Instructor? Start a Classroom", "¿Eres instructor? Crear un aula"), fontSize = 14.sp) }
                        var approvalExpanded by remember { mutableStateOf(false) }
                        Surface(color = Color.Transparent) {
                            Column(Modifier.fillMaxWidth()) {
                                TextButton(onClick = { approvalExpanded = !approvalExpanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = welcomeLinkColor)) {
                                    Text((if (approvalExpanded) "− " else "+ ") + "Nihil Obstat & Imprimatur", fontSize = 14.sp, fontWeight = FontWeight.Normal)
                                }
                                if (approvalExpanded) {
                                    Text(appT("Applies only to the English-language lesson and quiz materials and English-language spiritual formation materials submitted for review. It does not extend to translations, later additions, or user-created content.", "Se aplica únicamente a los materiales de lecciones y cuestionarios en inglés y a los materiales de formación espiritual en inglés presentados para revisión. No se extiende a traducciones, incorporaciones posteriores ni contenido creado por los usuarios."), color = welcomeLinkColor, fontSize = 14.sp)
                                    Spacer(Modifier.height(14.dp))
                                    Text("Nihil Obstat:\nThe Reverend James M. Dunfee, MA, STL\nCensor Librorum\nSeptember 28, 2026\n\nImprimatur:\nThe Most Reverend Edward M. Lohse, JCD\nApostolic Administrator of Steubenville\nSeptember 28, 2026\n\nThe nihil obstat and imprimatur do not signify agreement with the content, opinions, or statements expressed but simply affirm that the content does not contradict faith and morals.", color = welcomeLinkColor, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    TextButton(onClick = { findingClassroom = true; onClearMessage() }, enabled = !isWorking) { Text(appT("‹ Back", "‹ Encontrar mi aula")) }
                    selectedClassroom?.let { room ->
                        Text("${room.parishName} · ${room.city}\n${room.className}", fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                        Text(appT("After profile setup, your instructor will review your request.", "Después de configurar el perfil, el instructor revisará la solicitud."), color = IlluminedThemeTokens.SecondaryText)
                    }
                    inviteLink?.let { invite ->
                        Text(stringResource(R.string.auth_invitation_saved), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                        Text(invite.title, fontSize = 14.sp, color = IlluminedThemeTokens.Blue)
                        Text(appT("We’ll apply your invitation during profile setup.", "Aplicaremos tu invitación al configurar el perfil."), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                    }
                    if(isCreatingAccount) Text(stringResource(R.string.auth_create_your_account), fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; localMessage = null; onClearMessage() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_email)) },
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
                label = { Text(stringResource(R.string.auth_password)) },
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
                            localMessage = invalidEmailMessage
                        password.isBlank() -> localMessage = passwordRequiredMessage
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
                    Text(stringResource(if (isCreatingAccount) R.string.auth_create_account else R.string.auth_sign_in), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
                    if (isCreatingAccount || instructorEntry || inviteLink != null || selectedClassroom != null) OutlinedButton(onClick = { isCreatingAccount = !isCreatingAccount; localMessage = null; onClearMessage() }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !isWorking) { Text(stringResource(if (isCreatingAccount) R.string.auth_use_existing_account else R.string.auth_create_new_account)) }
                    if (!isCreatingAccount) TextButton(onClick = { resetEmail = email; localMessage = null; onClearMessage(); showReset = true }, modifier = Modifier.fillMaxWidth(), enabled = !isWorking) { Text(stringResource(R.string.auth_forgot_password)) }
                }
            }
        }
        message?.let { AuthMessageCard(it, it == AuthErrorPresentation.ResetEmailSent) }
        if (!isConfigured) Text(stringResource(R.string.auth_developer_setup), color = Color.White)
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
                    Text(stringResource(R.string.auth_reset_password), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    Text(
                        stringResource(R.string.auth_reset_description),
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
                        label = { Text(stringResource(R.string.auth_email)) },
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
                        else Text(stringResource(R.string.auth_send_reset_email), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.auth_back_to_sign_in), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
            message?.let { AuthMessageCard(it, it == AuthErrorPresentation.ResetEmailSent) }
        }
    }
}

@Composable
private fun AuthCard(cornerRadius: androidx.compose.ui.unit.Dp = 16.dp, transparent: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = if (transparent) Color.Transparent else Color.White.copy(.94f),
        shadowElevation = if (transparent) 0.dp else 6.dp,
        border = if (transparent) null else androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f)),
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
private fun FormationHome(userId: String, email: String, inviteLink: IlluminedInviteLink?, onInviteConsumed: () -> Unit, onSignOut: () -> Unit, dailyFormationOpenRequest: DailyFormationOpenRequest? = null) {
    LaunchedEffect(userId) {
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1")
            .getHttpsCallable("refreshAssignmentProgress").call()
            .addOnFailureListener { android.util.Log.w("AssignmentProgress", "Progress refresh unavailable", it) }
    }
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
    var headerAccountOpen by rememberSaveable(userId) { mutableStateOf(false) }
    var requestInboxOpen by rememberSaveable(userId) { mutableStateOf(false) }
    val requestCounts = pendingClassroomRequests(overview?.profile)
    val messageRequest = com.illumined.app.notifications.MessageNotificationNavigation.request
    val messageProfile = overview?.profile
    val privateUnread = inboxUnreadCount(messageProfile)
    val classroomUnread = classroomUnreadCount(messageProfile)
    if (messageRequest != null && messageProfile != null &&
        com.illumined.app.notifications.canOpenMessageRequest(messageRequest, userId,
            messageProfile.activeClassIds, messageProfile.removedClassIds, messageProfile.inactiveClassIds)) {
        if(messageRequest.refreshments) RefreshmentSignup(messageRequest.classId, userId, initiallyOpen = true) {
            com.illumined.app.notifications.MessageNotificationNavigation.request = null
        } else androidx.compose.ui.window.Dialog(
            onDismissRequest = { com.illumined.app.notifications.MessageNotificationNavigation.request = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            ChatPage(userId, messageProfile.copy(activeClassId = messageRequest.classId), initialInbox = messageRequest.privateMessage) {
                com.illumined.app.notifications.MessageNotificationNavigation.request = null
            }
        }
    }
    val requestCount = if(requestCounts.values.any { it < 0 }) -1 else requestCounts.values.sum()
    var headerPhotoRefresh by remember(userId) { mutableIntStateOf(0) }
    var dailyFormation by remember { mutableStateOf<DailyFormationEntry?>(null) }
    var startupFormationChecked by remember(userId) { mutableStateOf(false) }
    var loadedDailyFormationKey by remember { mutableStateOf("") }
    var dailyFormationLoadGeneration by remember { mutableIntStateOf(0) }
    val walkthrough = remember(userId) { InstructorWalkthroughState() }
    LaunchedEffect(overview?.profile?.isConfigured, overview?.profile?.isInstructor) {
        if (overview?.profile?.isInstructor == false) walkthrough.stop()
        overview?.profile?.takeIf { it.isConfigured }?.let { walkthrough.prepare(notificationContext, userId, it.isInstructor, it.isAdmin) }
    }
    LaunchedEffect(walkthrough.page, walkthrough.active) {
        if (walkthrough.active) {
            selectedAssignmentId = null
            selectedSectionReset += 1
            selectedSection = FormationSection.entries.first { it.name.lowercase() == walkthrough.page }
        }
    }
    val walkthroughBlocking = !walkthrough.checked || walkthrough.loadingRemote || walkthrough.invitation || walkthrough.active

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
            onError = { error = appT("We couldn’t load your formation. Please try again.", "No pudimos cargar tu formación. Inténtalo de nuevo.") },
        )
        onDispose { listener.remove() }
    }

    val setupPhotoPreferences = notificationContext.getSharedPreferences("setup-photos", 0)
    val setupPhotoProfile = overview?.profile
    val pendingPhotoClassroom = ClassroomChoiceStore(notificationContext).load()
    if (setupPhotoProfile?.isConfigured == true && setupPhotoPreferences.getBoolean(userId, false) &&
        (pendingPhotoClassroom == null || pendingPhotoClassroom.classId in setupPhotoProfile.classIds)) {
        SetupPhotosPage(userId, setupPhotoProfile.selectedClassId, setupPhotoProfile.isInstructor) {
            setupPhotoPreferences.edit().remove(userId).apply()
            onInviteConsumed()
            profileReload += 1
        }
        return
    }

    if (OverviewPresentation.errorPresentation(overview != null, error) == OverviewErrorPresentation.Alert) {
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(appT("Dashboard Error", "Error de la página de inicio")) },
            text = { Text(localizedUserMessage(error.orEmpty())) },
            confirmButton = { TextButton(onClick = { error = null }) { Text(appT("OK", "Aceptar")) } },
        )
    }

    val notificationClassId = overview?.profile?.selectedClassId.orEmpty()
    LaunchedEffect(userId, notificationClassId, overview?.profile?.isConfigured) {
        val permissionGranted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(notificationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (overview?.profile?.isConfigured == true && permissionGranted) notificationRegistrar.register(notificationClassId, {}, {})
    }

    val dailyProfile = overview?.profile
    val dailyKey = dailyProfile?.let { "${it.userId}:${it.selectedClassId}" }.orEmpty()
    LaunchedEffect(dailyKey) {
        val profile = dailyProfile ?: return@LaunchedEffect
        if (dailyKey.isNotBlank() && loadedDailyFormationKey != dailyKey) {
            loadedDailyFormationKey = dailyKey
            dailyFormationLoadGeneration += 1
            val generation = dailyFormationLoadGeneration
            repository.loadDailyFormation(
                profile,
                onSuccess = { if (generation == dailyFormationLoadGeneration) dailyFormation = it; startupFormationChecked = true },
                onError = { startupFormationChecked = true },
            )
        }
    }

    LaunchedEffect(dailyFormationOpenRequest?.id, dailyProfile?.userId) {
        val profile = dailyProfile ?: return@LaunchedEffect
        val request = dailyFormationOpenRequest ?: return@LaunchedEffect
        dailyFormationLoadGeneration += 1
        val generation = dailyFormationLoadGeneration
        repository.loadDailyFormation(
            profile = profile,
            force = true,
            requestedClassId = request.classId,
            requestedDate = request.date,
            onSuccess = { if (generation == dailyFormationLoadGeneration) dailyFormation = it },
            onError = { /* supplemental content */ },
        )
    }

    dailyFormation?.takeUnless { walkthroughBlocking }?.let { entry ->
        DailyFormationDialog(entry = entry) {
            dailyFormation = null
            dailyProfile?.let { repository.dismissDailyFormation(it, entry) }
        }
    }

    InstructorStartupCard(dailyProfile, startupFormationChecked && dailyFormation == null && !walkthroughBlocking)
    val nextScheduleDay = overview?.let { ClassScheduleSelection.nextDay(it.schedule) }
    val selectedAssignment = overview?.assignments?.firstOrNull { it.id == selectedAssignmentId }
    LaunchedEffect(selectedAssignmentId, overview?.assignments) {
        if (overview != null && selectedAssignmentId != null && selectedAssignment == null) {
            selectedAssignmentId = null
        }
    }

    val requestedClassroom = remember(userId, profileReload) { ClassroomChoiceStore(notificationContext).load() }
    if (overview?.profile?.isConfigured == false || (overview != null && requestedClassroom != null && requestedClassroom.classId !in overview!!.profile.classIds)) {
        Column(Modifier.fillMaxSize().background(IlluminedThemeTokens.Cream)) {
            IlluminedBrandHeader()
            ProfileSetupExperience(
                inviteLink = inviteLink,
                onComplete = { onInviteConsumed(); profileReload += 1 },
                onSignOut = onSignOut,
            )
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
                        completionError = appT("We couldn’t save your progress. Please try again.", "No pudimos guardar tu progreso. Inténtalo de nuevo.")
                    },
                )
            },
            onSetReadingCompleted = { reading, completed ->
                completionWorking = true
                completionError = null
                repository.setReadingCompleted(activeProfile, assignment, reading, completed, completedReadingIds, {
                    val readingKey = "${assignment.id}__reading__${reading.id}"
                    val updatedRecords = overview?.assignmentCompletions.orEmpty().filterNot { it.assignmentId == readingKey } + listOf(
                        AssignmentCompletion(readingKey, userId, overview?.profile?.displayName.orEmpty(), completed, assignment.id, reading.id, reading.title, "reading"),
                    )
                    overview = overview?.copy(
                        assignmentCompletions = updatedRecords,
                    )
                    completionWorking = false
                }, {
                    completionWorking = false
                    completionError = appT("We couldn’t save your reading progress. Please try again.", "No pudimos guardar el progreso de tu lectura. Inténtalo de nuevo.")
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

    androidx.compose.runtime.CompositionLocalProvider(LocalInstructorWalkthrough provides walkthrough) {
    BackHandler(enabled = headerAccountOpen) { headerAccountOpen = false; headerPhotoRefresh++ }
    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream),
                radius = 1600f,
            ),
        ),
    ) {
        IlluminedBrandHeader(userId, headerPhotoRefresh, requestCount, { requestInboxOpen=true; headerAccountOpen=false }) { headerAccountOpen = true; requestInboxOpen=false }
        if ((privateUnread > 0 || classroomUnread > 0) && messageProfile != null) {
            TextButton(onClick = {
                com.illumined.app.notifications.MessageNotificationNavigation.request =
                    com.illumined.app.notifications.MessageOpenRequest(messageProfile.selectedClassId, userId, privateUnread > 0)
            }) { Text(appT("Unread messages", "Mensajes sin leer") + " · ${privateUnread + classroomUnread}") }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().walkthroughAnchor("viewport")) {
            if (requestInboxOpen) ClassroomRequestInbox(requestCounts) { requestInboxOpen=false }
            else if (headerAccountOpen) AccountPage(email, overview?.profile, onSignOut) { headerAccountOpen = false; headerPhotoRefresh++ }
            else key(selectedSection, selectedSectionReset) { when (selectedSection) {
                FormationSection.Home -> HomeSection(
                    userId = userId,
                    email = email,
                    repository = repository,
                    overview = overview,
                    error = error,
                    nextScheduleDay = nextScheduleDay,
                    onOpenLessons = { selectedSection = FormationSection.Lessons; walkthrough.selected("lessons") },
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
                FormationSection.Discussion -> Box(Modifier.fillMaxSize().walkthroughAnchor("discussion")) { DiscussionExperience(
                    userId = userId,
                    profile = overview?.profile,
                    prompts = overview?.discussionPrompts.orEmpty(),
                    assignments = overview?.assignments.orEmpty(),
                    assignmentCompletions = overview?.assignmentCompletions.orEmpty(),
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
                }
                FormationSection.Formation -> SpiritualFormationExperience(
                    profile = overview?.profile,
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
            headerAccountOpen = false
            requestInboxOpen = false
            headerPhotoRefresh++
            if (selectedSection == it) selectedSectionReset += 1 else selectedSection = it
            walkthrough.selected(it.name.lowercase())
        }
    }
    InstructorWalkthroughOverlay(walkthrough)
    }
}

@Composable
internal fun DailyFormationDialog(entry: DailyFormationEntry, onDismiss: () -> Unit) {
    val colors = dailyFormationColors(entry.colorCode)
    val buttonContentColor = dailyFormationButtonContentColor(entry.colorCode)
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.first,
        titleContentColor = colors.third,
        textContentColor = colors.third,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(dailyFormationTypeLabel(entry.type), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(entry.title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.width(86.dp).height(3.dp).background(colors.second))
            }
        },
        text = {
            Text(
                text = entry.details,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                fontSize = 18.sp,
                lineHeight = 27.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.second,
                    contentColor = buttonContentColor,
                ),
            ) {
                Text(appT("Dismiss for Today", "Cerrar por hoy"))
            }
        },
    )
}

internal fun appT(english: String, spanish: String): String =
    if (java.util.Locale.getDefault().language == "es") spanish else english

private fun dailyFormationTypeLabel(type: String): String = when (type.lowercase()) {
    "fact" -> appT("FACT OF THE DAY", "DATO DEL DÍA")
    "saint" -> appT("SAINT OF THE DAY", "SANTO DEL DÍA")
    else -> appT("LITURGICAL NOTE", "NOTA LITÚRGICA")
}

private fun dailyFormationButtonContentColor(code: String): Color = when (code.uppercase()) {
    "RED" -> Color(0xFF7A0D12)
    "PURPLE" -> Color(0xFF481F61)
    "ROSE" -> Color.White
    else -> Color.Black
}

private fun dailyFormationColors(code: String): Triple<Color, Color, Color> = when (code.uppercase()) {
    "WHITE" -> Triple(Color.White, Color(0xFFC99B47), Color.Black)
    "GOLD" -> Triple(Color(0xFFF9EFCF), Color(0xFFA66E14), Color.Black)
    "RED" -> Triple(Color(0xFF7A0D12), Color.White, Color.White)
    "PURPLE" -> Triple(Color(0xFF481F61), Color(0xFFE3C4F2), Color.White)
    "ROSE" -> Triple(Color(0xFFE092A3), Color(0xFF661526), Color.Black)
    else -> Triple(Color(0xFF1A5C38), Color(0xFFE8C97A), Color.White)
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
            Text(stringResource(R.string.nav_discussion), color = IlluminedThemeTokens.Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            Text(appT("Discuss, reflect, and pray with your OCIA class", "Conversa, reflexiona y ora con tu clase de OCIA"), color = IlluminedThemeTokens.SecondaryText)
            Spacer(modifier = Modifier.height(12.dp))
        }
        when {
            error != null -> item { Text(localizedUserMessage(error), color = Color(0xFFFFB4AB)) }
            overview == null -> item { LoadingFormation() }
            else -> {
                item {
                    Text(appT("DISCUSSIONS", "DISCUSIONES"), color = IlluminedThemeTokens.Gold, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                }
                if (overview.discussionPrompts.isEmpty()) {
                    item { FormationCard(appT("DISCUSSIONS", "DISCUSIONES"), appT("No active prompts", "No hay temas activos"), appT("Your instructor’s prompts will appear here.", "Los temas de tu instructor aparecerán aquí.")) }
                } else {
                    items(overview.discussionPrompts, key = { "prompt-${it.id}" }) { prompt ->
                        FormationCard(
                            eyebrow = if (prompt.requiredForAssignment) appT("REQUIRED DISCUSSION", "DISCUSIÓN OBLIGATORIA") else appT("DISCUSSION", "DISCUSIÓN"),
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
    nextScheduleDay: ClassScheduleDay?,
    onOpenLessons: () -> Unit,
    onOpenAssignment: (Assignment) -> Unit,
    onPrayerPosted: () -> Unit,
    onRetry: () -> Unit,
) {
    val usesStackedTracker = ResponsivePresentation.usesStackedTracker(LocalDensity.current.fontScale)
    val announcementRepository = remember { InstructorRepository() }
    var announcements by remember { mutableStateOf(emptyList<com.illumined.app.data.Announcement>()) }
    var selectedAnnouncement by remember { mutableStateOf<com.illumined.app.data.Announcement?>(null) }
    var prayerComposer by rememberSaveable { mutableStateOf(false) }
    var selectedPrayerId by rememberSaveable { mutableStateOf<String?>(null) }
    var assignmentsOpen by rememberSaveable { mutableStateOf(false) }
    var prayerTitle by rememberSaveable { mutableStateOf("") }
    var prayerDetails by rememberSaveable { mutableStateOf("") }
    var prayerWorking by remember { mutableStateOf(false) }
    var prayerError by remember { mutableStateOf<String?>(null) }
    val classId = overview?.profile?.selectedClassId.orEmpty()
    val selectedPrayer = overview?.prayerRequests?.firstOrNull { it.id == selectedPrayerId }

    BackHandler(enabled = selectedAnnouncement != null || selectedPrayerId != null || prayerComposer || assignmentsOpen) {
        when {
            selectedAnnouncement != null -> selectedAnnouncement = null
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
        announcements = emptyList()
        val listener = if (classId.isNotBlank()) announcementRepository.listenAnnouncements(
            classId, { announcements = it.filter { item -> item.isActive } }, {},
        ) else null
        onDispose { listener?.remove() }
    }
    selectedAnnouncement?.let { announcement ->
        AnnouncementDetail(announcement) { selectedAnnouncement = null }
        return
    }
    selectedPrayer?.let { request ->
        PrayerRequestDetail(request, userId, repository) { selectedPrayerId = null }
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
                    prayerError = localizedUserMessage(throwable.message ?: appT("Prayer request could not be posted.", "No se pudo publicar la petición de oración."))
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
                Box(Modifier.walkthroughAnchor("welcome")) { HomeWelcomeCard(overview.profile) }
                Spacer(modifier = Modifier.height(14.dp))
                NextScheduledDayCard(nextScheduleDay, overview.profile.selectedClassId, userId)
                Spacer(modifier = Modifier.height(14.dp))
                HomeAnnouncementsCard(
                    announcements = announcements.take(3),
                    onOpen = { selectedAnnouncement = it },
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(Modifier.walkthroughAnchor("guides")) {
                    RitePreparationDashboard(classId = classId, userId = userId)
                }
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
    val noClassAssigned = stringResource(R.string.home_no_class_assigned)
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
        color = Color.White.copy(.94f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SavedProfilePhoto("classroom", profile.selectedClassId)
            Text(stringResource(R.string.home_welcome, profile.displayName), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeSymbol(HomeSymbolKind.ClassMembers, IlluminedThemeTokens.SecondaryText, Modifier.size(22.dp))
                Text(profile.selectedClassId.ifBlank { noClassAssigned }, color = IlluminedThemeTokens.SecondaryText)
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
        modifier = Modifier.walkthroughAnchor("prayers").fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
        color = Color.White.copy(.94f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_prayer_requests), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.home_prayer_invitation), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
            }
            Button(
                onClick = onNewRequest,
                enabled = canPost,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){InstructorSymbol(InstructorSymbolKind.PlusCircle,Color.White,Modifier.size(18.dp));Text(stringResource(R.string.home_new_prayer_request),fontSize=15.sp,fontWeight=FontWeight.SemiBold)} }
            if (requests.isEmpty()) {
                Text(
                    stringResource(R.string.home_no_prayer_requests),
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
                                if (cleaned.isEmpty()) stringResource(R.string.home_no_additional_details) else cleaned.let { if (it.length <= 50) it else "${it.take(50)}..." },
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
private fun NextScheduledDayCard(day: ClassScheduleDay?, classId: String, userId: String) {
    Surface(
        modifier = Modifier.walkthroughAnchor("schedule").fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
        color = Color.White.copy(.94f), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(44.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                HomeSymbol(HomeSymbolKind.CalendarBadgeClock, IlluminedThemeTokens.Gold, Modifier.size(27.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_upcoming), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                if (day == null) {
                    Text(stringResource(R.string.home_no_class_scheduled), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                } else {
                    day.sessions.forEach { session ->
                        Text(
                            session.topic,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IlluminedThemeTokens.Blue,
                        )
                    }
                    Text(
                        java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(day.date),
                        fontSize = 15.sp,
                        color = IlluminedThemeTokens.SecondaryText,
                    )
                }
                RefreshmentSignup(classId, userId)
            }
        }
    }
}

@Composable
private fun HomeAnnouncementsCard(
    announcements: List<com.illumined.app.data.Announcement>,
    onOpen: (com.illumined.app.data.Announcement) -> Unit,
) {
    Surface(modifier = Modifier.walkthroughAnchor("announcements").fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), color = Color.White.copy(.94f), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.home_announcements), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    Text(stringResource(R.string.home_instructor_updates), fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                }
                HomeSymbol(HomeSymbolKind.Megaphone, IlluminedThemeTokens.Gold, Modifier.size(23.dp))
            }
            if (announcements.isEmpty()) Text(stringResource(R.string.home_no_announcements), fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText, modifier = Modifier.padding(vertical = 8.dp))
            else announcements.forEach { announcement ->
                Surface(
                    onClick = { onOpen(announcement) },
                    color = IlluminedThemeTokens.Gold.copy(.09f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(announcement.title, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue, maxLines = 2)
                                Spacer(Modifier.width(10.dp)); Text(announcement.displayTimestamp?.toDate()?.let { java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it) }.orEmpty(), fontSize = 11.sp, color = IlluminedThemeTokens.SecondaryText)
                            }
                            Text(announcement.message, fontSize = 14.sp, color = IlluminedThemeTokens.Ink, maxLines = 3)
                        }
                        Spacer(Modifier.width(10.dp))
                        LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(10.dp, 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementDetail(
    announcement: com.illumined.app.data.Announcement,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f))
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Spacer(Modifier.weight(1f))
            Text(appT("Announcement", "Anuncio"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
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
                Text(announcement.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IlluminedThemeTokens.Ink)
                announcement.displayTimestamp?.toDate()?.let { date ->
                    Text(
                        java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG, java.text.DateFormat.SHORT).format(date),
                        fontSize = 13.sp,
                        color = IlluminedThemeTokens.SecondaryText,
                    )
                }
                androidx.compose.material3.HorizontalDivider()
                Text(
                    announcement.message,
                    fontSize = 17.sp,
                    lineHeight = 27.sp,
                    color = IlluminedThemeTokens.Ink,
                )
            }
        }
    }
}

@Composable
private fun HomeAssignmentsCard(assignments: List<Assignment>, completedIds: Set<String>, onClick: () -> Unit) {
    val visible = homeAssignmentPreview(assignments)
    val remaining = remainingHomeAssignmentCount(assignments)
    Surface(onClick = onClick, modifier = Modifier.walkthroughAnchor("assignments").fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), color = Color.White.copy(.94f), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.home_assignments), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    Text(if (visible.isEmpty()) stringResource(R.string.home_no_active_assignments) else pluralStringResource(R.plurals.home_active_assignments, assignments.size, assignments.size), fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                }
                HomeSymbol(HomeSymbolKind.Checklist, IlluminedThemeTokens.Gold, Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); HomeSymbol(HomeSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
            }
            if (visible.isEmpty()) Text(stringResource(R.string.home_assignments_prompt), fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText, modifier = Modifier.padding(vertical = 8.dp))
            else visible.forEach { assignment -> AssignmentSummaryRow(assignment, assignment.id in completedIds) }
            if (remaining > 0) Text(pluralStringResource(R.plurals.home_more_assignments, remaining, remaining), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.SecondaryText)
        }
    }
}

@Composable
private fun AssignmentSummaryRow(assignment: Assignment, completed: Boolean) {
    val dueDate = assignment.dueAt?.toDate()
    val dueText = if (dueDate == null) {
        stringResource(R.string.home_due_not_set)
    } else {
        stringResource(R.string.home_due, java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(dueDate))
    }
    Row(Modifier.fillMaxWidth().background(IlluminedThemeTokens.Blue.copy(.07f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
        LessonSymbol(if (completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff, if (completed) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText, Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(assignment.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue, maxLines = 2)
            Text(dueText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.SecondaryText)
            val contentLabel = when {
                assignment.readings.isNotEmpty() -> pluralStringResource(R.plurals.home_readings, assignment.readings.size, assignment.readings.size)
                assignment.lessonLinks.isNotEmpty() -> assignment.lessonLinks.first().lessonTitle.ifBlank { assignment.lessonLinks.first().lessonId }
                else -> null
            }
            contentLabel?.let {
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
        item { TextButton(onClick = onBack) { Text(appT("‹ Back", "‹ Atrás")) } }
        item { AssignmentDetailCard { Text(appT("Assignments", "Tareas"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(appT("Select an assignment to open the full details, readings, lesson links, and completion check.", "Selecciona una tarea para ver todos los detalles, las lecturas, los enlaces a las lecciones y el estado de finalización."), fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText) } }
        if (assignments.isEmpty()) item { FormationCard(appT("ASSIGNMENTS", "TAREAS"), appT("No Assignments", "No hay tareas"), appT("Your instructor has not posted active assignments yet.", "Tu instructor todavía no ha publicado tareas activas.")) }
        else items(assignments, key = { it.id }) { assignment ->
            Surface(onClick = { onOpen(assignment) }, modifier = Modifier.border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
                    val completed = assignment.id in completedIds
                    LessonSymbol(
                        if (completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff,
                        if (completed) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,
                        Modifier.size(22.dp).semantics { contentDescription = if (completed) appT("Completed", "Completada") else appT("Not completed", "No completada") },
                    )
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(assignment.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink, maxLines = 2)
                        Text(assignment.dueAt?.toDate()?.let { appT("Due ${java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it)}", "Fecha límite: ${java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it)}") } ?: appT("Due date not set", "Sin fecha límite"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (assignment.readings.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                                LessonSymbol(LessonSymbolKind.DocumentText, IlluminedThemeTokens.Gold, Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)); Text(if (assignment.readings.size == 1) appT("1 reading", "1 lectura") else appT("${assignment.readings.size} readings", "${assignment.readings.size} lecturas"), fontSize = 12.sp, color = IlluminedThemeTokens.Gold)
                            }
                            if (assignment.lessonLinks.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                                LessonSymbol(LessonSymbolKind.BookClosed, IlluminedThemeTokens.Gold, Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)); Text(if (assignment.lessonLinks.size == 1) appT("1 lesson", "1 lección") else appT("${assignment.lessonLinks.size} lessons", "${assignment.lessonLinks.size} lecciones"), fontSize = 12.sp, color = IlluminedThemeTokens.Gold)
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
private fun PrayerRequestDetail(request: PrayerRequest, currentUserId: String, repository: FormationRepository, onBack: () -> Unit) {
    var reactionWorking by remember { mutableStateOf(false) }
    var reactionError by remember { mutableStateOf<String?>(null) }
    val reactionOptions = listOf(
        Triple("praying", "🙏", appT("Praying", "Orando")),
        Triple("with_you", "❤️", appT("With you", "Contigo")),
        Triple("amen", "🕊️", appT("Amen", "Amén")),
    )
    Column(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f))
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(appT("‹ Back", "‹ Atrás")) }
            Spacer(Modifier.weight(1f))
            Text(appT("Prayer Request", "Petición de oración"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
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
                    cleanedDetails.ifEmpty { appT("No additional details were added.", "No se añadieron detalles adicionales.") },
                    fontSize = if (cleanedDetails.isEmpty()) 16.sp else 17.sp,
                    lineHeight = if (cleanedDetails.isEmpty()) 22.sp else 27.sp,
                    color = if (cleanedDetails.isEmpty()) IlluminedThemeTokens.SecondaryText else IlluminedThemeTokens.Ink,
                )
                androidx.compose.material3.HorizontalDivider()
                Text(appT("Prayer acknowledgements", "Respuestas de oración"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reactionOptions.forEach { (value, emoji, label) ->
                        val selected = request.reactions[currentUserId] == value
                        val count = request.reactions.values.count { it == value }
                        OutlinedButton(
                            onClick = {
                                reactionWorking = true
                                reactionError = null
                                repository.setPrayerReaction(request, if (selected) null else value, {
                                    reactionWorking = false
                                }, { problem ->
                                    reactionWorking = false
                                    reactionError = problem.message ?: appT("Prayer response could not be saved.", "No se pudo guardar la respuesta de oración.")
                                })
                            },
                            enabled = request.requesterId != currentUserId && !reactionWorking,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selected) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Ink,
                            ),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji)
                                Text("$label${if (count > 0) " · $count" else ""}", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
                if (request.requesterId == currentUserId) {
                    Text(
                        if (request.reactions.size == 1) {
                            appT("Classmates can acknowledge this request. 1 response received.", "Los compañeros pueden reconocer esta petición. Se recibió 1 respuesta.")
                        } else {
                            appT("Classmates can acknowledge this request. ${request.reactions.size} responses received.", "Los compañeros pueden reconocer esta petición. Se recibieron ${request.reactions.size} respuestas.")
                        },
                        fontSize = 12.sp,
                        color = IlluminedThemeTokens.SecondaryText,
                    )
                }
                reactionError?.let { Text(localizedUserMessage(it), fontSize = 12.sp, color = Color.Red) }
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
                    Text(appT("‹ Back", "‹ Atrás"), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onPost, enabled = title.isNotBlank() && !working) {
                    Text(if (working) appT("Posting...", "Publicando...") else appT("Post", "Publicar"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            AssignmentDetailCard {
                Text(appT("New Prayer Request", "Nueva petición de oración"), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                Spacer(Modifier.height(10.dp))
                Text(
                    appT("Share a request with your class so they can pray with you.", "Comparte una petición con tu clase para que puedan orar contigo."),
                    fontSize = 15.sp,
                    color = IlluminedThemeTokens.SecondaryText,
                )
            }
        }
        item {
            AssignmentDetailCard {
                Text(appT("Prayer Request", "Petición de oración"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appT("Title", "Título")) },
                    singleLine = true,
                    enabled = !working,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = onDetails,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appT("Optional details", "Detalles opcionales")) },
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
                        appT("Requests stay visible for 3 days and then expire from the board.", "Las peticiones permanecen visibles durante 3 días y luego desaparecen del tablero."),
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
                        Text(localizedUserMessage(message), fontSize = 15.sp, color = Color.Red, modifier = Modifier.weight(1f))
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
            Text(stringResource(R.string.nav_lessons), color = IlluminedThemeTokens.Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            Text(appT("Your assignments and formation readings", "Tus tareas y lecturas de formación"), color = IlluminedThemeTokens.SecondaryText)
            Spacer(modifier = Modifier.height(12.dp))
        }
        when {
            error != null -> item { Text(localizedUserMessage(error), color = Color(0xFFFFB4AB)) }
            overview == null -> item { LoadingFormation() }
            overview.assignments.isEmpty() -> item {
                FormationCard(appT("ASSIGNMENTS", "TAREAS"), appT("Nothing assigned yet", "Todavía no hay tareas"), appT("New lessons will appear here.", "Las nuevas lecciones aparecerán aquí."))
            }
            else -> items(overview.assignments, key = { it.id }) { assignment ->
                val completed = assignment.id in overview.completedAssignmentIds
                FormationCard(
                    eyebrow = if (completed) appT("COMPLETED", "COMPLETADA") else appT("TO DO", "PENDIENTE"),
                    title = assignment.title,
                    detail = assignment.dueAt?.toDate()?.let {
                        buildString {
                            append(appT("Due ${java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it)}", "Fecha límite: ${java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it)}"))
                            if (assignment.lessonLinks.isNotEmpty()) append("  •  ${if (assignment.lessonLinks.size == 1) appT("1 lesson", "1 lección") else appT("${assignment.lessonLinks.size} lessons", "${assignment.lessonLinks.size} lecciones")}")
                            if (assignment.readings.isNotEmpty()) append("  •  ${if (assignment.readings.size == 1) appT("1 reading", "1 lectura") else appT("${assignment.readings.size} readings", "${assignment.readings.size} lecturas")}")
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
            Text(stringResource(R.string.nav_formation), color = IlluminedThemeTokens.Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            Text(appT("Your spiritual formation", "Tu formación espiritual"), color = IlluminedThemeTokens.SecondaryText)
            Spacer(modifier = Modifier.height(12.dp))
        }
        when {
            error != null -> item { Text(localizedUserMessage(error), color = Color(0xFFFFB4AB)) }
            overview == null -> item { LoadingFormation() }
            overview.schedule.isEmpty() -> item {
                FormationCard(appT("SCHEDULE", "HORARIO"), appT("No sessions scheduled", "No hay sesiones programadas"), appT("Check back with your instructor.", "Consulta de nuevo con tu instructor."))
            }
            else -> items(overview.schedule, key = { it.id }) { session ->
                FormationCard(
                    eyebrow = session.date?.toDate()?.let {
                        java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(it).uppercase()
                    } ?: appT("SESSION", "SESIÓN"),
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
        Text(stringResource(R.string.nav_more), color = IlluminedThemeTokens.Ink, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))
        FormationCard(
            eyebrow = appT("PARTICIPANT", "PARTICIPANTE"),
            title = overview?.profile?.displayName ?: appT("Illumined participant", "Participante de Illumined"),
            detail = email,
        )
        Spacer(modifier = Modifier.height(14.dp))
        FormationCard(
            eyebrow = appT("OCIA CLASS", "CLASE DE OCIA"),
            title = overview?.profile?.classIds?.joinToString().orEmpty().ifBlank { appT("Not assigned", "Sin asignar") },
            detail = appT("Your parish formation group", "Tu grupo de formación parroquial"),
        )
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text(appT("Sign out", "Cerrar sesión")) }
    }
}

@Composable
private fun LoadingFormation() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(stringResource(R.string.auth_loading_journey), color = IlluminedThemeTokens.SecondaryText)
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
                Text(stringResource(R.string.formation_unavailable), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                Text(message, color = IlluminedThemeTokens.SecondaryText, textAlign = TextAlign.Center, lineHeight = 21.sp)
                Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.action_try_again)) }
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
                            .walkthroughAnchor("nav-" + section.name.lowercase())
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
                                stringResource(section.labelResource),
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
internal fun TrackerStat(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
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
    val assignmentPrompts = prompts.filter { it.assignmentId == assignment.id }.ifEmpty {
        prompts.filter { prompt -> prompt.assignmentId.isBlank() && assignment.lessonLinks.any { it.lessonId == prompt.lessonId } }
    }
    val discussionRepository = remember { DiscussionRepository() }
    var completedPromptIds by remember(assignment.id) { mutableStateOf(emptySet<String>()) }
    var selectedReading by remember(assignment.id) { mutableStateOf<AssignmentReading?>(null) }
    var selectedLesson by remember(assignment.id) { mutableStateOf<CatechismLesson?>(null) }
    var selectedDiscussion by remember(assignment.id) { mutableStateOf<com.illumined.app.data.DiscussionPrompt?>(null) }
    val readingsCompleted = assignment.readings.all { it.id in completedReadingIds }
    val lessonsCompleted = assignment.lessonLinks.all { it.lessonId in completedLessonIds }
    val prerequisitesCompleted = readingsCompleted && lessonsCompleted
    val requiredPrompts = assignmentPrompts.filter { it.requiredForAssignment }
    val totalActivities = assignment.readings.size + assignment.lessonLinks.size + requiredPrompts.size
    val completedActivities = assignment.readings.count { it.id in completedReadingIds } + assignment.lessonLinks.count { it.lessonId in completedLessonIds } + requiredPrompts.count { it.id in completedPromptIds }
    val nextReading = assignment.readings.firstOrNull { it.id !in completedReadingIds }
    val nextLesson = assignment.lessonLinks.firstOrNull { it.lessonId !in completedLessonIds }
    val nextDiscussion = requiredPrompts.firstOrNull { it.id !in completedPromptIds }
    var continuationError by remember(assignment.id) { mutableStateOf<String?>(null) }
    DisposableEffect(profile?.selectedClassId, userId) {
        val listener = profile?.selectedClassId?.takeIf { it.isNotBlank() }?.let { classId ->
            discussionRepository.listenParticipation(classId, userId, { completedPromptIds = it }, {})
        }
        onDispose { listener?.remove() }
    }
    BackHandler {
        if (!isWorking) {
            when {
                selectedDiscussion != null -> selectedDiscussion = null
                selectedLesson != null -> selectedLesson = null
                selectedReading != null -> selectedReading = null
                else -> onBack()
            }
        }
    }
    selectedDiscussion?.let { discussion ->
        DiscussionBoard(
            prompt = discussion,
            userId = userId,
            profile = profile,
            linkedAssignments = listOf(assignment),
            onCompleteAssignment = onCompleteLinkedAssignment,
            onBack = { selectedDiscussion = null },
        )
        return
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
        TextButton(onClick = onBack, enabled = !isWorking) { Text(appT("‹ Back", "‹ Atrás")) }
        AssignmentDetailCard {
            Text(assignment.title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
            Text(
                assignment.dueAt?.toDate()?.let {
                    appT("Due ${java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(it)}", "Fecha límite: ${java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(it)}")
                } ?: appT("Due date not set", "Sin fecha límite"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = IlluminedThemeTokens.SecondaryText,
            )
        }
        if (totalActivities > 0) AssignmentDetailCard {
            Text(appT("$completedActivities of $totalActivities activities completed", "$completedActivities de $totalActivities actividades completadas"), fontWeight = FontWeight.SemiBold)
            if (completedActivities < totalActivities) Button(onClick = {
                continuationError = null
                when {
                    nextReading != null -> selectedReading = nextReading
                    nextLesson != null -> {
                        selectedLesson = linkedLessons.firstOrNull { it.id == nextLesson.lessonId }
                        if (selectedLesson == null) continuationError = appT("The next lesson is unavailable. Please contact your instructor.", "La siguiente lección no está disponible. Contacta a tu instructor.")
                    }
                    nextDiscussion != null -> selectedDiscussion = nextDiscussion
                }
            }, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                Text(appT("Continue Assignment", "Continuar tarea"))
            }
            continuationError?.let { Text(it, color = IlluminedThemeTokens.SecondaryText) }
        }
        if (assignment.instructions.isNotBlank()) AssignmentDetailCard {
            Text(appT("Instructions", "Instrucciones"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(assignment.instructions, fontSize = 16.sp, lineHeight = 23.sp)
        }
        if (assignment.readings.isNotEmpty()) AssignmentDetailCard {
            Text(appT("Step 1 · Assigned Readings", "Paso 1 · Lecturas asignadas"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                            AssignmentItemProgressLabel(reading.id in completedReadingIds)
                        }
                        LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
                    }
                }
            }
        }
        if (linkedLessons.isNotEmpty()) AssignmentDetailCard {
            Text(appT("Step ${if (assignment.readings.isNotEmpty()) 2 else 1} · Lessons", "Paso ${if (assignment.readings.isNotEmpty()) 2 else 1} · Lecciones"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                            AssignmentItemProgressLabel(lesson.id in completedLessonIds)
                        }
                        LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
                    }
                }
            }
        }
        assignmentPrompts.forEach { discussion ->
            val discussionCompleted = discussion.id in completedPromptIds
            AssignmentDetailCard {
                val step = (if (assignment.readings.isNotEmpty()) 1 else 0) + (if (linkedLessons.isNotEmpty()) 1 else 0) + 1
                Text(appT("Step $step · Discussion", "Paso $step · Discusión"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Surface(
                    onClick = { selectedDiscussion = discussion },
                    color = Color.White.copy(.72f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        LessonSymbol(if (discussionCompleted) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff, if (discussionCompleted) Color(0xFF2E7D32) else IlluminedThemeTokens.Blue, Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(discussion.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (discussionCompleted) appT("Completed", "Completada") else appT("Ready to discuss", "Lista para participar"), fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                        }
                        LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
                    }
                }
            }
        }
        error?.let { Text(localizedUserMessage(it), color = Color.Red, modifier = Modifier.padding(horizontal = 4.dp)) }
        if (assignmentPrompts.none { it.requiredForAssignment } && assignment.readings.isEmpty() && assignment.lessonLinks.isEmpty()) Button(
            onClick = onComplete,
            enabled = !isWorking && prerequisitesCompleted,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            when {
                isWorking -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                isComplete -> { LessonSymbol(LessonSymbolKind.CheckCircle, Color.White, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(appT("Mark Assignment Incomplete", "Marcar tarea como incompleta"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                else -> { LessonSymbol(LessonSymbolKind.RadioOff, Color.White, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(appT("Mark Assignment Completed", "Marcar tarea como completada"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            }
        } else Text(appT("The assignment completes automatically when every assigned reading, lesson, and discussion response is finished.", "La tarea se completa automáticamente al terminar todas las lecturas, lecciones y respuestas de discusión asignadas."), fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText)
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
private fun AssignmentItemProgressLabel(completed: Boolean) {
    val color = if (completed) Color(0xFF2E6B33) else IlluminedThemeTokens.SecondaryText
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        LessonSymbol(if (completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff, color, Modifier.size(14.dp))
        Text(if (completed) appT("Completed", "Completado") else appT("To do", "Pendiente"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
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
        TextButton(onClick = onBack, enabled = !isWorking) { Text(appT("‹ Back", "‹ Atrás")) }
        Surface(Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), color=Color.White.copy(.94f), shape=RoundedCornerShape(16.dp), shadowElevation=6.dp) {
            Column(Modifier.padding(20.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(reading.title.trim(),fontSize=24.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)
                Text(assignment.title,fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.SecondaryText)
            }
        }
        Surface(Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)), color=Color.White.copy(.94f), shape=RoundedCornerShape(16.dp), shadowElevation=6.dp) {
            Text(reading.text.trim(),Modifier.padding(20.dp),fontSize=17.sp,lineHeight=27.sp,color=IlluminedThemeTokens.Ink)
        }
        error?.let { Text(localizedUserMessage(it),color=Color.Red) }
        Button(onClick=onToggle,enabled=!isWorking,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=IlluminedThemeTokens.Blue),shape=RoundedCornerShape(14.dp)) {
            if(isWorking) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp,color=Color.White)
            else {
                LessonSymbol(if(completed) LessonSymbolKind.CheckCircle else LessonSymbolKind.RadioOff, Color.White, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if(completed) appT("Mark Reading Incomplete", "Marcar lectura como incompleta") else appT("Mark Reading Completed", "Marcar lectura como completada"),fontWeight=FontWeight.SemiBold)
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
internal fun IlluminedBrandHeader(userId: String? = null, photoRefresh: Int = 0, requestCount: Int = 0, onRequests: (() -> Unit)? = null, onAccount: (() -> Unit)? = null) {
    val chromeFontScale = LocalDensity.current.fontScale
    val brandAccessibilityLabel = stringResource(R.string.accessibility_brand_header)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // The logo asset is composed on the base Illumined blue. Keeping
            // the toolbar solid prevents its square artwork from showing a
            // different blue from the surrounding header.
            .background(IlluminedThemeTokens.Blue)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(AppChromePresentation.HeaderContentHeight)
            .padding(horizontal = 16.dp, vertical = 5.dp),
    ) {
        // Keep the wordmark and motto at the screen center, independent of the profile controls.
        Box(modifier = Modifier.align(Alignment.Center).width(230.dp).semantics(mergeDescendants = true) { contentDescription = brandAccessibilityLabel }) {
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
        if (onAccount != null && userId != null) Box(Modifier.align(Alignment.CenterEnd)) {
            HeaderProfileButton(userId, photoRefresh, onAccount)
            if(requestCount != 0 && onRequests != null) Surface(onClick=onRequests, color=Color(0xFFB3261E), contentColor=Color.White, shape=CircleShape, modifier=Modifier.align(Alignment.TopEnd).size(26.dp).semantics { contentDescription = if(requestCount < 0) "Check student requests" else "$requestCount pending student requests" }) {
                Box(contentAlignment=Alignment.Center) { Text(if(requestCount < 0) "!" else if(requestCount > 99) "99+" else "$requestCount", fontSize=11.sp, fontWeight=FontWeight.Bold) }
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
            inviteLink = null,
            isConfigured = true,
            onSignIn = { _, _ -> },
            onCreateAccount = { _, _ -> },
            onResetPassword = { _, _ -> },
            onClearMessage = {},
        )
    }
}
