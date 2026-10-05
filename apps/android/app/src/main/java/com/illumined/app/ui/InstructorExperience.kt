package com.illumined.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ListenerRegistration
import com.illumined.app.data.*
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class InstructorPage { MENU, CLASSES, ANNOUNCEMENTS, SCHEDULE, ASSIGNMENTS, DISCUSSIONS, PROGRESS, DAILY_FORMATION, RITE_PREPARATION, INVITES, UPDATES }

private fun instructorT(english: String, spanish: String): String =
    if (Locale.getDefault().language == "es") spanish else english

private val assignmentReadingsSaver = listSaver<List<AssignmentReading>, String>(
    save = { readings -> readings.flatMap { listOf(it.id, it.title, it.text) } },
    restore = { values -> values.chunked(3).mapNotNull { fields -> fields.takeIf { it.size == 3 }?.let { AssignmentReading(it[0], it[1], it[2]) } } },
)

@Composable
fun InstructorExperience(profile: UserProfile, schedule: List<ScheduleItem>, assignments: List<Assignment>, prompts: List<DiscussionPrompt>, onBack: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(InstructorPage.MENU) }
    BackHandler {
        if (page == InstructorPage.MENU) onBack() else page = InstructorPage.MENU
    }
    val tour = LocalInstructorWalkthrough.current
    val shownPage = if(tour?.active == true) when(tour.screen) {
        "classroom-codes" -> InstructorPage.INVITES
        "instructor-tools" -> InstructorPage.MENU
        else -> page
    } else page
    when (shownPage) {
        InstructorPage.UPDATES -> InstructorUpdatesExperience(profile) { page = InstructorPage.MENU }
        InstructorPage.MENU -> InstructorMenu(profile, onBack) { page = it }
        InstructorPage.CLASSES -> ClassManager(profile) { page = InstructorPage.MENU }
        InstructorPage.ANNOUNCEMENTS -> AnnouncementManager(profile) { page = InstructorPage.MENU }
        InstructorPage.SCHEDULE -> ScheduleManager(profile, schedule) { page = InstructorPage.MENU }
        InstructorPage.ASSIGNMENTS -> AssignmentManager(profile, assignments, schedule) { page = InstructorPage.MENU }
        InstructorPage.DISCUSSIONS -> DiscussionManager(profile, assignments) { page = InstructorPage.MENU }
        InstructorPage.PROGRESS -> StudentProgressManager(profile) { page = InstructorPage.MENU }
        InstructorPage.DAILY_FORMATION -> DailyFormationManager(profile) { page = InstructorPage.MENU }
        InstructorPage.RITE_PREPARATION -> RitePreparationManager(profile) { page = InstructorPage.MENU }
        InstructorPage.INVITES -> AccessCodeExperience(profile, parishMode = false) { page = InstructorPage.MENU }
    }
}

@Composable
private fun InstructorMenu(profile: UserProfile, onBack: () -> Unit, select: (InstructorPage) -> Unit) {
    val walkthrough = LocalInstructorWalkthrough.current
    fun destination(key: String) = when (key) {
        "classes" -> InstructorPage.CLASSES
        "announcements" -> InstructorPage.ANNOUNCEMENTS
        "schedule" -> InstructorPage.SCHEDULE
        "assignments" -> InstructorPage.ASSIGNMENTS
        "discussions" -> InstructorPage.DISCUSSIONS
        "progress" -> InstructorPage.PROGRESS
        "daily-formation" -> InstructorPage.DAILY_FORMATION
        "rite-preparation" -> InstructorPage.RITE_PREPARATION
        "invites" -> InstructorPage.INVITES
        "updates" -> InstructorPage.UPDATES
        else -> InstructorPage.MENU
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    fun anchor(key: String) = "tools-" + when(key) {
        "progress" -> "students"
        "daily-formation" -> "daily"
        "rite-preparation" -> "guides"
        "invites" -> "codes"
        else -> key
    }
    LaunchedEffect(walkthrough?.target) {
        if(walkthrough?.active == true && walkthrough.screen == "instructor-tools") {
            val index = if(walkthrough.target == "tools-overview") 0 else InstructorToolPresentation.items.indexOfFirst { anchor(it.key) == walkthrough.target } + 1
            listState.animateScrollToItem(index.coerceAtLeast(0))
        }
    }
    val className = profile.selectedClassId.ifBlank { instructorT("your class", "tu clase") }
    LazyColumn(
        Modifier.fillMaxSize().background(instructorBrush()),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text(instructorT("‹ Back", "‹ Atrás")) }
            Box(Modifier.walkthroughAnchor("tools-overview")) { InstructorCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InstructorSymbol(InstructorSymbolKind.Tools, IlluminedThemeTokens.Blue, Modifier.size(24.dp))
                    Text(instructorT("Instructor Tools", "Herramientas del instructor"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                }
                Text(if(Locale.getDefault().language=="es") "Administra el contenido de la clase $className." else "Manage class content for $className.", fontSize = 16.sp, lineHeight = 22.sp, color = IlluminedThemeTokens.SecondaryText)
            } }
        }
        if(walkthrough?.showsToolEntry == true) item { InstructorCard {
            Text(instructorT("Explore Illumined","Explora Illumined"),fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)
            Text(instructorT("Walk through your classroom’s pages and cards.","Recorre las páginas y tarjetas de tu aula."))
            Button(onClick={walkthrough?.start()}) {Text(instructorT("Explore","Explorar"))}
            TextButton(onClick={walkthrough?.dismiss()}) {Text(instructorT("Dismiss walkthrough", "Descartar recorrido"))}
        } }
        items(InstructorToolPresentation.items.filter { walkthrough?.active == true || it.key !in setOf("progress", "invites") }, key = { it.key }) { tool ->
            val localizedTitle = InstructorToolPresentation.localizedTitle(tool)
            val localizedSubtitle = InstructorToolPresentation.localizedSubtitle(tool)
            val localizedStatus = InstructorToolPresentation.localizedStatus()
            Surface(
                onClick = { select(destination(tool.key)) },
                modifier = Modifier.walkthroughAnchor(anchor(tool.key)).semantics(mergeDescendants = true) {
                    contentDescription = "$localizedTitle. $localizedSubtitle. $localizedStatus"
                },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(.94f),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f)),
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                        InstructorSymbol(instructorSymbol(tool.symbolName), IlluminedThemeTokens.Gold, Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(localizedTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                        Text(localizedSubtitle, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(localizedStatus, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                }
            }
        }
    }
}

