package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illumined.app.R
import com.illumined.app.data.ProfileSetupRepository
import com.illumined.app.ui.theme.IlluminedThemeTokens

private enum class SetupMode(val label: String) { STUDENT("Student"), INSTRUCTOR("Co-Instructor"), PARISH("New Parish") }

private fun setupT(english: String, spanish: String): String =
    if (java.util.Locale.getDefault().language == "es") spanish else english

private fun SetupMode.localizedLabel(): String = when (this) {
    SetupMode.STUDENT -> setupT("Student", "Estudiante")
    SetupMode.INSTRUCTOR -> setupT("Co-Instructor", "Coinstructor")
    SetupMode.PARISH -> setupT("New Parish", "Nueva parroquia")
}

@Composable
internal fun ProfileSetupExperience(inviteLink: IlluminedInviteLink? = null, onComplete: () -> Unit, onSignOut: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { ProfileSetupRepository() }; var mode by rememberSaveable { mutableStateOf(SetupMode.STUDENT) }; var name by rememberSaveable { mutableStateOf("") }; var classId by rememberSaveable { mutableStateOf("") }; var invite by rememberSaveable { mutableStateOf("") }; var parish by rememberSaveable { mutableStateOf("") }; var setupCode by rememberSaveable { mutableStateOf("") }; var working by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var city by rememberSaveable { mutableStateOf("") }
    var editingStartupCode by rememberSaveable { mutableStateOf(false) }
    val accountUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var hasParishAccess by remember(accountUid) { mutableStateOf(false) }
    var checkingAccess by remember(accountUid) { mutableStateOf(true) }
    var accessError by remember(accountUid) { mutableStateOf(false) }
    var accessAttempt by remember(accountUid) { mutableIntStateOf(0) }
    DisposableEffect(accountUid, accessAttempt) {
        var active = true
        checkingAccess = true
        hasParishAccess = false
        accessError = false
        repository.checkParishAccess(success = { ready ->
            if (active) {
                hasParishAccess = ready
                checkingAccess = false
                if (ready) { mode = SetupMode.PARISH; setupCode = ""; editingStartupCode = false }
            }
        }, error = { if (active) { checkingAccess = false; accessError = true } })
        onDispose { active = false }
    }
    val valid = !checkingAccess && name.isNotBlank() && (mode == SetupMode.PARISH || classId.isNotBlank()) && when(mode) { SetupMode.STUDENT -> true; SetupMode.INSTRUCTOR -> invite.isNotBlank(); SetupMode.PARISH -> parish.isNotBlank() && city.trim().length >= 2 && (hasParishAccess || setupCode.isNotBlank()) }
    LaunchedEffect(inviteLink, hasParishAccess) {
        // Server-confirmed account access outranks a saved student invitation on this device.
        if (hasParishAccess) { mode = SetupMode.PARISH; return@LaunchedEffect }
        if (inviteLink == null && context.getSharedPreferences("classroom-discovery", 0).getBoolean("start-classroom", false)) mode = SetupMode.PARISH
        inviteLink?.let { link ->
            when (link.role) {
                InviteRole.STUDENT -> { mode = SetupMode.STUDENT; classId = link.code }
                InviteRole.INSTRUCTOR -> { mode = SetupMode.INSTRUCTOR; classId = link.classId; invite = link.code }
                InviteRole.PARISH -> { mode = SetupMode.PARISH; setupCode = link.code }
            }
        }
    }
    Column(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
        if (checkingAccess) SetupCard { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(setupT("Checking your parish access…", "Comprobando el acceso a tu parroquia…")) }
        if (accessError) SetupCard {
            Text(setupT("We couldn’t confirm your account setup. Please retry before continuing so we can open the right classroom setup.", "No pudimos confirmar la configuración de tu cuenta. Reintenta para abrir la configuración correcta del aula."))
            TextButton(onClick = { accessAttempt++ }) { Text(setupT("Retry access check", "Reintentar comprobación")) }
        }
        if (!checkingAccess && !accessError) {
        SetupCard {
            if (mode != SetupMode.STUDENT || inviteLink != null) {
            Text(when(mode) {
                SetupMode.STUDENT -> setupT("Join Your Classroom", "Únete a tu aula")
                SetupMode.INSTRUCTOR -> setupT("Join Your Teaching Team", "Únete al equipo docente")
                SetupMode.PARISH -> setupT("Set Up Your Parish", "Configura tu parroquia")
            }, fontSize=26.sp, fontWeight=FontWeight.SemiBold, color=IlluminedThemeTokens.Blue)
            Text(if(mode == SetupMode.PARISH) setupT("A few details to make your classroom ready.", "Unos datos para preparar tu aula.") else setupT("Add your name to continue.", "Añade tu nombre para continuar."), color=IlluminedThemeTokens.SecondaryText)
            }
            if (mode == SetupMode.STUDENT && inviteLink == null) {
                ClassroomEnrollmentSetup(onApproved = onComplete)
            } else {
            OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text(setupT("Your Name", "Tu nombre"))},singleLine=true)
            if(mode==SetupMode.PARISH) OutlinedTextField(parish,{parish=it},Modifier.fillMaxWidth(),label={Text(setupT("Parish or Program Name", "Nombre de la parroquia o programa"))},singleLine=true)
            if(mode==SetupMode.PARISH) { OutlinedTextField(city,{city=it},Modifier.fillMaxWidth(),label={Text(setupT("City", "Ciudad"))},singleLine=true); Text(setupT("After setup, enable classroom search in Classroom Codes.", "Después de configurar el aula, habilita la búsqueda en Códigos del aula."),fontSize=13.sp) }
            // Invitation credentials stay in state, never in editable profile fields.
            if(mode==SetupMode.PARISH) {
                if (hasParishAccess) {
                    Text(setupT("Parish access is linked to your account. No startup code is needed.", "El acceso a la parroquia está vinculado a tu cuenta. No necesitas un código de inicio."),fontSize=14.sp,color=IlluminedThemeTokens.SecondaryText)
                } else {
                    if(inviteLink?.role != InviteRole.PARISH || editingStartupCode) OutlinedTextField(setupCode,{setupCode=it.uppercase()},Modifier.fillMaxWidth(),label={Text(setupT("Parish Setup Code", "Código de configuración de la parroquia"))},singleLine=true)
                }
            }
            Button(onClick={offerSetupPhotos(context);working=true;error=null;if(mode != SetupMode.STUDENT) { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { context.getSharedPreferences("instructor-walkthrough",0).edit().putBoolean("setup-$it-pending",true).apply() } };val success={working=false;onComplete()};val failure:(Throwable)->Unit={working=false;error=it.message?:setupT("Profile setup could not be completed.", "No se pudo completar la configuración del perfil.")};when(mode){SetupMode.STUDENT->repository.joinStudent(name,classId,success,failure);SetupMode.INSTRUCTOR->repository.claimInstructor(name,classId,invite,success,failure);SetupMode.PARISH->repository.startClass(name,parish,setupCode,success,failure,city=city)}},enabled=valid&&!working,shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=IlluminedThemeTokens.Gold,contentColor=Color(0xFF172330)),modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){Text(if(working) setupT("Saving…", "Guardando…") else when(mode){SetupMode.STUDENT->setupT("Join Classroom", "Unirse al aula");SetupMode.INSTRUCTOR->setupT("Accept Invitation", "Aceptar invitación");SetupMode.PARISH->setupT("Create Classroom", "Crear aula")})}
        }
        }
        }
        error?.let { message -> SetupCard { Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=androidx.compose.ui.Alignment.Top){DiscussionSymbol(DiscussionSymbolKind.Warning,Color.Red,Modifier.size(18.dp));Text(localizedUserMessage(message),fontSize=15.sp,color=Color.Red,modifier=Modifier.weight(1f))} } }
        if (mode == SetupMode.STUDENT && inviteLink == null) Button(onClick=onSignOut, enabled=!working, modifier=Modifier.fillMaxWidth().heightIn(min=54.dp), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=IlluminedThemeTokens.Gold, contentColor=IlluminedThemeTokens.Ink)) {
            Text(setupT("Back to Login", "Volver al inicio"))
        } else TextButton(onClick=onSignOut, enabled=!working, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFFD7EDFF))) {
            Text(setupT("Not you? Use another account", "¿No eres tú? Usa otra cuenta"))
        }
    }
}

@Composable private fun SetupCard(content:@Composable ColumnScope.()->Unit){Surface(shape=RoundedCornerShape(26.dp),color=Color.White.copy(.97f),shadowElevation=6.dp,border=androidx.compose.foundation.BorderStroke(1.dp,IlluminedThemeTokens.Gold.copy(.22f))){Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)}}
