package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ListenerRegistration
import com.illumined.app.data.AccessCode
import com.illumined.app.data.AccessCodeRepository
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.util.Locale

private fun accessT(english: String, spanish: String) = if (Locale.getDefault().language == "es") spanish else english

@Composable
fun AccessCodeExperience(profile: UserProfile, parishMode: Boolean, onBack: () -> Unit) {
    var walkthroughPage by remember { mutableStateOf<String?>(null) }
    if (walkthroughPage != null && profile.isAdmin) {
        androidx.activity.compose.BackHandler { walkthroughPage = null }
        WalkthroughManagement(editor = walkthroughPage == "editor") { walkthroughPage = null }
        return
    }
    val tour = LocalInstructorWalkthrough.current
    val touring = tour?.active == true
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(tour?.target) {
        if (touring && !parishMode) {
            val index = when {
                tour?.target == "codes-overview" || tour?.target == "codes-instructor-create" -> 0
                tour?.target?.startsWith("codes-student-") == true -> 1
                else -> 2
            }
            listState.animateScrollToItem(index)
        }
    }
    var showDirectory by remember { mutableStateOf(false) }
    if (showDirectory && profile.isAdmin) {
        androidx.activity.compose.BackHandler { showDirectory = false }
        AdminDirectoryExperience(profile) { showDirectory = false }
        return
    }
    var showUpdates by remember { mutableStateOf(false) }
    if (showUpdates && profile.isAdmin) {
        androidx.activity.compose.BackHandler { showUpdates = false }
        InstructorUpdatesExperience(profile, adminTools = true) { showUpdates = false }
        return
    }
    val repository = remember { AccessCodeRepository() }; val classId = profile.selectedClassId
    var codes by remember { mutableStateOf(emptyList<AccessCode>()) }; var error by remember { mutableStateOf<String?>(null) }; var working by remember { mutableStateOf(false) }
    DisposableEffect(parishMode, classId) { val listener: ListenerRegistration = if (parishMode) repository.listenParishCodes({ codes = it }, { error = accessT("Setup codes could not be loaded.", "No se pudieron cargar los códigos de configuración.") }) else repository.listenInstructorCodes(classId, { codes = it }, { error = accessT("Invite codes could not be loaded.", "No se pudieron cargar los códigos de invitación.") }); onDispose { listener.remove() } }
    val title = if (parishMode) accessT("Parish Setup Codes", "Códigos de configuración parroquial") else accessT("Classroom Codes", "Códigos del aula")
    val description = if (parishMode) accessT("Create one-use setup codes for the first instructor at a new parish. After they use the code, the app closes it automatically.", "Crea códigos de un solo uso para el primer instructor de una parroquia nueva. Después de usar el código, la aplicación lo cierra automáticamente.") else instructorInviteDescription(classId)
    LazyColumn(Modifier.fillMaxSize().background(codeBrush()), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (parishMode && profile.isAdmin) item {
            Button(onClick = { walkthroughPage = "management" }, modifier = Modifier.fillMaxWidth()) { Text(accessT("Walkthrough Management", "Administrar recorrido")) }
            Button(onClick = { walkthroughPage = "editor" }, modifier = Modifier.fillMaxWidth()) { Text(accessT("Edit Walkthrough", "Editar recorrido")) }
        }
        if (parishMode && profile.isAdmin) item { Button(onClick = { showDirectory = true }, modifier = Modifier.fillMaxWidth()) { Text(accessT("Parish & Account Support", "Parroquias y soporte de cuentas")) } }
        if (parishMode && profile.isAdmin) item { Button(onClick = { showUpdates = true }, modifier = Modifier.fillMaxWidth()) { Text(accessT("From Illumined", "De Illumined")) } }
        item { TextButton(onClick = onBack) { Text(accessT("‹ Back", "‹ Atrás")) }; CodeCard(Modifier.walkthroughAnchor(if(tour?.target == "codes-instructor-create") "codes-instructor-create" else "codes-overview")) { Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){InstructorSymbol(InstructorSymbolKind.Key,IlluminedThemeTokens.Blue,Modifier.size(22.dp));Text(title,fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)}; Text(description, color = IlluminedThemeTokens.SecondaryText, lineHeight = 22.sp); Button(onClick = { working = true; val success = { working = false }; val failure: (Throwable) -> Unit = { working = false; error = it.localizedMessage ?: accessT("A new code could not be created.", "No se pudo crear un código nuevo.") }; if (parishMode) repository.createParishCode(profile, success, failure) else repository.createInstructorCode(profile, success, failure) }, enabled = !touring && !working && (parishMode || classId.isNotBlank()), modifier = Modifier.fillMaxWidth()) { if(working)Text(accessT("Creating…", "Creando…")) else Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){InstructorSymbol(InstructorSymbolKind.PlusCircle,Color.White,Modifier.size(18.dp));Text(accessT("New Code", "Nuevo código"))} } } }
        if (!parishMode && classId.isNotBlank()) item {
            CodeCard(Modifier.walkthroughAnchor(if(tour?.target?.startsWith("codes-student-") == true) tour.target else "codes-student-create")) {
                Text(accessT("Student Class Link", "Enlace de la clase para estudiantes"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text(if(Locale.getDefault().language=="es") "Comparte este enlace reutilizable con los estudiantes que se unirán a la clase $classId." else "Share this reusable link with students joining class $classId.", color = IlluminedThemeTokens.SecondaryText)
                StudentInvitationControls(classId)
            }
        }
        if (codes.isEmpty()) item { CodeCard(Modifier.walkthroughAnchor(tour?.target?.takeIf { it.startsWith("codes-instructor-") && it != "codes-instructor-create" } ?: "codes-instructor-share")) { InstructorEmptyStateContent(InstructorEmptyStateSpec(if(parishMode)accessT("No Setup Codes", "No hay códigos de configuración") else accessT("No Invite Codes", "No hay códigos de invitación"), accessCodeEmptyDescription(parishMode), "key")) } }
        items(codes, key = { it.code }) { code -> CodeCard(Modifier.walkthroughAnchor(if(code == codes.firstOrNull()) tour?.target?.takeIf { it.startsWith("codes-instructor-") && it != "codes-instructor-create" } ?: "codes-instructor-share" else "code-${code.code}")) { Row(Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text(code.code, fontSize = if (parishMode) 24.sp else 26.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(if (code.isActive) accessT("Unused", "Sin usar") else accessT("Used", "Usado"), color = if (code.isActive) IlluminedThemeTokens.Gold else IlluminedThemeTokens.SecondaryText) }; if (code.isActive) TextButton(enabled = !touring, onClick = { repository.deactivate(if (parishMode) "parishSetupCodes" else "instructorInviteCodes", code.code, {}, { error = accessT("Code could not be deactivated.", "No se pudo desactivar el código.") }) }) { Text(accessT("Deactivate", "Desactivar"), color = Color.Red) } }; if (code.parishName.isNotBlank()) Text("${accessT("Parish", "Parroquia")}: ${code.parishName}", color = IlluminedThemeTokens.SecondaryText); if (code.classId.isNotBlank()) Text("${accessT("Class", "Clase")}: ${code.classId}", color = IlluminedThemeTokens.SecondaryText); Text(if (code.usedByName.isNotBlank()) "${accessT("Used by", "Usado por")}: ${code.usedByName}" else if (code.usedByEmail.isNotBlank()) "${accessT("Used by", "Usado por")}: ${code.usedByEmail}" else if (parishMode) accessT("Unused codes can start one new parish/class.", "Los códigos sin usar pueden iniciar una parroquia o clase nueva.") else accessT("Unused codes can be shared with one new instructor.", "Cada código sin usar se puede compartir con un instructor nuevo."), color = IlluminedThemeTokens.SecondaryText); if (code.isActive && !touring) InviteShareControls(IlluminedInviteLink(if (parishMode) InviteRole.PARISH else InviteRole.INSTRUCTOR, classId = if (parishMode) "" else code.classId, code = code.code)) } }
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(if (parishMode) accessT("Setup Code Error", "Error del código de configuración") else accessT("Invite Code Error", "Error del código de invitación")) },
            text = { Text(localizedUserMessage(message)) },
            confirmButton = { TextButton(onClick = { error = null }) { Text(accessT("OK", "Aceptar")) } }
        )
    }
}