@Composable
private fun ClassManager(profile: UserProfile, onBack: () -> Unit) {
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    var tool by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = opened != null) { if(tool != null) tool = null else opened = null }
    val selected = opened?.takeIf { it in profile.activeClassIds }
    if(selected != null && tool != null) {
        val scoped = profile.copy(activeClassId = selected)
        when(tool) {
            "students" -> StudentProgressManager(scoped) { tool = null }
            "invites" -> AccessCodeExperience(scoped, false) { tool = null }
            else -> Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp)) {
                TextButton(onClick={tool=null}) { Text("‹ " + selected) }
                if(tool == "requests") ClassroomApprovalQueue(selected)
                else { ProfilePhotoEditor("classroom", selected); ClassroomListingEditor(selected) }
            }
        }
        return
    }
    val profileRepository = remember { FormationRepository() }
    val setupRepository = remember { ProfileSetupRepository() }
    val classRepository = remember { ClassManagementRepository() }
    var creating by rememberSaveable { mutableStateOf(false) }
    var newClassId by rememberSaveable { mutableStateOf("") }
    var workingClassId by remember { mutableStateOf<String?>(null) }
    var archiveCandidate by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    val activeClasses = profile.activeClassIds
    val archivedClasses = profile.classIds.filter(profile.archivedClassIds::contains)

    LazyColumn(
        Modifier.fillMaxSize().background(instructorBrush()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ManagerHeader(
                instructorT("Classroom Management", "Administración del aula"),
                instructorT("Create classes, choose the active class, or archive a class while preserving its records.", "Crea clases, elige la clase activa o archiva una clase conservando sus registros."),
                onBack,
                workingClassId == null,
            ) { creating = true }
        }
        if (creating) item {
            InstructorCard {
                Text(instructorT("Create a Class", "Crear una clase"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text(instructorT("Students will enter this class ID when setting up their accounts.", "Los estudiantes ingresarán este ID de clase al configurar sus cuentas."), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                OutlinedTextField(
                    value = newClassId,
                    onValueChange = { newClassId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(instructorT("New class ID", "ID de la clase nueva")) },
                    singleLine = true,
                    enabled = workingClassId == null,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { creating = false; newClassId = "" }, modifier = Modifier.weight(1f)) { Text(instructorT("Cancel", "Cancelar")) }
                    Button(
                        onClick = {
                            val requestedId = newClassId.trim()
                            workingClassId = requestedId
                            setupRepository.createAdditionalInstructorClass(profile, requestedId, success = {
                                workingClassId = null
                                creating = false
                                newClassId = ""
                                confirmation = if(Locale.getDefault().language=="es") "Se creó $requestedId y ahora está activa." else "$requestedId was created and is now active."
                            }, error = {
                                workingClassId = null
                                error = it.localizedMessage ?: instructorT("The class could not be created.", "No se pudo crear la clase.")
                            })
                        },
                        modifier = Modifier.weight(1f),
                        enabled = newClassId.isNotBlank() && workingClassId == null,
                    ) { Text(if (workingClassId != null) instructorT("Creating...", "Creando...") else instructorT("Create", "Crear")) }
                }
            }
        }
        item { Text(instructorT("Active Classes", "Clases activas"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink) }
        if (activeClasses.isEmpty()) item { InstructorCard { Text(instructorT("No active classes.", "No hay clases activas."), color = IlluminedThemeTokens.SecondaryText) } }
        items(activeClasses, key = { "active-$it" }) { classId ->
            InstructorCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    InstructorSymbol(InstructorSymbolKind.People, IlluminedThemeTokens.Gold, Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(classId, modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    if (classId == profile.selectedClassId) Text(instructorT("Active", "Activa"), color = IlluminedThemeTokens.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick={opened=if(opened==classId)null else classId}, modifier=Modifier.fillMaxWidth()) { Text(if(opened==classId) instructorT("Close Classroom", "Cerrar aula") else instructorT("Manage Classroom", "Administrar aula")) }
                if(opened==classId) {
                    RefreshmentSetting(classId)
                    OutlinedButton(onClick={tool="details"}, modifier=Modifier.fillMaxWidth()) { Text(instructorT("Classroom Details & Photo", "Datos y foto del aula")) }
                    OutlinedButton(onClick={tool="students"}, modifier=Modifier.fillMaxWidth()) { Text(instructorT("Students & Progress", "Estudiantes y progreso")) }
                    OutlinedButton(onClick={tool="invites"}, modifier=Modifier.fillMaxWidth()) { Text(instructorT("Invitations & Codes", "Invitaciones y códigos")) }
                    OutlinedButton(onClick={tool="requests"}, modifier=Modifier.fillMaxWidth()) { Text(instructorT("Join Requests", "Solicitudes de ingreso")) }
                if (classId != profile.selectedClassId) OutlinedButton(
                    onClick = {
                        workingClassId = classId
                        profileRepository.setActiveClass(profile, classId, { workingClassId = null }, {
                            workingClassId = null
                            error = it.localizedMessage ?: instructorT("The active class could not be changed.", "No se pudo cambiar la clase activa.")
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = workingClassId == null,
                ) { Text(instructorT("Make Active", "Activar")) }
                TextButton(
                    onClick = { archiveCandidate = classId },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeClasses.size > 1 && workingClassId == null,
                ) { Text(instructorT("Archive Class", "Archivar clase")) }
                }
                if (opened==classId && activeClasses.size <= 1) Text(instructorT("Create or restore another class before archiving this one.", "Crea o restaura otra clase antes de archivar esta."), fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
            }
        }
        if (archivedClasses.isNotEmpty()) {
            item { Text(instructorT("Archived Classes", "Clases archivadas"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink) }
            items(archivedClasses, key = { "archived-$it" }) { classId ->
                InstructorCard {
                    Text(classId, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(instructorT("Records are preserved. New class activity is paused.", "Los registros se conservan. La actividad nueva de la clase está pausada."), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                    Button(
                        onClick = {
                            workingClassId = classId
                            classRepository.restoreClass(classId, {
                                workingClassId = null
                                confirmation = if(Locale.getDefault().language=="es") "Se restauró $classId." else "$classId was restored."
                            }, {
                                workingClassId = null
                                error = it.localizedMessage ?: instructorT("The class could not be restored.", "No se pudo restaurar la clase.")
                            })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = workingClassId == null,
                    ) { Text(if (workingClassId == classId) instructorT("Restoring...", "Restaurando...") else instructorT("Restore Class", "Restaurar clase")) }
                }
            }
        }
    }

    archiveCandidate?.let { classId ->
        AlertDialog(
            onDismissRequest = { archiveCandidate = null },
            title = { Text(if(Locale.getDefault().language=="es") "¿Archivar $classId?" else "Archive $classId?") },
            text = { Text(instructorT("The class will move out of the active list and new activity will pause. All class records will be preserved and the class can be restored later.", "La clase saldrá de la lista activa y la actividad nueva se pausará. Todos sus registros se conservarán y la clase podrá restaurarse después.")) },
            dismissButton = { TextButton(onClick = { archiveCandidate = null }) { Text(instructorT("Cancel", "Cancelar")) } },
            confirmButton = { TextButton(onClick = {
                archiveCandidate = null
                workingClassId = classId
                classRepository.archiveClass(classId, {
                    workingClassId = null
                    confirmation = if(Locale.getDefault().language=="es") "Se archivó $classId." else "$classId was archived."
                }, {
                    workingClassId = null
                    error = it.localizedMessage ?: instructorT("The class could not be archived.", "No se pudo archivar la clase.")
                })
            }) { Text(instructorT("Archive", "Archivar")) } },
        )
    }
    InstructorErrorAlert(instructorT("Class Error", "Error de clase"), error) { error = null }
    confirmation?.let { message -> AlertDialog(onDismissRequest = { confirmation = null }, title = { Text(instructorT("Classes Updated", "Clases actualizadas")) }, text = { Text(message) }, confirmButton = { TextButton(onClick = { confirmation = null }) { Text(instructorT("OK", "Aceptar")) } }) }
}

@Composable
private fun AnnouncementManager(profile: UserProfile, onBack: () -> Unit) {
    val repository = remember { InstructorRepository() }; val classId = profile.selectedClassId; var values by remember { mutableStateOf(emptyList<Announcement>()) }; var editingId by rememberSaveable { mutableStateOf<String?>(null) }; val editing = restoreEditedRecord(values, editingId, Announcement::id); var creating by rememberSaveable { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; var sentMessage by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = creating || editingId != null) {
        creating = false
        editingId = null
    }
    DisposableEffect(classId) { var listener: ListenerRegistration? = null; if (classId.isNotBlank()) listener = repository.listenAnnouncements(classId, { values = it }, { error = it.localizedMessage ?: instructorT("Announcements could not be loaded.", "No se pudieron cargar los anuncios.") }); onDispose { listener?.remove() } }
    if (creating || editing != null) AnnouncementEditor(
        editing,
        onCancel = { creating = false; editingId = null },
        onSave = { title, message, active, sendPush, finished ->
            val failed: (Throwable) -> Unit = { finished(); error = it.localizedMessage ?: instructorT("Announcement could not be saved.", "No se pudo guardar el anuncio.") }
            if (editing == null && sendPush) repository.createAnnouncementWithPush(profile, title, message, active, { recipients ->
                finished(); creating = false
                sentMessage = if (Locale.getDefault().language == "es") "Anuncio enviado a $recipients dispositivo${if (recipients == 1) "" else "s"}." else "Announcement sent to $recipients device${if (recipients == 1) "" else "s"}."
            }, failed)
            else if (editing == null) repository.createAnnouncement(profile, title, message, { finished(); creating = false }, failed)
            else repository.updateAnnouncement(profile, editing.id, title, message, active, { finished(); editingId = null }, failed)
        },
        onDelete = editing?.let { value -> { finished -> repository.deleteAnnouncement(profile, value.id, { finished(); editingId = null }, { finished(); error = it.localizedMessage ?: instructorT("Announcement could not be deleted.", "No se pudo eliminar el anuncio.") }) } },
    ) else LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ManagerHeader(instructorT("Announcements", "Anuncios"), instructorT("Create updates that appear on the student dashboard.", "Crea novedades que aparecen en la página de inicio del estudiante."), onBack, classId.isNotBlank()) { creating = true } }
        if (values.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStateSpec(instructorT("No Announcements", "No hay anuncios"), instructorT("Create your first announcement for this class.", "Crea el primer anuncio para esta clase."), "megaphone")) } }
        items(values, key = { it.id }) { value ->
            InstructorListCard(
                onClick = { editingId = value.id },
                description = "${value.title}. ${if (value.isActive) instructorT("Active", "Activo") else instructorT("Hidden", "Oculto")}. ${value.message}",
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        InstructorSymbol(if(value.isActive) InstructorSymbolKind.Active else InstructorSymbolKind.Paused, if (value.isActive) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText, Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(value.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2); Text(if (value.isActive) instructorT("Active", "Activo") else instructorT("Hidden", "Oculto"), color = if (value.isActive) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        InstructorSymbol(InstructorSymbolKind.Chevron, IlluminedThemeTokens.SecondaryText, Modifier.size(10.dp,18.dp))
                    }
                    Text(value.message, color = IlluminedThemeTokens.SecondaryText, maxLines = 3)
                    value.displayTimestamp?.toDate()?.let { Text(if (Locale.getDefault().language == "es") "Actualizado ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)}" else "Updated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)}", color = IlluminedThemeTokens.SecondaryText, fontSize = 11.sp) }
                }
            }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text(instructorT("Announcement Error", "Error del anuncio")) }, text = { Text(localizedUserMessage(message)) }, confirmButton = { TextButton(onClick = { error = null }) { Text(instructorT("OK", "Aceptar")) } }) }
    sentMessage?.let { message -> AlertDialog(onDismissRequest = { sentMessage = null }, title = { Text(instructorT("Announcement Sent", "Anuncio enviado")) }, text = { Text(message) }, confirmButton = { TextButton(onClick = { sentMessage = null }) { Text(instructorT("OK", "Aceptar")) } }) }
}

@Composable
private fun AnnouncementEditor(value: Announcement?, onCancel: () -> Unit, onSave: (String, String, Boolean, Boolean, () -> Unit) -> Unit, onDelete: (((() -> Unit) -> Unit))?) {
    var title by rememberSaveable(value?.id) { mutableStateOf(value?.title.orEmpty()) }
    var message by rememberSaveable(value?.id) { mutableStateOf(value?.message.orEmpty()) }
    var active by rememberSaveable(value?.id) { mutableStateOf(value?.isActive ?: true) }
    var sendPush by rememberSaveable(value?.id) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    BackHandler { if (InstructorEditorOperationPolicy.canInteract(saving)) onCancel() }
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onCancel, enabled = !saving) { Text(instructorT("‹ Cancel", "‹ Cancelar")) }; Text(if (value == null) instructorT("New Announcement", "Nuevo anuncio") else instructorT("Edit Announcement", "Editar anuncio"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
        item { InstructorCard { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(instructorT("Title", "Título")) }, singleLine = true); OutlinedTextField(message, { message = it }, Modifier.fillMaxWidth(), label = { Text(instructorT("Message", "Mensaje")) }, minLines = 5, maxLines = 10); Row(verticalAlignment = Alignment.CenterVertically) { Text(instructorT("Visible to Students", "Visible para los estudiantes"), fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Switch(active, { active = it }, enabled = !saving) }; if (value == null) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(instructorT("Send push notification", "Enviar notificación push"), fontWeight = FontWeight.SemiBold); Text(instructorT("Alert class members who have notifications enabled.", "Avisa a los miembros de la clase que tienen activadas las notificaciones."), fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText) }; Switch(sendPush, { sendPush = it }, enabled = !saving) } } } }
        item { Button(onClick = { saving = true; onSave(title, message, active, sendPush) { saving = false } }, enabled = AnnouncementEditorPolicy.canSave(title, message, saving), modifier = Modifier.fillMaxWidth()) { Text(if (saving) instructorT("Saving...", "Guardando...") else if (value == null && sendPush) instructorT("Send Announcement", "Enviar anuncio") else instructorT("Save Announcement", "Guardar anuncio")) } }
        if (onDelete != null) item { OutlinedButton(onClick = { confirmingDelete = true }, enabled = !saving, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text(instructorT("Delete Announcement", "Eliminar anuncio")) } }
    }
    if (confirmingDelete && onDelete != null) AlertDialog(onDismissRequest = { confirmingDelete = false }, title = { Text(instructorT("Delete this announcement?", "¿Eliminar este anuncio?")) }, confirmButton = { TextButton(onClick = { confirmingDelete = false; saving = true; onDelete { saving = false } }) { Text(instructorT("Delete", "Eliminar"), color = Color.Red) } }, dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(instructorT("Cancel", "Cancelar")) } })
}

@Composable
private fun ScheduleManager(profile: UserProfile, schedule: List<ScheduleItem>, onBack: () -> Unit) {
    val repository = remember { InstructorRepository() }
    val classId = profile.selectedClassId
    var showingEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = restoreEditedRecord(schedule, editingId, ScheduleItem::id)
    var showingImport by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = showingEditor || showingImport) {
        showingEditor = false
        showingImport = false
        editingId = null
    }

    when {
        showingImport -> ScheduleImportScreen(
            existing = schedule,
            onCancel = { showingImport = false },
            onImport = { rows, replace, done ->
                repository.importSchedule(profile, rows, replace, schedule,
                    { done(); showingImport = false },
                    { problem -> done(); error = problem.message ?: instructorT("The schedule could not be imported.", "No se pudo importar el calendario.") })
            },
        )
        showingEditor -> ScheduleEditor(
            value = editing,
            onCancel = { showingEditor = false; editingId = null },
            onSave = { topic, details, date, finished ->
                val value = editing
                val keepsExistingOrder = value?.date?.toDate()?.time?.let { sameScheduleDay(it, date) } == true
                val sortOrder = if (keepsExistingOrder && value?.sortOrder != null) {
                    value.sortOrder
                } else {
                    nextScheduleSortOrder(schedule, date, value?.id)
                }
                if (value == null) repository.createSchedule(profile, topic, details, date, sortOrder,
                    { finished(); showingEditor = false }, { finished(); error = it.message ?: instructorT("The class could not be saved.", "No se pudo guardar la clase.") })
                else repository.updateSchedule(profile, value.id, topic, details, date, sortOrder,
                    { finished(); showingEditor = false; editingId = null }, { finished(); error = it.message ?: instructorT("The class could not be saved.", "No se pudo guardar la clase.") })
            },
            onDelete = editing?.let { value ->
                { finished ->
                    repository.deleteSchedule(
                        profile,
                        value.id,
                        { finished(); showingEditor = false; editingId = null },
                        { problem -> finished(); error = problem.message ?: instructorT("The class could not be deleted.", "No se pudo eliminar la clase." ) },
                    )
                }
            },
        )
        else -> LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                TextButton(onClick = onBack) { Text(instructorT("‹ Back", "‹ Volver")) }
                InstructorCard {
                    Text(instructorT("Class Schedule", "Calendario de clases"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                    Text(instructorT("Create classes one at a time, or import a full schedule from a spreadsheet.", "Crea clases una por una o importa un calendario completo desde una hoja de cálculo."), color = IlluminedThemeTokens.SecondaryText)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { editingId = null; showingEditor = true }, enabled = classId.isNotBlank(), modifier = Modifier.weight(1f)) { Text(instructorT("New Class", "Nueva clase")) }
                        OutlinedButton(onClick = { showingImport = true }, enabled = classId.isNotBlank(), modifier = Modifier.weight(1f)) { Text(instructorT("Import", "Importar")) }
                    }
                }
            }
            if (schedule.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStateSpec(instructorT("No Schedule Items", "No hay elementos en el calendario"), instructorT("Create your first class date for this group.", "Crea la primera fecha de clase para este grupo."), "calendar")) } }
            items(schedule.sortedWith(scheduleItemComparator), key = { it.id }) { value ->
                val dateText = value.date?.toDate()?.let { DateFormat.getDateInstance(DateFormat.FULL).format(it) }.orEmpty()
                InstructorListCard(
                    onClick = { editingId = value.id; showingEditor = true },
                    description = listOf(value.topic, dateText, value.details).filter { it.isNotBlank() }.joinToString(". "),
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
                        InstructorSymbol(if(isTomorrow(value.date?.toDate()?.time ?: 0L)) InstructorSymbolKind.CalendarClock else InstructorSymbolKind.Calendar, IlluminedThemeTokens.Gold, Modifier.padding(end=14.dp).size(36.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(value.topic, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(dateText, color = IlluminedThemeTokens.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (value.details.isNotBlank()) Text(value.details, color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp, maxLines = 2)
                        }
                        InstructorSymbol(InstructorSymbolKind.Chevron, IlluminedThemeTokens.SecondaryText, Modifier.size(10.dp,18.dp))
                    }
                }
            }
        }
    }
    InstructorErrorAlert(instructorT(InstructorErrorPresentation.ScheduleTitle, "Error del calendario"), error) { error = null }
}

private fun sameScheduleDay(firstMillis: Long, secondMillis: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
    return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

private fun nextScheduleSortOrder(schedule: List<ScheduleItem>, dateMillis: Long, excludingId: String?): Long {
    val sameDay = schedule.filter { item ->
        item.id != excludingId && item.date?.toDate()?.time?.let { sameScheduleDay(it, dateMillis) } == true
    }
    val highestOrder = sameDay.mapNotNull { it.sortOrder }.maxOrNull() ?: -1L
    return maxOf(highestOrder + 1L, sameDay.size.toLong())
}

@Composable
private fun ScheduleEditor(value: ScheduleItem?, onCancel: () -> Unit, onSave: (String, String, Long, () -> Unit) -> Unit, onDelete: (((() -> Unit) -> Unit))?) {
    val context = LocalContext.current
    var topic by rememberSaveable(value?.id) { mutableStateOf(value?.topic.orEmpty()) }
    var details by rememberSaveable(value?.id) { mutableStateOf(value?.details.orEmpty()) }
    var date by rememberSaveable(value?.id) { mutableLongStateOf(value?.date?.toDate()?.time ?: System.currentTimeMillis()) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    BackHandler { if (InstructorEditorOperationPolicy.canInteract(saving)) onCancel() }
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onCancel, enabled = InstructorEditorOperationPolicy.canInteract(saving)) { Text(instructorT("‹ Cancel", "‹ Cancelar")) }; Text(if (value == null) instructorT("New Class", "Nueva clase") else instructorT("Edit Class", "Editar clase"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
        item {
            InstructorCard {
                OutlinedButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = date }
                    DatePickerDialog(context, { _, y, m, d -> c.set(y, m, d, 0, 0, 0); c.set(Calendar.MILLISECOND, 0); date = c.timeInMillis }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }, Modifier.fillMaxWidth()) { Text("${instructorT("Class Date", "Fecha de la clase")}  ·  ${DateFormat.getDateInstance().format(java.util.Date(date))}") }
                OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth(), label = { Text(instructorT("Topic", "Tema")) })
                OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), label = { Text(instructorT("Optional details", "Detalles opcionales")) }, minLines = 3, maxLines = 7)
            }
        }
        item { Button(onClick = { saving = true; onSave(topic, details, date) { saving = false } }, enabled = topic.isNotBlank() && InstructorEditorOperationPolicy.canInteract(saving), modifier = Modifier.fillMaxWidth()) { Text(if (saving) instructorT("Saving...", "Guardando...") else instructorT("Save Class", "Guardar clase")) } }
        if (onDelete != null) item { OutlinedButton(onClick = { confirmingDelete = true }, enabled = InstructorEditorOperationPolicy.canInteract(saving), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text(instructorT("Delete Class", "Eliminar clase")) } }
    }
    if (confirmingDelete && onDelete != null) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text(instructorT("Delete this class date?", "¿Eliminar esta fecha de clase?")) },
        confirmButton = {
            TextButton(onClick = {
                confirmingDelete = false
                saving = true
                onDelete { saving = false }
            }) { Text(instructorT("Delete", "Eliminar"), color = Color.Red) }
        },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(instructorT("Cancel", "Cancelar")) } },
    )
}