@Composable
private fun StudentInvitationControls(classId: String) {
    val touring = LocalInstructorWalkthrough.current?.active == true
    if (touring) {
        Text(accessT("Student invitation controls are paused during this read-only tour. No codes will be loaded or generated.", "Las invitaciones están en pausa durante este recorrido de solo lectura. No se cargan ni generan códigos."))
        return
    }
    var code by remember(classId) { mutableStateOf("") }
    var working by remember(classId) { mutableStateOf(false) }
    var message by remember(classId) { mutableStateOf("") }
    var action by remember { mutableStateOf<String?>(null) }
    fun load(kind: String) {
        working = true; message = ""
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1").getHttpsCallable("manageStudentInvitation")
            .call(mapOf("classId" to classId, "action" to kind))
            .addOnSuccessListener { code = (it.data as? Map<*, *>)?.get("code") as? String ?: ""; working = false }
            .addOnFailureListener { message = it.localizedMessage.orEmpty(); working = false }
    }
    LaunchedEffect(classId) { load("get") }
    Text(accessT("Student invitation code", "Código de invitación de estudiante"), fontWeight = FontWeight.SemiBold)
    if (code.isNotEmpty()) {
        Text(code, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        InviteShareControls(IlluminedInviteLink(InviteRole.STUDENT, classId = classId, code = code))
        TextButton(enabled = !working, onClick = { action = "disable" }) { Text(accessT("Disable invitation", "Desactivar invitación")) }
    }
    Text(accessT("Codes expire after 90 days. Replacing or disabling a code does not remove enrolled students.", "Los códigos vencen a los 90 días. Reemplazar o desactivar un código no retira a los estudiantes inscritos."))
    Button(enabled = !working, onClick = { action = "regenerate" }) { Text(accessT("Generate new student code", "Generar nuevo código de estudiante")) }
    if (working) CircularProgressIndicator()
    if (message.isNotEmpty()) Text(message, color = Color.Red)
    if (action != null) AlertDialog(onDismissRequest = { action = null }, title = { Text(accessT("Replace invitation?", "¿Cambiar invitación?")) }, text = { Text(accessT("The previous invitation will stop working.", "La invitación anterior dejará de funcionar.")) }, confirmButton = { TextButton(onClick = { val selected = action; action = null; selected?.let { load(it) } }) { Text(accessT("Confirm", "Confirmar")) } }, dismissButton = { TextButton(onClick = { action = null }) { Text(accessT("Cancel", "Cancelar")) } })
}

internal fun instructorInviteDescription(classId: String) = if(Locale.getDefault().language=="es")
    "Crea códigos de instructor de un solo uso para $classId. Entrega el código a un instructor nuevo para que lo ingrese al configurar su perfil. Una vez utilizado, el código se cierra automáticamente."
else "Create one-use instructor codes for $classId. Give the code to a new instructor, and they can enter it while setting up their profile. Once used, the code is automatically closed."

internal fun accessCodeEmptyDescription(parishMode: Boolean) = if (parishMode) accessT("Tap New Code when a new parish needs its first instructor account.", "Pulsa Nuevo código cuando una parroquia nueva necesite su primera cuenta de instructor.") else accessT("Create a code when you need to add another instructor.", "Crea un código cuando necesites añadir otro instructor.")

@Composable private fun CodeCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) { Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) } }
private fun codeBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