@Composable
private fun ScheduleImportScreen(existing: List<ScheduleItem>, onCancel: () -> Unit, onImport: (List<ImportedScheduleRow>, Boolean, () -> Unit) -> Unit) {
    var csv by rememberSaveable { mutableStateOf("date,topic,details\n2026-09-03,Welcome Night,Introductions and overview\n2026-09-10,The Kerygma,The first proclamation of the Gospel") }
    var preview by remember { mutableStateOf(emptyList<ImportedScheduleRow>()) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var replace by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    BackHandler { if (InstructorEditorOperationPolicy.canInteract(saving)) onCancel() }
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onCancel, enabled = !saving) { Text(instructorT("‹ Cancel", "‹ Cancelar")) } }
        item { InstructorCard { Text(instructorT("Import Full Schedule", "Importar calendario completo"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(instructorT("Copy rows from Numbers, Excel, or Google Sheets, paste them below, preview the classes, then import them.", "Copia filas de Numbers, Excel o Google Sheets, pégalas abajo, previsualiza las clases y luego impórtalas."), color = IlluminedThemeTokens.SecondaryText); Text(instructorT("Expected columns", "Columnas esperadas"), fontWeight = FontWeight.SemiBold); Text("date, topic, details", color = IlluminedThemeTokens.Gold, fontWeight = FontWeight.SemiBold); Text(instructorT("Details are optional. Dates can be 2026-09-03 or 9/3/2026.", "Los detalles son opcionales. Las fechas pueden escribirse como 2026-09-03 o 9/3/2026."), color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp) } }
        item {
            InstructorCard {
                Text(instructorT("Paste Schedule", "Pegar calendario"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(csv, { csv = it; preview = emptyList(); parseError = null }, Modifier.fillMaxWidth().heightIn(min = 170.dp), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp), minLines = 7)
                OutlinedButton(onClick = {
                    when (val result = ScheduleImportParser.parse(csv)) {
                        is ScheduleParseResult.Success -> { preview = result.rows; parseError = null }
                        is ScheduleParseResult.Failure -> { preview = emptyList(); parseError = result.message }
                    }
                }, enabled = csv.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(instructorT("Preview Schedule", "Previsualizar calendario")) }
                parseError?.let { Text(it, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (preview.isNotEmpty()) item {
            InstructorCard {
                Text(instructorT("Preview", "Vista previa"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (Locale.getDefault().language == "es") "${preview.size} fechas de clase listas para importar." else "${preview.size} class dates ready to import.", color = IlluminedThemeTokens.SecondaryText)
                Row(verticalAlignment = Alignment.CenterVertically) { Text(instructorT("Replace existing schedule", "Reemplazar el calendario existente"), fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Switch(replace, { replace = it }) }
                Text(if (replace) { if (Locale.getDefault().language == "es") "Esto eliminará los ${existing.size} elementos actuales del calendario de esta clase y usará las filas importadas." else "This will remove the current ${existing.size} schedule items for this class and use the imported rows instead." } else instructorT("This will add the imported rows to the schedule you already have.", "Esto añadirá las filas importadas al calendario que ya tienes."), color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp)
                preview.forEach { row ->
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(.72f)) { Column(Modifier.fillMaxWidth().padding(10.dp)) { Text(row.topic, fontWeight = FontWeight.SemiBold); Text(row.date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, uuuu")), color = IlluminedThemeTokens.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); if (row.details.isNotBlank()) Text(row.details, color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp, maxLines = 2) } }
                }
            }
        }
        if (preview.isNotEmpty()) item { Button(onClick = { saving = true; onImport(preview, replace) { saving = false } }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(if (saving) instructorT("Importing…", "Importando…") else instructorT("Import Schedule", "Importar calendario")) } }
    }
}

@Composable
private fun AssignmentManager(profile: UserProfile, assignments: List<Assignment>, schedule: List<ScheduleItem>, onBack: () -> Unit) {
    val repository = remember { InstructorRepository() }; val classId = profile.selectedClassId; var values by remember { mutableStateOf(assignments) }; var students by remember { mutableStateOf(emptyList<UserProfile>()) }; var completions by remember { mutableStateOf(emptyList<AssignmentCompletion>()) }; var editorId by rememberSaveable { mutableStateOf<String?>(null) }; val editor = restoreEditedRecord(values, editorId, Assignment::id); var creating by rememberSaveable { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = creating || editorId != null) {
        creating = false
        editorId = null
    }
    DisposableEffect(classId) {
        val listeners = mutableListOf<ListenerRegistration>()
        if (classId.isNotBlank()) {
            listeners += repository.listenAssignments(classId, { values = it }, { problem -> error = InstructorErrorPresentation.message(problem, instructorT("Assignments could not be loaded.", "No se pudieron cargar las tareas.")) })
            listeners += repository.listenStudents(classId, { students = it.sortedBy { student -> student.displayName.lowercase() } }, { problem -> error = InstructorErrorPresentation.message(problem, instructorT("Student details could not be loaded.", "No se pudieron cargar los detalles de estudiantes.")) }, includeRoster = true)
            listeners += repository.listenAssignmentCompletions(classId, { completions = it }, { problem -> error = InstructorErrorPresentation.message(problem, instructorT("Assignment completions could not be loaded.", "No se pudieron cargar las tareas completadas.")) })
        }
        onDispose { listeners.forEach { it.remove() } }
    }
    val today = remember { Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }.timeInMillis }
    val nextClass = schedule.filter { (it.date?.toDate()?.time ?: Long.MIN_VALUE) > today }.minByOrNull { it.date?.seconds ?: Long.MAX_VALUE }
    val readinessAssignments = values.filter { assignment -> assignment.isActive && nextClass != null && (assignment.dueAt?.toDate()?.time ?: Long.MAX_VALUE) in today..nextClass.date!!.toDate().time }
    val readiness = InstructorReadinessCalculator.classReadiness(readinessAssignments, students, completions)
    if (creating || editor != null) AssignmentEditor(editor, onCancel = { creating = false; editorId = null }, onSave = { title, instructions, due, links, readings, active, finished ->
        if (editor == null) repository.createAssignment(profile, title, instructions, due, links, readings, { finished(); creating = false }, { problem -> finished(); error = problem.localizedMessage ?: instructorT("Assignment could not be saved.", "No se pudo guardar la tarea.") })
        else repository.updateAssignment(profile, editor.id, title, instructions, due, links, readings, active, { finished(); editorId = null }, { problem -> finished(); error = problem.localizedMessage ?: instructorT("Assignment could not be saved.", "No se pudo guardar la tarea.") })
    }, onDelete = editor?.let { value ->
        { finished ->
            repository.deleteAssignment(
                profile,
                value.id,
                { finished(); editorId = null },
                { problem -> finished(); error = problem.localizedMessage ?: instructorT("Assignment could not be deleted.", "No se pudo eliminar la tarea.") },
            )
        }
    })
    else LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ManagerHeader(instructorT("Assignments", "Tareas"), instructorT("Post readings, lesson work, and preparation tasks for students.", "Publica lecturas, lecciones y tareas de preparación para los estudiantes."), onBack, classId.isNotBlank()) { creating = true } }
        item { InstructorCard {
            Text(if (nextClass?.date?.toDate()?.let { isTomorrow(it.time) } == true) instructorT("Tomorrow's Class Readiness", "Preparación para la clase de mañana") else instructorT("Next Class Readiness", "Preparación para la próxima clase"), fontSize=18.sp,fontWeight=FontWeight.SemiBold)
            if(nextClass==null) Text(instructorT("Add a class schedule item to activate readiness tracking.", "Añade una clase al calendario para activar el seguimiento de preparación."),color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)
            else Text("${nextClass.topic} · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(nextClass.date!!.toDate())}",color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)
            if(readiness.totalChecks==0) Text(instructorT("No assignments are due before the next class yet.", "Todavía no hay tareas con fecha límite antes de la próxima clase."),color=IlluminedThemeTokens.SecondaryText)
            else { Row { Text(if(Locale.getDefault().language=="es")"${readiness.percent}% preparados" else "${readiness.percent}% ready",fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue);Spacer(Modifier.weight(1f));Text(if(Locale.getDefault().language=="es")"${readiness.completedChecks}/${readiness.totalChecks} verificaciones" else "${readiness.completedChecks}/${readiness.totalChecks} checks",color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp,fontWeight=FontWeight.SemiBold) }; LinearProgressIndicator(progress={readiness.fraction},Modifier.fillMaxWidth(),color=if(readiness.percent>=80)IlluminedThemeTokens.Blue else IlluminedThemeTokens.Gold); if(nextClass?.date?.toDate()?.let{isTomorrow(it.time)}==true&&readiness.percent<80)Text(instructorT("Readiness alert: follow up with students who still have assignments unchecked.", "Alerta de preparación: comunícate con los estudiantes que todavía tienen tareas sin completar."),color=IlluminedThemeTokens.Gold,fontSize=13.sp,fontWeight=FontWeight.SemiBold) }
        } }
        if (values.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStateSpec(instructorT("No Assignments", "No hay tareas"), instructorT("Create your first assignment for this class.", "Crea la primera tarea para esta clase."), "checklist")) } }
        items(values, key = { it.id }) { item ->
            val progress=InstructorReadinessCalculator.assignmentProgress(item.id,students,completions)
            val visibilityDescription = if (item.isActive) instructorT("Visible to students", "Visible para los estudiantes") else instructorT("Hidden from students", "Oculta para los estudiantes")
            val completionDescription = if (Locale.getDefault().language == "es") "${progress.completedCount} de ${progress.totalStudents} completaron" else "${progress.completedCount} of ${progress.totalStudents} completed"
            InstructorListCard(onClick = { editorId = item.id }, description = "${item.title}. $visibilityDescription. $completionDescription") {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){InstructorSymbol(if(item.isActive)InstructorSymbolKind.Active else InstructorSymbolKind.Paused,if(item.isActive)IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,Modifier.size(20.dp));Spacer(Modifier.width(9.dp));Text(item.title,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));InstructorSymbol(InstructorSymbolKind.Chevron,IlluminedThemeTokens.SecondaryText,Modifier.size(10.dp,18.dp))}
                    Text("${instructorT("Due", "Fecha límite")}: ${item.dueAt?.toDate()?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(it) }.orEmpty()}",color=IlluminedThemeTokens.Blue,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                    item.lessonLinks.take(3).forEach{link->InstructorMetadataRow(InstructorSymbolKind.Book,link.lessonTitle.ifBlank{link.lessonId})}
                    if(item.lessonLinks.size>3)Text(if(Locale.getDefault().language=="es") "+ ${item.lessonLinks.size-3} lecciones más" else "+ ${item.lessonLinks.size-3} more lessons",color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp)
                    if(item.readings.isNotEmpty()){InstructorMetadataRow(InstructorSymbolKind.Document,if(Locale.getDefault().language=="es") "${item.readings.size} lectura${if(item.readings.size==1)"" else "s"}" else "${item.readings.size} reading${if(item.readings.size==1)"" else "s"}");item.readings.forEach{reading->val readingProgress=InstructorReadinessCalculator.assignmentProgress("${item.id}__reading__${reading.id}",students,completions);Text("${reading.title}: ${readingProgress.completedCount}/${readingProgress.totalStudents}",color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,maxLines=1)}}
                    if(item.instructions.isNotBlank())Text(item.instructions,color=IlluminedThemeTokens.SecondaryText,maxLines=3)
                    Text(if(item.isActive)instructorT("Visible to students", "Visible para los estudiantes") else instructorT("Hidden from students", "Oculta para los estudiantes"),color=if(item.isActive)IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                    Row{Text(instructorT("Completed", "Completada"),color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text("${progress.completedCount}/${progress.totalStudents}",color=IlluminedThemeTokens.Blue,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}
                    LinearProgressIndicator(progress={progress.fraction},Modifier.fillMaxWidth(),color=IlluminedThemeTokens.Gold)
                    if(progress.incompleteNames.isNotEmpty())Text(if(Locale.getDefault().language=="es") "Aún faltan ${progress.incompleteNames.take(3).joinToString()}" else "Still waiting on ${progress.incompleteNames.take(3).joinToString()}",color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,maxLines=2)
                }
            }
        }
    }
    InstructorErrorAlert(instructorT(InstructorErrorPresentation.AssignmentTitle, "Error de la tarea"), error) { error = null }
}

@Composable
private fun AssignmentEditor(value: Assignment?, onCancel: () -> Unit, onSave: (String,String,Long,List<AssignmentLessonLink>,List<AssignmentReading>,Boolean,() -> Unit) -> Unit, onDelete: (((() -> Unit) -> Unit))?) {
    val context = LocalContext.current
    val categories = remember { LessonCatalog.load(context).getOrNull().orEmpty() }
    val allLessons = remember(categories) { categories.flatMap { it.lessons } }
    var title by rememberSaveable(value?.id) { mutableStateOf(value?.title.orEmpty()) }
    var instructions by rememberSaveable(value?.id) { mutableStateOf(value?.instructions.orEmpty()) }
    var due by rememberSaveable(value?.id) { mutableLongStateOf(value?.dueAt?.toDate()?.time ?: System.currentTimeMillis()) }
    var selectedIds by rememberSaveable(value?.id) { mutableStateOf(value?.lessonLinks?.map { it.lessonId }.orEmpty()) }
    var expandedCategories by remember { mutableStateOf(emptySet<String>()) }
    var readings by rememberSaveable(value?.id, stateSaver = assignmentReadingsSaver) { mutableStateOf(value?.readings.orEmpty()) }
    var active by rememberSaveable(value?.id) { mutableStateOf(value?.isActive ?: true) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    BackHandler { if (InstructorEditorOperationPolicy.canInteract(saving)) onCancel() }
    val partialReading = readings.any { it.title.isBlank() != it.text.isBlank() }
    val links = allLessons.filter { it.id in selectedIds }.map { AssignmentLessonLink(it.id, it.title) }
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onCancel, enabled = InstructorEditorOperationPolicy.canInteract(saving)) { Text(instructorT("‹ Cancel", "‹ Cancelar")) }; Text(if (value == null) instructorT("New Assignment", "Nueva tarea") else instructorT("Edit Assignment", "Editar tarea"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
        item { InstructorCard {
            OutlinedButton(onClick = { val c=Calendar.getInstance().apply { timeInMillis=due };DatePickerDialog(context,{_,y,m,d->c.set(y,m,d,0,0,0);c.set(Calendar.MILLISECOND,0);due=c.timeInMillis},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show() },Modifier.fillMaxWidth()){Text("${instructorT("Due Date", "Fecha de entrega")}  ·  ${DateFormat.getDateInstance().format(java.util.Date(due))}")}
            OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text(instructorT("Title", "Título"))})
            OutlinedTextField(instructions,{instructions=it},Modifier.fillMaxWidth(),label={Text(instructorT("Instructions", "Instrucciones"))},minLines=4,maxLines=8)
            HorizontalDivider()
            Text(instructorT("Optional Lesson Links", "Enlaces opcionales a lecciones"), fontWeight = FontWeight.SemiBold)
            Text(if (selectedIds.isEmpty()) instructorT("No linked lessons selected.", "No hay lecciones enlazadas seleccionadas.") else if(Locale.getDefault().language=="es")"${selectedIds.size} lecciones seleccionadas" else "${selectedIds.size} lesson${if(selectedIds.size==1)"" else "s"} selected", color = if(selectedIds.isEmpty()) IlluminedThemeTokens.SecondaryText else IlluminedThemeTokens.Blue, fontSize = 13.sp)
            categories.forEach { category ->
                val expanded = category.name in expandedCategories
                val count = category.lessons.count { it.id in selectedIds }
                val localizedCategoryName = when (category.name) {
                    "Profession of Faith" -> instructorT(category.name, "Profesión de fe")
                    "Celebration of the Christian Mysteries" -> instructorT(category.name, "Celebración del misterio cristiano")
                    "Life in Christ" -> instructorT(category.name, "La vida en Cristo")
                    "Christian Prayer" -> instructorT(category.name, "La oración cristiana")
                    else -> category.name
                }
                Surface(onClick = { expandedCategories = if(expanded) expandedCategories-category.name else expandedCategories+category.name }, color = IlluminedThemeTokens.Cream, shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment=Alignment.CenterVertically) { InstructorSymbol(if(expanded)InstructorSymbolKind.Collapse else InstructorSymbolKind.Expand,IlluminedThemeTokens.Blue,Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Column { Text(localizedCategoryName, fontWeight=FontWeight.SemiBold, fontSize=15.sp); Text(if(Locale.getDefault().language=="es") { if(count==0) "${category.lessons.size} lecciones" else "$count de ${category.lessons.size} seleccionadas" } else { if(count==0) "${category.lessons.size} lessons" else "$count of ${category.lessons.size} selected" }, color=IlluminedThemeTokens.SecondaryText, fontSize=12.sp) } } }
                if(expanded) Column(Modifier.padding(start=12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) { category.lessons.forEach { lesson -> val selected=lesson.id in selectedIds; Surface(onClick={selectedIds=if(selected)selectedIds-lesson.id else selectedIds+lesson.id},color=if(selected)IlluminedThemeTokens.Blue.copy(.08f) else Color.White.copy(.72f),shape=RoundedCornerShape(10.dp)){Row(Modifier.fillMaxWidth().padding(10.dp)){Text(if(selected)"●" else "○",color=if(selected)IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText);Spacer(Modifier.width(10.dp));Text(lesson.title,fontSize=14.sp,modifier=Modifier.weight(1f))}} } }
            }
            HorizontalDivider()
            Text(instructorT("Optional Assigned Readings", "Lecturas asignadas opcionales"), fontWeight = FontWeight.SemiBold)
            Text(instructorT("Add one or more readings when you want students to open and complete text-based assignments.", "Añade una o más lecturas cuando quieras que los estudiantes abran y completen tareas de texto."), color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)
            if(readings.isEmpty()) Text(instructorT("No readings added.", "No se añadieron lecturas."),color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)
            readings.forEachIndexed { index, reading -> Surface(color=Color.White.copy(.72f),shape=RoundedCornerShape(12.dp)){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(instructorT("Reading", "Lectura"),color=IlluminedThemeTokens.Blue,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));TextButton(onClick={readings=readings.filterIndexed{i,_->i!=index}}){Text(instructorT("Remove", "Quitar"),color=Color.Red)}};OutlinedTextField(reading.title,{new->readings=readings.toMutableList().also{it[index]=reading.copy(title=new)}},Modifier.fillMaxWidth(),label={Text(instructorT("Reading Title", "Título de la lectura"))});OutlinedTextField(reading.text,{new->readings=readings.toMutableList().also{it[index]=reading.copy(text=new)}},Modifier.fillMaxWidth(),label={Text(instructorT("Paste full reading text", "Pega el texto completo de la lectura"))},minLines=8,maxLines=16)}} }
            OutlinedButton(onClick={readings=readings+AssignmentReading(java.util.UUID.randomUUID().toString(),"","")},Modifier.fillMaxWidth()){Text(instructorT("Add Reading", "Añadir lectura"))}
            if(partialReading) Text(instructorT("Please add both a title and full text for each reading.", "Añade un título y el texto completo para cada lectura."),color=Color.Red,fontSize=13.sp)
            Row(verticalAlignment=Alignment.CenterVertically){Text(instructorT("Visible to Students", "Visible para los estudiantes"),fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Switch(active,{active=it})}
        } }
        item { Button(onClick={saving=true;onSave(title,instructions,due,links,readings,active){saving=false}},enabled=title.isNotBlank()&&!partialReading&&InstructorEditorOperationPolicy.canInteract(saving),modifier=Modifier.fillMaxWidth()){Text(if(saving)instructorT("Saving...", "Guardando...") else instructorT("Save Assignment", "Guardar tarea"))} }
        if(onDelete!=null)item{OutlinedButton(onClick={confirmingDelete=true},enabled=InstructorEditorOperationPolicy.canInteract(saving),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.Red)){Text(instructorT("Delete Assignment", "Eliminar tarea"))}}
    }
    if (confirmingDelete && onDelete != null) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text(instructorT("Delete this assignment?", "¿Eliminar esta tarea?")) },
        confirmButton = {
            TextButton(onClick = {
                confirmingDelete = false
                saving = true
                onDelete { saving = false }
            }) { Text(instructorT("Delete", "Eliminar"), color = Color.Red) }
        },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(instructorT("Cancel", "Cancelar")) } },
    )
}

@Composable
private fun DiscussionManager(profile: UserProfile, assignments: List<Assignment>, onBack: () -> Unit) {
    val repository = remember { InstructorRepository() }
    val classId = profile.selectedClassId
    var prompts by remember { mutableStateOf(emptyList<DiscussionPrompt>()) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    val editor = restoreEditedRecord(prompts, editorId, DiscussionPrompt::id)
    var creating by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = creating || editorId != null) {
        creating = false
        editorId = null
    }

    DisposableEffect(classId) {
        val listener = if (classId.isBlank()) null else repository.listenDiscussionPrompts(
            classId,
            update = { prompts = it },
            error = { error = it.localizedMessage ?: instructorT("Discussion boards could not be loaded.", "No se pudieron cargar los foros de discusión.") },
        )
        onDispose { listener?.remove() }
    }

    if (creating || editor != null) {
        DiscussionEditor(
            value = editor,
            assignments = assignments,
            blockedAssignmentIds = prompts.filter { it.id != editor?.id }.map { it.assignmentId }.filter { it.isNotBlank() }.toSet(),
            onCancel = { creating = false; editorId = null },
            onSave = { title, prompt, assignmentId, assignmentTitle, required, active, finished ->
                val failed: (Throwable) -> Unit = {
                    finished()
                    error = it.localizedMessage ?: instructorT("Discussion could not be saved.", "No se pudo guardar la discusión.")
                }
                if (editor == null) repository.createDiscussion(
                    profile, title, prompt, assignmentId, assignmentTitle, required, active,
                    { finished(); creating = false }, failed,
                ) else repository.updateDiscussion(
                    profile, editor.id, title, prompt, assignmentId, assignmentTitle, required, active,
                    { finished(); editorId = null }, failed,
                )
            },
            onDelete = editor?.let { value ->
                { finished ->
                    repository.deleteDiscussion(profile, value.id, { finished(); editorId = null }, {
                        finished()
                        error = it.localizedMessage ?: instructorT("Discussion could not be deleted.", "No se pudo eliminar la discusión.")
                    })
                }
            },
        )
    } else {
        LazyColumn(
            Modifier.fillMaxSize().background(instructorBrush()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ManagerHeader(instructorT("Discussion Boards", "Foros de discusión"), instructorT("Create discussion prompts as the final step of an assignment.", "Crea consignas de discusión como el paso final de una tarea."), onBack, classId.isNotBlank()) { creating = true } }
            if (prompts.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStateSpec(instructorT("No Discussion Boards", "No hay foros de discusión"), instructorT("Create your first assignment-linked discussion prompt.", "Crea la primera consigna de discusión vinculada a una tarea."), "text.bubble")) } }
            items(prompts, key = { it.id }) { item ->
                InstructorListCard(
                    onClick = { editorId = item.id },
                    description = "${item.title}. ${if (item.isVisible) instructorT("Visible to students", "Visible para los estudiantes") else instructorT("Hidden from students", "Oculta para los estudiantes")}. ${item.assignmentTitle.ifBlank { item.lessonTitle }}",
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            InstructorSymbol(if(item.isVisible)InstructorSymbolKind.Bubble else InstructorSymbolKind.EyeSlash,if(item.isVisible)IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,Modifier.size(21.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){InstructorSymbol(InstructorSymbolKind.Book,IlluminedThemeTokens.Gold,Modifier.size(14.dp));Text(item.assignmentTitle.ifBlank { item.lessonTitle }, color = IlluminedThemeTokens.Gold, fontSize = 13.sp, maxLines = 2)}
                            }
                            InstructorSymbol(InstructorSymbolKind.Chevron,IlluminedThemeTokens.SecondaryText,Modifier.size(10.dp,18.dp))
                        }
                        Text(item.prompt, color = IlluminedThemeTokens.SecondaryText, maxLines = 3)
                        Text(
                            if (item.isVisible) instructorT("Visible to students", "Visible para los estudiantes") else instructorT("Hidden from students", "Oculta para los estudiantes"),
                            color = if (item.isVisible) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(instructorT("Discussion Error", "Error de discusión")) },
            text = { Text(localizedUserMessage(message)) },
            confirmButton = { TextButton(onClick = { error = null }) { Text(instructorT("OK", "Aceptar")) } },
        )
    }
}

@Composable
private fun DiscussionEditor(
    value: DiscussionPrompt?,
    assignments: List<Assignment>,
    blockedAssignmentIds: Set<String>,
    onCancel: () -> Unit,
    onSave: (String, String, String, String, Boolean, Boolean, () -> Unit) -> Unit,
    onDelete: (((() -> Unit) -> Unit))?,
) {
    var title by rememberSaveable(value?.id) { mutableStateOf(value?.title.orEmpty()) }
    var prompt by rememberSaveable(value?.id) { mutableStateOf(value?.prompt.orEmpty()) }
    var assignmentId by rememberSaveable(value?.id) { mutableStateOf(value?.assignmentId.orEmpty()) }
    var active by rememberSaveable(value?.id) { mutableStateOf(value?.isVisible ?: true) }
    var saving by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    BackHandler { if (!saving) onCancel() }
    val assignment = assignments.firstOrNull { it.id == assignmentId }
    val assignmentBlocked = assignmentId in blockedAssignmentIds
    val canSave = InstructorDiscussionPolicy.canSave(title, prompt, !assignmentBlocked, saving)

    LazyColumn(
        Modifier.fillMaxSize().background(instructorBrush()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onCancel, enabled = !saving) { Text(instructorT("‹ Cancel", "‹ Cancelar")) }
            Text(if (value == null) instructorT("New Discussion", "Nueva discusión") else instructorT("Edit Discussion", "Editar discusión"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        }
        item {
            InstructorCard {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(instructorT("Discussion Title", "Título de la discusión")) }, singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text(instructorT("Discussion Prompt", "Consigna de discusión")) }, minLines = 5, maxLines = 10)
                HorizontalDivider()
                Text(instructorT("Linked Assignment", "Tarea vinculada"), fontWeight = FontWeight.SemiBold)
                Text(assignment?.title ?: instructorT("Standalone discussion — available immediately.", "Discusión independiente — disponible de inmediato."), color = if (assignment == null) IlluminedThemeTokens.SecondaryText else IlluminedThemeTokens.Blue, fontSize = 13.sp)
                if (assignmentBlocked) Text(instructorT("That assignment already has a discussion step.", "Esa tarea ya tiene un paso de discusión."), color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Surface(onClick = { assignmentId = "" }, color = if (assignmentId.isBlank()) IlluminedThemeTokens.Blue.copy(.08f) else Color.White.copy(.72f), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(if (assignmentId.isBlank()) "●" else "○", color = IlluminedThemeTokens.Blue)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(instructorT("Standalone Discussion", "Discusión independiente"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(instructorT("Available immediately and not part of an assignment.", "Disponible de inmediato y sin formar parte de una tarea."), fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                        }
                    }
                }
                assignments.forEach { option ->
                    val selected = option.id == assignmentId
                    Surface(onClick = { assignmentId = option.id }, color = if (selected) IlluminedThemeTokens.Blue.copy(.08f) else Color.White.copy(.72f), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(if (selected) "●" else "○", color = if (selected) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText)
                            Spacer(Modifier.width(10.dp))
                            Text(option.title, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Text(instructorT("Visible to Students", "Visible para los estudiantes"), fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Switch(active, { active = it }, enabled = !saving) }
            }
        }
        item {
            Button(
                onClick = { saving = true; onSave(title, prompt, assignmentId, assignment?.title.orEmpty(), assignment != null, active) { saving = false } },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saving) instructorT("Saving...", "Guardando...") else instructorT("Save Discussion", "Guardar discusión")) }
        }
        if (onDelete != null) item {
            OutlinedButton(onClick = { confirmingDelete = true }, enabled = !saving, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text(instructorT("Delete Discussion", "Eliminar discusión")) }
        }
    }
    if (confirmingDelete && onDelete != null) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text(instructorT("Delete this discussion?", "¿Eliminar esta discusión?")) },
        confirmButton = { TextButton(onClick = { confirmingDelete = false; saving = true; onDelete { saving = false } }) { Text(instructorT("Delete", "Eliminar"), color = Color.Red) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(instructorT("Cancel", "Cancelar")) } },
    )
}

@Composable
private fun StudentProgressManager(profile: UserProfile, onBack: () -> Unit) {
    val repository = remember { InstructorRepository() }
    val context = LocalContext.current
    val classId = profile.selectedClassId
    val totalLessons = remember { LessonCatalog.load(context.applicationContext).getOrNull().orEmpty().sumOf { it.lessons.size } }
    val prayerNamesById = remember { CommonPrayerCatalog.namesById(context.applicationContext) }
    var students by remember(classId) { mutableStateOf(emptyList<UserProfile>()) }
    var search by remember(classId) { mutableStateOf("") }
    var filter by remember(classId) { mutableStateOf("Active") }
    var completions by remember { mutableStateOf(emptyList<AssignmentCompletion>()) }
    var selected by remember(classId) { mutableStateOf<UserProfile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selected != null) { selected = null }
    DisposableEffect(classId) {
        val listeners = mutableListOf<ListenerRegistration>()
        if (classId.isNotBlank()) {
            listeners += repository.listenStudents(classId, { students = it.sortedBy { student -> student.displayName.lowercase() } }, { problem -> error = InstructorErrorPresentation.message(problem, instructorT("Student progress could not be loaded.", "No se pudo cargar el progreso de los estudiantes.")) })
            listeners += repository.listenAssignmentCompletions(classId, { completions = it }, { problem -> error = InstructorErrorPresentation.message(problem, instructorT("Completed readings could not be loaded.", "No se pudieron cargar las lecturas completadas.")) })
        }
        onDispose { listeners.forEach { it.remove() } }
    }
    InstructorErrorAlert(instructorT("Student Progress Error", "Error de progreso de estudiantes"), error) { error = null }
    selected?.let { student ->
        StudentProgressDetail(students.firstOrNull { it.userId == student.userId } ?: student, classId, profile.userId, totalLessons, prayerNamesById, InstructorReadinessCalculator.completedReadingNames(student.userId, completions)) { selected = null }
        return
    }
    val active = students.filter { studentRosterStatus(it, classId) == "Active" }
    val visible = students.filter { (filter == "All" || studentRosterStatus(it, classId) == filter) && (search.isBlank() || it.displayName.contains(search, true) || it.email.contains(search, true)) }
    val average = if (active.isEmpty()) 0 else active.sumOf { it.completedLessons.size.coerceAtMost(totalLessons) } / active.size
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = onBack) { Text(instructorT("‹ Back", "‹ Atrás")) }
            InstructorCard {
                Text(instructorT("Student Details", "Detalles de estudiantes"), fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)
                Text(instructorT("Review progress and manage your classroom roster. Accounts and progress are preserved when students are removed.", "Consulta el progreso y administra tu clase. Las cuentas y el progreso se conservan al retirar a estudiantes."),color=IlluminedThemeTokens.SecondaryText)
                OutlinedTextField(value=search,onValueChange={search=it},label={Text(instructorT("Search name or email", "Buscar nombre o correo"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
                androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    items(listOf("Active","Inactive","Removed","All")) { option ->
                        FilterChip(selected=filter==option,onClick={filter=option},label={Text(studentRosterLabel(option),fontSize=12.sp)})
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { ProgressStatPill(instructorT("Students", "Estudiantes"),"${active.size}",IlluminedThemeTokens.Blue,Modifier.weight(1f));ProgressStatPill(instructorT("Avg. Lessons", "Promedio de lecciones"),"$average",IlluminedThemeTokens.Gold,Modifier.weight(1f)) }
            }
        }
        if (visible.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStateSpec(instructorT("No Students Found", "No se encontraron estudiantes"), instructorT("Students will appear here after they join this class.", "Los estudiantes aparecerán aquí después de unirse a esta clase."), "person.3")) } }
        items(visible,key={it.userId.ifBlank{it.displayName}}) { student ->
            val completed=student.completedLessons.size.coerceAtMost(totalLessons);val fraction=if(totalLessons==0)0f else completed.toFloat()/totalLessons
            val prayers=student.memorizedPrayerIds.mapNotNull{prayerNamesById[it]}.sortedBy{it.lowercase()};val readings=InstructorReadinessCalculator.completedReadingNames(student.userId,completions)
            val progressDescription = if(Locale.getDefault().language=="es") "${student.displayName}. $completed de $totalLessons lecciones. ${student.earnedBadges.size} insignias. ${prayers.size} oraciones. ${readings.size} lecturas" else "${student.displayName}. $completed of $totalLessons lessons. ${student.earnedBadges.size} badges. ${prayers.size} prayers. ${readings.size} readings"
            InstructorListCard(onClick={selected=student},description=progressDescription){
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Row(verticalAlignment=Alignment.Top){MemberProfilePhoto(student.userId, 40);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(student.displayName,fontSize=18.sp,fontWeight=FontWeight.SemiBold);if(student.email.isNotBlank())Text(student.email,color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,maxLines=1)};Text("$completed/$totalLessons",color=IlluminedThemeTokens.Blue,fontWeight=FontWeight.SemiBold)}
                    Text(studentRosterLabel(studentRosterStatus(student,classId)),color=IlluminedThemeTokens.Blue,fontSize=13.sp)
                    LinearProgressIndicator(progress={fraction},Modifier.fillMaxWidth(),color=IlluminedThemeTokens.Gold)
                    Row{InstructorMetadataRow(InstructorSymbolKind.Rosette,if(Locale.getDefault().language=="es") "${student.earnedBadges.size} insignias" else "${student.earnedBadges.size} badges",IlluminedThemeTokens.SecondaryText);Spacer(Modifier.weight(1f));InstructorMetadataRow(InstructorSymbolKind.Book,if(Locale.getDefault().language=="es") "${prayers.size} oraciones" else "${prayers.size} prayers",IlluminedThemeTokens.SecondaryText)}
                    InstructorMetadataRow(InstructorSymbolKind.Document,if(Locale.getDefault().language=="es") "${readings.size} lecturas" else "${readings.size} readings",IlluminedThemeTokens.SecondaryText)
                    if(prayers.isNotEmpty())Text(prayers.take(2).joinToString(),color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,maxLines=1)
                }
            }
        }
    }
}

@Composable
private fun StudentProgressDetail(student: UserProfile,classId:String,instructorId:String,totalLessons:Int,prayerNamesById:Map<String,String>,completedReadingNames:List<String>,onBack:()->Unit){
    val completed=student.completedLessons.size.coerceAtMost(totalLessons);val incomplete=(totalLessons-completed).coerceAtLeast(0);val fraction=if(totalLessons==0)0f else completed.toFloat()/totalLessons
    val prayers=student.memorizedPrayerIds.mapNotNull{prayerNamesById[it]}.sortedBy{it.lowercase()}
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{TextButton(onClick=onBack){Text(instructorT("‹ Student Details", "‹ Detalles de estudiantes"))};InstructorCard{MemberProfilePhoto(student.userId, 56);Text(student.displayName,fontSize=26.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue);if(student.email.isNotBlank())Text(student.email,color=IlluminedThemeTokens.SecondaryText);LinearProgressIndicator(progress={fraction},Modifier.fillMaxWidth(),color=IlluminedThemeTokens.Gold);Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){ProgressStatPill(instructorT("Completed", "Completadas"),"$completed",IlluminedThemeTokens.Blue,Modifier.weight(1f));ProgressStatPill(instructorT("Uncompleted", "Pendientes"),"$incomplete",IlluminedThemeTokens.Gold,Modifier.weight(1f))}}}
        item { StudentRosterControls(student,classId,instructorId,onBack) }
        item{InstructorCard{Text(instructorT("Formation Summary", "Resumen de formación"),fontSize=18.sp,fontWeight=FontWeight.SemiBold);ProgressDetailRow(instructorT("Badges Earned", "Insignias obtenidas"),"${student.earnedBadges.size}");ProgressDetailRow(instructorT("Rosary Mysteries", "Misterios del Rosario"),"${student.completedMysteries.size}");ProgressDetailRow(instructorT("Prayers Memorized", "Oraciones memorizadas"),"${prayers.size}");ProgressDetailRow(instructorT("Readings Completed", "Lecturas completadas"),"${completedReadingNames.size}");ProgressDetailRow(instructorT("Current Lesson Index", "Índice actual de lecciones"),"${student.currentLessonIndex}")}}
        item{InstructorCard{Text(instructorT("Memorized Prayers", "Oraciones memorizadas"),fontSize=18.sp,fontWeight=FontWeight.SemiBold);if(prayers.isEmpty())Text(instructorT("No memorized prayers yet.", "Todavía no hay oraciones memorizadas."),color=IlluminedThemeTokens.SecondaryText) else prayers.forEach{InstructorCheckRow(it)}}}
        item{InstructorCard{Text(instructorT("Completed Readings", "Lecturas completadas"),fontSize=18.sp,fontWeight=FontWeight.SemiBold);if(completedReadingNames.isEmpty())Text(instructorT("No completed readings yet.", "Todavía no hay lecturas completadas."),color=IlluminedThemeTokens.SecondaryText) else completedReadingNames.forEach{InstructorCheckRow(it)}}}
        item{InstructorCard{Text(instructorT("Completed Lesson IDs", "ID de lecciones completadas"),fontSize=18.sp,fontWeight=FontWeight.SemiBold);if(student.completedLessons.isEmpty())Text(instructorT("No completed lessons yet.", "Todavía no hay lecciones completadas."),color=IlluminedThemeTokens.SecondaryText) else student.completedLessons.sorted().forEach{Text(it,color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)}}}
    }
}

@Composable private fun ProgressStatPill(title:String,value:String,color:Color,modifier:Modifier=Modifier){Column(modifier.background(color.copy(.10f),RoundedCornerShape(12.dp)).padding(12.dp)){Text(value,fontSize=22.sp,fontWeight=FontWeight.Bold,color=color);Text(title,fontSize=12.sp,color=IlluminedThemeTokens.SecondaryText)}}
@Composable private fun ProgressDetailRow(title:String,value:String){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("●",color=IlluminedThemeTokens.Gold);Spacer(Modifier.width(10.dp));Text(title,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text(value,color=IlluminedThemeTokens.SecondaryText)}}
@Composable private fun InstructorCheckRow(label:String){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){InstructorSymbol(InstructorSymbolKind.CheckCircle,IlluminedThemeTokens.Blue,Modifier.size(16.dp));Text(label,color=IlluminedThemeTokens.Blue,fontSize=14.sp)}}
@Composable private fun InstructorMetadataRow(kind:InstructorSymbolKind,label:String,color:Color=IlluminedThemeTokens.Gold){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){InstructorSymbol(kind,color,Modifier.size(14.dp));Text(label,color=color,fontSize=12.sp,maxLines=1)}}

@Composable
private fun ManagerHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actionEnabled: Boolean,
    add: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text(instructorT("‹ Back", "‹ Atrás")) }
        InstructorCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InstructorSymbol(InstructorToolPresentation.managerSymbol(title),IlluminedThemeTokens.Blue,Modifier.size(22.dp))
                        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                    }
                    Text(subtitle, fontSize = 15.sp, lineHeight = 21.sp, color = IlluminedThemeTokens.SecondaryText)
                }
                val actionLabel = InstructorToolPresentation.managerAction(title)
                if (add != null && actionLabel != null) {
                    Button(
                        onClick = add,
                        enabled = actionEnabled,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){InstructorSymbol(InstructorSymbolKind.PlusCircle,Color.White,Modifier.size(18.dp));Text(actionLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)} }
                }
            }
        }
    }
}
@Composable private fun InstructorErrorAlert(title: String, message: String?, clear: () -> Unit) {
    message?.let {
        AlertDialog(
            onDismissRequest = clear,
            title = { Text(title) },
            text = { Text(localizedUserMessage(it)) },
            confirmButton = { TextButton(onClick = clear) { Text(instructorT("OK", "Aceptar")) } },
        )
    }
}
@Composable
private fun DailyFormationManager(profile: UserProfile, onBack: () -> Unit) {
    val repository = remember { InstructorRepository() }
    val classId = profile.selectedClassId
    var settings by remember { mutableStateOf(ManagedDailyFormationSettings()) }
    var entries by remember { mutableStateOf(emptyList<ManagedDailyFormationEntry>()) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingDate by rememberSaveable { mutableStateOf<String?>(null) }
    var importingCSV by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(classId) {
        val listener = repository.listenDailyFormationSettings(classId, { settings = it }, { error = it.localizedMessage })
        onDispose { listener.remove() }
    }
    DisposableEffect(classId) {
        val listener = repository.listenDailyFormationEntries(classId, { entries = it }, { error = it.localizedMessage })
        onDispose { listener.remove() }
    }

    val editing = entries.firstOrNull { it.date == editingDate }
    if (importingCSV) {
        DailyFormationCsvImportScreen(profile, onCancel = { importingCSV = false }, onImported = { count ->
            importingCSV = false; status = if(Locale.getDefault().language=="es") "$count entradas de Formación diaria publicadas." else "$count Daily Formation entries published."
        })
        return
    }
    if (creating || editing != null) {
        DailyFormationEntryEditor(profile, editing, onCancel = { creating = false; editingDate = null }, onSaved = {
            creating = false; editingDate = null; status = instructorT("Daily Formation entry saved.", "Entrada de Formación diaria guardada.")
        })
        return
    }

    var enabled by remember(settings) { mutableStateOf(settings.enabled) }
    var reminderTime by remember(settings) { mutableStateOf(settings.notificationTime) }
    var timeZone by remember(settings) { mutableStateOf(settings.timeZone) }
    LazyColumn(
        Modifier.fillMaxSize().background(instructorBrush()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text(instructorT("‹ Back", "‹ Atrás")) }
            InstructorCard {
                Text(instructorT("Daily Formation", "Formación diaria"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text(instructorT("Create entries one at a time, or import a full liturgical calendar from a spreadsheet.", "Crea entradas una por una o importa un calendario litúrgico completo desde una hoja de cálculo."), color = IlluminedThemeTokens.SecondaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { creating = true }, enabled = classId.isNotBlank(), modifier = Modifier.weight(1f)) { Text(instructorT("New Entry", "Nueva entrada")) }
                    OutlinedButton(onClick = { importingCSV = true }, enabled = classId.isNotBlank(), modifier = Modifier.weight(1f)) { Text(instructorT("Import", "Importar")) }
                }
            }
        }
        item {
            InstructorCard {
                Text(instructorT("Settings", "Configuración"), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(instructorT("Enable Daily Formation", "Activar Formación diaria"), Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Text(instructorT("Choose when the daily reminder should be sent to users who have not already opened and dismissed today’s card. The time zone determines how that reminder time is interpreted.", "Elige cuándo se enviará el recordatorio diario a los usuarios que todavía no hayan abierto y cerrado la tarjeta de hoy. La zona horaria determina cómo se interpreta esa hora."), color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp)
                ParishReminderTimePicker(reminderTime) { reminderTime = it }
                ParishTimeZonePicker(timeZone) { timeZone = it }
                Button(onClick = {
                    repository.saveDailyFormationSettings(profile, ManagedDailyFormationSettings(enabled, reminderTime.trim(), timeZone.trim()), {
                        status = instructorT("Daily Formation settings saved.", "Configuración de Formación diaria guardada.")
                    }, { error = it.localizedMessage })
                }, modifier = Modifier.fillMaxWidth()) { Text(instructorT("Save Settings", "Guardar configuración")) }
            }
        }
        status?.let { item { Text(it, color = IlluminedThemeTokens.Blue) } }
        if (entries.isEmpty()) item { InstructorCard { Text(instructorT("No Daily Formation entries yet.", "Todavía no hay entradas de Formación diaria."), color = IlluminedThemeTokens.SecondaryText) } }
        items(entries, key = { it.date }) { entry ->
            InstructorListCard(onClick = { editingDate = entry.date }, description = "${entry.title}. ${entry.date}. ${if (entry.isPublished) instructorT("Published", "Publicada") else instructorT("Draft", "Borrador")}.") {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    InstructorSymbol(InstructorSymbolKind.Calendar, IlluminedThemeTokens.Gold, Modifier.size(32.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("${entry.date} · ${localizedDailyFormationValue(entry.type)} · ${localizedDailyFormationValue(entry.colorCode)}", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                        Text(if (entry.isPublished) instructorT("Published", "Publicada") else instructorT("Draft", "Borrador"), fontSize = 13.sp, color = IlluminedThemeTokens.Blue)
                    }
                    InstructorSymbol(InstructorSymbolKind.Chevron, IlluminedThemeTokens.SecondaryText, Modifier.size(10.dp, 18.dp))
                }
            }
        }
    }
    InstructorErrorAlert(instructorT("Daily Formation Error", "Error de Formación diaria"), error) { error = null }
}

@Composable
private fun DailyFormationCsvImportScreen(profile: UserProfile, onCancel: () -> Unit, onImported: (Int) -> Unit) {
    val repository = remember { InstructorRepository() }
    val context = LocalContext.current
    var csv by rememberSaveable { mutableStateOf("date,type,title,details,color\n2026-09-03,saint,Saint Gregory the Great,\"Pope and Doctor of the Church, remembered for pastoral leadership and sacred music.\",WHITE\n2026-09-04,note,Friday Penance,Offer prayer or another act of penance today.,GREEN") }
    var preview by remember { mutableStateOf<DailyFormationCsvPreview?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error(instructorT("The selected file could not be read.", "No se pudo leer el archivo seleccionado."))
            }.onSuccess {
                csv = it; preview = null; fileError = null
            }.onFailure {
                fileError = if(Locale.getDefault().language=="es") "No se pudo abrir el archivo CSV: ${it.localizedMessage ?: "Error desconocido"}" else "The CSV file could not be opened: ${it.localizedMessage ?: "Unknown error"}"
            }
        }
    }
    BackHandler { if (!saving) onCancel() }
    LazyColumn(
        Modifier.fillMaxSize().background(instructorBrush()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TextButton(onClick = onCancel, enabled = !saving) { Text(instructorT("‹ Cancel", "‹ Cancelar")) } }
        item {
            InstructorCard {
                Text(instructorT("Import Daily Formation", "Importar Formación diaria"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text(instructorT("Use this when you already have your liturgical calendar in Excel, Google Sheets, or another spreadsheet. Choose the CSV file or paste its rows below, preview the cards, then publish them.", "Usa esta opción si ya tienes tu calendario litúrgico en Excel, Google Sheets u otra hoja de cálculo. Elige el archivo CSV o pega sus filas abajo, revisa las tarjetas y luego publícalas."), color = IlluminedThemeTokens.SecondaryText)
                Text(instructorT("Expected columns", "Columnas esperadas"), fontWeight = FontWeight.SemiBold)
                Text("date, type, title, details, color", color = IlluminedThemeTokens.Gold, fontWeight = FontWeight.SemiBold)
                Text(instructorT("Use YYYY-MM-DD dates; fact, saint, or note types; and WHITE, GOLD, GREEN, RED, PURPLE, or ROSE colors.", "Usa fechas YYYY-MM-DD; tipos fact, saint o note; y colores WHITE, GOLD, GREEN, RED, PURPLE o ROSE."), color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp)
            }
        }
        item {
            InstructorCard {
                Text(instructorT("Choose or Paste Calendar", "Elegir o pegar calendario"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { fileLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(instructorT("Choose CSV File", "Elegir archivo CSV")) }
                OutlinedTextField(csv, { csv = it; preview = null }, Modifier.fillMaxWidth().heightIn(min = 190.dp), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp), minLines = 8, enabled = !saving)
                OutlinedButton(onClick = { preview = DailyFormationCsvParser.parse(csv) }, enabled = csv.isNotBlank() && !saving, modifier = Modifier.fillMaxWidth()) { Text(instructorT("Preview Calendar", "Vista previa del calendario")) }
                fileError?.let { Text(localizedUserMessage(it), color = Color.Red, fontSize = 13.sp) }
            }
        }
        preview?.let { result ->
            item {
                InstructorCard {
                    Text(instructorT("Preview", "Vista previa"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(if(Locale.getDefault().language=="es") "${result.validRows.size} de ${result.totalRows} entradas válidas listas para publicar." else "${result.validRows.size} valid of ${result.totalRows} entries ready to publish.", color = IlluminedThemeTokens.SecondaryText)
                    result.issues.take(12).forEach { issue -> Text(if(Locale.getDefault().language=="es") "Fila ${issue.rowNumber}: ${issue.message}" else "Row ${issue.rowNumber}: ${issue.message}", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    result.validRows.take(20).forEach { row ->
                        Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(.72f)) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Text(row.title, fontWeight = FontWeight.SemiBold)
                                Text("${instructorT("Row", "Fila")} ${row.rowNumber} · ${row.date} · ${localizedDailyFormationValue(row.type)} · ${localizedDailyFormationValue(row.colorCode)}", color = IlluminedThemeTokens.SecondaryText, fontSize = 12.sp)
                            }
                        }
                    }
                    if (result.validRows.size > 20) Text(if(Locale.getDefault().language=="es") "Además, hay ${result.validRows.size - 20} entradas válidas más." else "Plus ${result.validRows.size - 20} more valid entries.", color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp)
                    Text(instructorT("An imported row replaces the entry with the same date in this classroom only.", "Una fila importada reemplaza la entrada con la misma fecha solamente en esta clase."), color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp)
                }
            }
            item {
                Button(onClick = {
                    saving = true
                    repository.importDailyFormationEntries(profile, result.validRows, { onImported(result.validRows.size) }, {
                        saving = false; fileError = it.localizedMessage
                    })
                }, enabled = !saving && result.validRows.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(if (saving) instructorT("Publishing…", "Publicando…") else instructorT("Publish Calendar", "Publicar calendario")) }
            }
        }
    }
}

@Composable
private fun DailyFormationEntryEditor(profile: UserProfile, value: ManagedDailyFormationEntry?, onCancel: () -> Unit, onSaved: () -> Unit) {
    val repository = remember { InstructorRepository() }
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var date by remember(value?.date) { mutableStateOf(value?.date ?: dateFormatter.format(java.util.Date())) }
    var type by remember(value?.date) { mutableStateOf(value?.type ?: "saint") }
    var title by remember(value?.date) { mutableStateOf(value?.title.orEmpty()) }
    var details by remember(value?.date) { mutableStateOf(value?.details.orEmpty()) }
    var color by remember(value?.date) { mutableStateOf(value?.colorCode ?: "WHITE") }
    var published by remember(value?.date) { mutableStateOf(value?.isPublished ?: true) }
    var saving by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onCancel, enabled = !saving) { Text(instructorT("‹ Cancel", "‹ Cancelar")) }; Text(if (value == null) instructorT("New Daily Formation Entry", "Nueva entrada de Formación diaria") else instructorT("Edit Daily Formation Entry", "Editar entrada de Formación diaria"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
        item {
            InstructorCard {
                ParishDatePicker(instructorT("Date", "Fecha"), date, value == null && !saving) { date = it }
                DailyFormationChoice(instructorT("Type", "Tipo"), type, listOf("fact", "saint", "note"), !saving) { type = it }
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(instructorT("Title", "Título")) }, singleLine = true, enabled = !saving)
                OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), label = { Text(instructorT("Details", "Detalles")) }, minLines = 6, maxLines = 12, enabled = !saving)
                DailyFormationChoice(instructorT("Liturgical color", "Color litúrgico"), color, listOf("WHITE", "GOLD", "GREEN", "RED", "PURPLE", "ROSE"), !saving) { color = it }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(instructorT("Published", "Publicada"), Modifier.weight(1f)); Switch(checked = published, onCheckedChange = { published = it }, enabled = !saving) }
                Button(onClick = {
                    saving = true
                    repository.saveDailyFormationEntry(profile, ManagedDailyFormationEntry(date, type, title, details, color, published), onSaved, {
                        saving = false; error = it.localizedMessage
                    })
                }, enabled = !saving && title.isNotBlank() && details.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (saving) instructorT("Saving…", "Guardando…") else instructorT("Save Entry", "Guardar entrada")) }
                if (value != null) OutlinedButton(onClick = { confirmingDelete = true }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(instructorT("Delete Entry", "Eliminar entrada"), color = Color.Red) }
            }
        }
    }
    if (confirmingDelete && value != null) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text(instructorT("Delete this Daily Formation entry?", "¿Eliminar esta entrada de Formación diaria?")) },
        text = { Text(if(Locale.getDefault().language=="es") "La entrada del ${value.date} se eliminará de esta clase." else "The entry for ${value.date} will be removed from this classroom.") },
        confirmButton = { TextButton(onClick = { saving = true; confirmingDelete = false; repository.deleteDailyFormationEntry(profile, value.date, onSaved, { saving = false; error = it.localizedMessage }) }) { Text(instructorT("Delete", "Eliminar"), color = Color.Red) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(instructorT("Cancel", "Cancelar")) } },
    )
    InstructorErrorAlert(instructorT("Daily Formation Error", "Error de Formación diaria"), error) { error = null }
}

@Composable
private fun DailyFormationChoice(label: String, value: String, choices: List<String>, enabled: Boolean, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("$label · ${localizedDailyFormationValue(value)}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { choice -> DropdownMenuItem(text = { Text(localizedDailyFormationValue(choice)) }, onClick = { select(choice); expanded = false }) }
        }
    }
}

private fun localizedDailyFormationValue(value: String): String = if (Locale.getDefault().language == "es") when (value.lowercase()) {
    "fact" -> "Dato"
    "saint" -> "Santo"
    "note" -> "Nota litúrgica"
    "white" -> "Blanco"
    "gold" -> "Dorado"
    "green" -> "Verde"
    "red" -> "Rojo"
    "purple" -> "Morado"
    "rose" -> "Rosa"
    else -> value
} else value.replaceFirstChar { it.uppercase() }

@Composable
private fun InstructorListCard(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
        shape = RoundedCornerShape(InstructorListCardPresentation.CornerRadius.dp),
        color = Color.White.copy(.94f),
        shadowElevation = InstructorListCardPresentation.ShadowElevation.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(InstructorListCardPresentation.GoldBorderAlpha)),
        content = content,
    )
}
@Composable private fun InstructorCard(content: @Composable ColumnScope.() -> Unit) { Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) } }
private fun instructorBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
private fun isTomorrow(timeMillis: Long): Boolean {
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val value = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
    return tomorrow.get(Calendar.ERA) == value.get(Calendar.ERA) && tomorrow.get(Calendar.YEAR) == value.get(Calendar.YEAR) && tomorrow.get(Calendar.DAY_OF_YEAR) == value.get(Calendar.DAY_OF_YEAR)
}

private fun studentRosterStatus(student: UserProfile, classId: String): String =
    if (classId in student.removedClassIds) "Removed" else if (classId in student.inactiveClassIds) "Inactive" else "Active"

private fun studentRosterLabel(status: String): String = when(status) {
    "Removed" -> instructorT("Removed", "Retirados")
    "Inactive" -> instructorT("Inactive", "Inactivos")
    "All" -> instructorT("All", "Todos")
    else -> instructorT("Active", "Activos")
}

@Composable
private fun StudentRosterControls(student: UserProfile, classId: String, instructorId: String, done: () -> Unit) {
    val context = LocalContext.current
    var pending by remember(student.userId,classId) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val status = studentRosterStatus(student,classId)
    fun label(action: String) = when(action) {
        "remove" -> instructorT("Remove from Class", "Retirar de la clase")
        "inactive" -> instructorT("Mark Inactive", "Marcar como inactivo")
        else -> instructorT("Restore Student", "Restaurar estudiante")
    }
    fun explanation(action: String) = when(action) {
        "remove" -> instructorT("Class access will be revoked. The account and progress are kept. A class code cannot restore access; an instructor must restore this student.", "Se revocará el acceso a esta clase. La cuenta y el progreso se conservan. Un código de clase no permite volver a entrar; un instructor debe restaurar al estudiante.")
        "inactive" -> instructorT("The student keeps class access and progress but is excluded from active counts and class notifications.", "El estudiante conserva el acceso y el progreso, pero se excluye de los recuentos activos y las notificaciones de la clase.")
        else -> instructorT("Restore active membership in this class, including class access and notifications. Existing progress is preserved.", "Restablece la participación activa en esta clase, incluido el acceso y las notificaciones. Se conserva el progreso existente.")
    }
    InstructorCard {
        Text(instructorT("Class Membership", "Participación en la clase"),fontSize=18.sp,fontWeight=FontWeight.SemiBold)
        Text(studentRosterLabel(status),color=IlluminedThemeTokens.Blue)
        if(student.email.isNotBlank()) TextButton(onClick={
            try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO,android.net.Uri.fromParts("mailto",student.email,null))) }
            catch(_: Exception) { error=instructorT("No email app is available.", "No hay una aplicación de correo disponible.") }
        }) { Text(instructorT("Email Student", "Enviar correo al estudiante")) }
        if(!student.isInstructor && !student.isAdmin && student.userId != instructorId) {
            if(status != "Active") Button(onClick={pending="restore"},enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text(label("restore")) }
            if(status == "Active") OutlinedButton(onClick={pending="inactive"},enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text(label("inactive")) }
            if(status != "Removed") TextButton(onClick={pending="remove"},enabled=!busy) { Text(label("remove"),color=MaterialTheme.colorScheme.error) }
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    }
    pending?.let { action ->
        AlertDialog(onDismissRequest={pending=null},title={Text(label(action))},
            text={Text(student.displayName+" ("+student.email+")\n\n"+explanation(action))},
            confirmButton={TextButton(onClick={
                pending=null;busy=true;error=null
                com.google.firebase.functions.FirebaseFunctions.getInstance().getHttpsCallable("manageStudentRoster")
                    .call(mapOf("classId" to classId,"userId" to student.userId,"action" to action))
                    .addOnSuccessListener { busy=false;done() }
                    .addOnFailureListener { busy=false;error=InstructorErrorPresentation.message(it,instructorT("The roster could not be updated.", "No se pudo actualizar la lista de estudiantes.")) }
            }) { Text(label(action)) }},
            dismissButton={TextButton(onClick={pending=null}) { Text(instructorT("Cancel","Cancelar")) }})
    }
}
