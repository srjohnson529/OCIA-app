package com.illumined.app.ui

import androidx.activity.compose.BackHandler
import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import java.util.Calendar

private enum class InstructorPage { MENU, CLASSES, ANNOUNCEMENTS, SCHEDULE, ASSIGNMENTS, DISCUSSIONS, PROGRESS, INVITES }

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
    when (page) {
        InstructorPage.MENU -> InstructorMenu(profile, onBack) { page = it }
        InstructorPage.CLASSES -> ClassManager(profile) { page = InstructorPage.MENU }
        InstructorPage.ANNOUNCEMENTS -> AnnouncementManager(profile) { page = InstructorPage.MENU }
        InstructorPage.SCHEDULE -> ScheduleManager(profile, schedule) { page = InstructorPage.MENU }
        InstructorPage.ASSIGNMENTS -> AssignmentManager(profile, assignments, schedule) { page = InstructorPage.MENU }
        InstructorPage.DISCUSSIONS -> DiscussionManager(profile) { page = InstructorPage.MENU }
        InstructorPage.PROGRESS -> StudentProgressManager(profile) { page = InstructorPage.MENU }
        InstructorPage.INVITES -> AccessCodeExperience(profile, parishMode = false) { page = InstructorPage.MENU }
    }
}

@Composable
private fun InstructorMenu(profile: UserProfile, onBack: () -> Unit, select: (InstructorPage) -> Unit) {
    fun destination(key: String) = when (key) {
        "classes" -> InstructorPage.CLASSES
        "announcements" -> InstructorPage.ANNOUNCEMENTS
        "schedule" -> InstructorPage.SCHEDULE
        "assignments" -> InstructorPage.ASSIGNMENTS
        "discussions" -> InstructorPage.DISCUSSIONS
        "progress" -> InstructorPage.PROGRESS
        "invites" -> InstructorPage.INVITES
        else -> InstructorPage.MENU
    }
    val className = profile.selectedClassId.ifBlank { "your class" }
    LazyColumn(
        Modifier.fillMaxSize().background(instructorBrush()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ Back") }
            InstructorCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InstructorSymbol(InstructorSymbolKind.Tools, IlluminedThemeTokens.Blue, Modifier.size(24.dp))
                    Text("Instructor Tools", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                }
                Text("Manage class content for $className.", fontSize = 16.sp, lineHeight = 22.sp, color = IlluminedThemeTokens.SecondaryText)
            }
        }
        items(InstructorToolPresentation.items, key = { it.key }) { tool ->
            Surface(
                onClick = { select(destination(tool.key)) },
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "${tool.title}. ${tool.subtitle}. ${InstructorToolPresentation.Status}"
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
                        Text(tool.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                        Text(tool.subtitle, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(InstructorToolPresentation.Status, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                }
            }
        }
    }
}

@Composable
private fun ClassManager(profile: UserProfile, onBack: () -> Unit) {
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
                "Classes",
                "Create classes, choose the active class, or archive a class while preserving its records.",
                onBack,
                workingClassId == null,
            ) { creating = true }
        }
        if (creating) item {
            InstructorCard {
                Text("Create a Class", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text("Students will enter this class ID when setting up their accounts.", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                OutlinedTextField(
                    value = newClassId,
                    onValueChange = { newClassId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New class ID") },
                    singleLine = true,
                    enabled = workingClassId == null,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { creating = false; newClassId = "" }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val requestedId = newClassId.trim()
                            workingClassId = requestedId
                            setupRepository.createAdditionalInstructorClass(profile, requestedId, success = {
                                workingClassId = null
                                creating = false
                                newClassId = ""
                                confirmation = "$requestedId was created and is now active."
                            }, error = {
                                workingClassId = null
                                error = it.localizedMessage ?: "The class could not be created."
                            })
                        },
                        modifier = Modifier.weight(1f),
                        enabled = newClassId.isNotBlank() && workingClassId == null,
                    ) { Text(if (workingClassId != null) "Creating..." else "Create") }
                }
            }
        }
        item { Text("Active Classes", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink) }
        if (activeClasses.isEmpty()) item { InstructorCard { Text("No active classes.", color = IlluminedThemeTokens.SecondaryText) } }
        items(activeClasses, key = { "active-$it" }) { classId ->
            InstructorCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    InstructorSymbol(InstructorSymbolKind.People, IlluminedThemeTokens.Gold, Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(classId, modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    if (classId == profile.selectedClassId) Text("Active", color = IlluminedThemeTokens.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (classId != profile.selectedClassId) OutlinedButton(
                    onClick = {
                        workingClassId = classId
                        profileRepository.setActiveClass(profile, classId, { workingClassId = null }, {
                            workingClassId = null
                            error = it.localizedMessage ?: "The active class could not be changed."
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = workingClassId == null,
                ) { Text("Make Active") }
                TextButton(
                    onClick = { archiveCandidate = classId },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeClasses.size > 1 && workingClassId == null,
                ) { Text("Archive Class") }
                if (activeClasses.size <= 1) Text("Create or restore another class before archiving this one.", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
            }
        }
        if (archivedClasses.isNotEmpty()) {
            item { Text("Archived Classes", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink) }
            items(archivedClasses, key = { "archived-$it" }) { classId ->
                InstructorCard {
                    Text(classId, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Records are preserved. New class activity is paused.", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                    Button(
                        onClick = {
                            workingClassId = classId
                            classRepository.restoreClass(classId, {
                                workingClassId = null
                                confirmation = "$classId was restored."
                            }, {
                                workingClassId = null
                                error = it.localizedMessage ?: "The class could not be restored."
                            })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = workingClassId == null,
                    ) { Text(if (workingClassId == classId) "Restoring..." else "Restore Class") }
                }
            }
        }
    }

    archiveCandidate?.let { classId ->
        AlertDialog(
            onDismissRequest = { archiveCandidate = null },
            title = { Text("Archive $classId?") },
            text = { Text("The class will move out of the active list and new activity will pause. All class records will be preserved and the class can be restored later.") },
            dismissButton = { TextButton(onClick = { archiveCandidate = null }) { Text("Cancel") } },
            confirmButton = { TextButton(onClick = {
                archiveCandidate = null
                workingClassId = classId
                classRepository.archiveClass(classId, {
                    workingClassId = null
                    confirmation = "$classId was archived."
                }, {
                    workingClassId = null
                    error = it.localizedMessage ?: "The class could not be archived."
                })
            }) { Text("Archive") } },
        )
    }
    InstructorErrorAlert("Class Error", error) { error = null }
    confirmation?.let { message -> AlertDialog(onDismissRequest = { confirmation = null }, title = { Text("Classes Updated") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { confirmation = null }) { Text("OK") } }) }
}

@Composable
private fun AnnouncementManager(profile: UserProfile, onBack: () -> Unit) {
    val repository = remember { InstructorRepository() }; val classId = profile.selectedClassId; var values by remember { mutableStateOf(emptyList<Announcement>()) }; var editingId by rememberSaveable { mutableStateOf<String?>(null) }; val editing = restoreEditedRecord(values, editingId, Announcement::id); var creating by rememberSaveable { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; var sentMessage by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = creating || editingId != null) {
        creating = false
        editingId = null
    }
    DisposableEffect(classId) { var listener: ListenerRegistration? = null; if (classId.isNotBlank()) listener = repository.listenAnnouncements(classId, { values = it }, { error = it.localizedMessage ?: "Announcements could not be loaded." }); onDispose { listener?.remove() } }
    if (creating || editing != null) AnnouncementEditor(
        editing,
        onCancel = { creating = false; editingId = null },
        onSave = { title, message, active, sendPush, finished ->
            val failed: (Throwable) -> Unit = { finished(); error = it.localizedMessage ?: "Announcement could not be saved." }
            if (editing == null && sendPush) repository.createAnnouncementWithPush(profile, title, message, active, { recipients ->
                finished(); creating = false
                sentMessage = "Announcement sent to $recipients device${if (recipients == 1) "" else "s"}."
            }, failed)
            else if (editing == null) repository.createAnnouncement(profile, title, message, { finished(); creating = false }, failed)
            else repository.updateAnnouncement(profile, editing.id, title, message, active, { finished(); editingId = null }, failed)
        },
        onDelete = editing?.let { value -> { finished -> repository.deleteAnnouncement(profile, value.id, { finished(); editingId = null }, { finished(); error = it.localizedMessage ?: "Announcement could not be deleted." }) } },
    ) else LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ManagerHeader("Announcements", "Create updates that appear on the student dashboard.", onBack, classId.isNotBlank()) { creating = true } }
        if (values.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStatePresentation.announcements) } }
        items(values, key = { it.id }) { value ->
            InstructorListCard(
                onClick = { editingId = value.id },
                description = "${value.title}. ${AnnouncementEditorPolicy.statusText(value.isActive)}. ${value.message}",
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        InstructorSymbol(if(value.isActive) InstructorSymbolKind.Active else InstructorSymbolKind.Paused, if (value.isActive) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText, Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(value.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2); Text(AnnouncementEditorPolicy.statusText(value.isActive), color = if (value.isActive) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        InstructorSymbol(InstructorSymbolKind.Chevron, IlluminedThemeTokens.SecondaryText, Modifier.size(10.dp,18.dp))
                    }
                    Text(value.message, color = IlluminedThemeTokens.SecondaryText, maxLines = 3)
                    value.displayTimestamp?.toDate()?.let { Text("Updated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)}", color = IlluminedThemeTokens.SecondaryText, fontSize = 11.sp) }
                }
            }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text("Announcement Error") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
    sentMessage?.let { message -> AlertDialog(onDismissRequest = { sentMessage = null }, title = { Text("Announcement Sent") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { sentMessage = null }) { Text("OK") } }) }
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
        item { TextButton(onClick = onCancel, enabled = !saving) { Text("‹ Cancel") }; Text(if (value == null) "New Announcement" else "Edit Announcement", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
        item { InstructorCard { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true); OutlinedTextField(message, { message = it }, Modifier.fillMaxWidth(), label = { Text("Message") }, minLines = 5, maxLines = 10); Row(verticalAlignment = Alignment.CenterVertically) { Text("Visible to Students", fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Switch(active, { active = it }, enabled = !saving) }; if (value == null) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Send push notification", fontWeight = FontWeight.SemiBold); Text("Alert class members who have notifications enabled.", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText) }; Switch(sendPush, { sendPush = it }, enabled = !saving) } } } }
        item { Button(onClick = { saving = true; onSave(title, message, active, sendPush) { saving = false } }, enabled = AnnouncementEditorPolicy.canSave(title, message, saving), modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Saving..." else if (value == null && sendPush) "Send Announcement" else "Save Announcement") } }
        if (onDelete != null) item { OutlinedButton(onClick = { confirmingDelete = true }, enabled = !saving, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("Delete Announcement") } }
    }
    if (confirmingDelete && onDelete != null) AlertDialog(onDismissRequest = { confirmingDelete = false }, title = { Text("Delete this announcement?") }, confirmButton = { TextButton(onClick = { confirmingDelete = false; saving = true; onDelete { saving = false } }) { Text("Delete", color = Color.Red) } }, dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } })
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
                    { problem -> done(); error = problem.message ?: "The schedule could not be imported." })
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
                    { finished(); showingEditor = false }, { finished(); error = it.message ?: "The class could not be saved." })
                else repository.updateSchedule(profile, value.id, topic, details, date, sortOrder,
                    { finished(); showingEditor = false; editingId = null }, { finished(); error = it.message ?: "The class could not be saved." })
            },
            onDelete = editing?.let { value ->
                { finished ->
                    repository.deleteSchedule(
                        profile,
                        value.id,
                        { finished(); showingEditor = false; editingId = null },
                        { problem -> finished(); error = problem.message ?: "The class could not be deleted." },
                    )
                }
            },
        )
        else -> LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                TextButton(onClick = onBack) { Text("‹ Back") }
                InstructorCard {
                    Text("Class Schedule", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                    Text("Create classes one at a time, or import a full schedule from a spreadsheet.", color = IlluminedThemeTokens.SecondaryText)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { editingId = null; showingEditor = true }, enabled = classId.isNotBlank(), modifier = Modifier.weight(1f)) { Text("New Class") }
                        OutlinedButton(onClick = { showingImport = true }, enabled = classId.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Import") }
                    }
                }
            }
            if (schedule.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStatePresentation.schedule) } }
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
    InstructorErrorAlert(InstructorErrorPresentation.ScheduleTitle, error) { error = null }
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
        item { TextButton(onClick = onCancel, enabled = InstructorEditorOperationPolicy.canInteract(saving)) { Text("‹ Cancel") }; Text(if (value == null) "New Class" else "Edit Class", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
        item {
            InstructorCard {
                OutlinedButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = date }
                    DatePickerDialog(context, { _, y, m, d -> c.set(y, m, d, 0, 0, 0); c.set(Calendar.MILLISECOND, 0); date = c.timeInMillis }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }, Modifier.fillMaxWidth()) { Text("Class Date  ·  ${DateFormat.getDateInstance().format(java.util.Date(date))}") }
                OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth(), label = { Text("Topic") })
                OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), label = { Text("Optional details") }, minLines = 3, maxLines = 7)
            }
        }
        item { Button(onClick = { saving = true; onSave(topic, details, date) { saving = false } }, enabled = topic.isNotBlank() && InstructorEditorOperationPolicy.canInteract(saving), modifier = Modifier.fillMaxWidth()) { Text(InstructorEditorOperationPolicy.saveLabel(saving, "Save Class")) } }
        if (onDelete != null) item { OutlinedButton(onClick = { confirmingDelete = true }, enabled = InstructorEditorOperationPolicy.canInteract(saving), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("Delete Class") } }
    }
    if (confirmingDelete && onDelete != null) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text("Delete this class date?") },
        confirmButton = {
            TextButton(onClick = {
                confirmingDelete = false
                saving = true
                onDelete { saving = false }
            }) { Text("Delete", color = Color.Red) }
        },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
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
        item { TextButton(onClick = onCancel, enabled = !saving) { Text("‹ Cancel") } }
        item { InstructorCard { Text("Import Full Schedule", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text("Copy rows from Numbers, Excel, or Google Sheets, paste them below, preview the classes, then import them.", color = IlluminedThemeTokens.SecondaryText); Text("Expected columns", fontWeight = FontWeight.SemiBold); Text("date, topic, details", color = IlluminedThemeTokens.Gold, fontWeight = FontWeight.SemiBold); Text("Details are optional. Dates can be 2026-09-03 or 9/3/2026.", color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp) } }
        item {
            InstructorCard {
                Text("Paste Schedule", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(csv, { csv = it; preview = emptyList(); parseError = null }, Modifier.fillMaxWidth().heightIn(min = 170.dp), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp), minLines = 7)
                OutlinedButton(onClick = {
                    when (val result = ScheduleImportParser.parse(csv)) {
                        is ScheduleParseResult.Success -> { preview = result.rows; parseError = null }
                        is ScheduleParseResult.Failure -> { preview = emptyList(); parseError = result.message }
                    }
                }, enabled = csv.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Preview Schedule") }
                parseError?.let { Text(it, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (preview.isNotEmpty()) item {
            InstructorCard {
                Text("Preview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("${preview.size} class dates ready to import.", color = IlluminedThemeTokens.SecondaryText)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Replace existing schedule", fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Switch(replace, { replace = it }) }
                Text(if (replace) "This will remove the current ${existing.size} schedule items for this class and use the imported rows instead." else "This will add the imported rows to the schedule you already have.", color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp)
                preview.forEach { row ->
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(.72f)) { Column(Modifier.fillMaxWidth().padding(10.dp)) { Text(row.topic, fontWeight = FontWeight.SemiBold); Text(row.date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, uuuu")), color = IlluminedThemeTokens.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); if (row.details.isNotBlank()) Text(row.details, color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp, maxLines = 2) } }
                }
            }
        }
        if (preview.isNotEmpty()) item { Button(onClick = { saving = true; onImport(preview, replace) { saving = false } }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Importing…" else "Import Schedule") } }
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
            listeners += repository.listenAssignments(classId, { values = it }, { problem -> error = InstructorErrorPresentation.message(problem, "Assignments could not be loaded.") })
            listeners += repository.listenStudents(classId, { students = it.sortedBy { student -> student.displayName.lowercase() } }, { problem -> error = InstructorErrorPresentation.message(problem, "Student progress could not be loaded.") })
            listeners += repository.listenAssignmentCompletions(classId, { completions = it }, { problem -> error = InstructorErrorPresentation.message(problem, "Assignment completions could not be loaded.") })
        }
        onDispose { listeners.forEach { it.remove() } }
    }
    val today = remember { Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }.timeInMillis }
    val nextClass = schedule.filter { (it.date?.toDate()?.time ?: Long.MIN_VALUE) > today }.minByOrNull { it.date?.seconds ?: Long.MAX_VALUE }
    val readinessAssignments = values.filter { assignment -> assignment.isActive && nextClass != null && (assignment.dueAt?.toDate()?.time ?: Long.MAX_VALUE) in today..nextClass.date!!.toDate().time }
    val readiness = InstructorReadinessCalculator.classReadiness(readinessAssignments, students, completions)
    if (creating || editor != null) AssignmentEditor(editor, onCancel = { creating = false; editorId = null }, onSave = { title, instructions, due, links, readings, active, finished ->
        if (editor == null) repository.createAssignment(profile, title, instructions, due, links, readings, { finished(); creating = false }, { problem -> finished(); error = problem.localizedMessage ?: "Assignment could not be saved." })
        else repository.updateAssignment(profile, editor.id, title, instructions, due, links, readings, active, { finished(); editorId = null }, { problem -> finished(); error = problem.localizedMessage ?: "Assignment could not be saved." })
    }, onDelete = editor?.let { value ->
        { finished ->
            repository.deleteAssignment(
                profile,
                value.id,
                { finished(); editorId = null },
                { problem -> finished(); error = problem.localizedMessage ?: "Assignment could not be deleted." },
            )
        }
    })
    else LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ManagerHeader("Assignments", "Post readings, lesson work, and preparation tasks for students.", onBack, classId.isNotBlank()) { creating = true } }
        item { InstructorCard {
            Text(if (nextClass?.date?.toDate()?.let { isTomorrow(it.time) } == true) "Tomorrow's Class Readiness" else "Next Class Readiness", fontSize=18.sp,fontWeight=FontWeight.SemiBold)
            if(nextClass==null) Text("Add a class schedule item to activate readiness tracking.",color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)
            else Text("${nextClass.topic} · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(nextClass.date!!.toDate())}",color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)
            if(readiness.totalChecks==0) Text("No assignments are due before the next class yet.",color=IlluminedThemeTokens.SecondaryText)
            else { Row { Text("${readiness.percent}% ready",fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue);Spacer(Modifier.weight(1f));Text("${readiness.completedChecks}/${readiness.totalChecks} checks",color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp,fontWeight=FontWeight.SemiBold) }; LinearProgressIndicator(progress={readiness.fraction},Modifier.fillMaxWidth(),color=if(readiness.percent>=80)IlluminedThemeTokens.Blue else IlluminedThemeTokens.Gold); if(nextClass?.date?.toDate()?.let{isTomorrow(it.time)}==true&&readiness.percent<80)Text("Readiness alert: follow up with students who still have assignments unchecked.",color=IlluminedThemeTokens.Gold,fontSize=13.sp,fontWeight=FontWeight.SemiBold) }
        } }
        if (values.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStatePresentation.assignments) } }
        items(values, key = { it.id }) { item ->
            val progress=InstructorReadinessCalculator.assignmentProgress(item.id,students,completions)
            InstructorListCard(onClick = { editorId = item.id }, description = "${item.title}. ${if (item.isActive) "Visible to students" else "Hidden from students"}. ${progress.completedCount} of ${progress.totalStudents} completed") {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){InstructorSymbol(if(item.isActive)InstructorSymbolKind.Active else InstructorSymbolKind.Paused,if(item.isActive)IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,Modifier.size(20.dp));Spacer(Modifier.width(9.dp));Text(item.title,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));InstructorSymbol(InstructorSymbolKind.Chevron,IlluminedThemeTokens.SecondaryText,Modifier.size(10.dp,18.dp))}
                    Text("Due ${item.dueAt?.toDate()?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(it) }.orEmpty()}",color=IlluminedThemeTokens.Blue,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                    item.lessonLinks.take(3).forEach{link->InstructorMetadataRow(InstructorSymbolKind.Book,link.lessonTitle.ifBlank{link.lessonId})}
                    if(item.lessonLinks.size>3)Text("+ ${item.lessonLinks.size-3} more lessons",color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp)
                    if(item.readings.isNotEmpty()){InstructorMetadataRow(InstructorSymbolKind.Document,"${item.readings.size} reading${if(item.readings.size==1)"" else "s"}");item.readings.forEach{reading->val readingProgress=InstructorReadinessCalculator.assignmentProgress("${item.id}__reading__${reading.id}",students,completions);Text("${reading.title}: ${readingProgress.completedCount}/${readingProgress.totalStudents}",color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,maxLines=1)}}
                    if(item.instructions.isNotBlank())Text(item.instructions,color=IlluminedThemeTokens.SecondaryText,maxLines=3)
                    Text(if(item.isActive)"Visible to students" else "Hidden from students",color=if(item.isActive)IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                    Row{Text("Completed",color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text("${progress.completedCount}/${progress.totalStudents}",color=IlluminedThemeTokens.Blue,fontSize=12.sp,fontWeight=FontWeight.SemiBold)}
                    LinearProgressIndicator(progress={progress.fraction},Modifier.fillMaxWidth(),color=IlluminedThemeTokens.Gold)
                    if(progress.incompleteNames.isNotEmpty())Text("Still waiting on ${progress.incompleteNames.take(3).joinToString()}",color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,maxLines=2)
                }
            }
        }
    }
    InstructorErrorAlert(InstructorErrorPresentation.AssignmentTitle, error) { error = null }
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
        item { TextButton(onClick = onCancel, enabled = InstructorEditorOperationPolicy.canInteract(saving)) { Text("‹ Cancel") }; Text(if (value == null) "New Assignment" else "Edit Assignment", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
        item { InstructorCard {
            OutlinedButton(onClick = { val c=Calendar.getInstance().apply { timeInMillis=due };DatePickerDialog(context,{_,y,m,d->c.set(y,m,d,0,0,0);c.set(Calendar.MILLISECOND,0);due=c.timeInMillis},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show() },Modifier.fillMaxWidth()){Text("Due Date  ·  ${DateFormat.getDateInstance().format(java.util.Date(due))}")}
            OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Title")})
            OutlinedTextField(instructions,{instructions=it},Modifier.fillMaxWidth(),label={Text("Instructions")},minLines=4,maxLines=8)
            HorizontalDivider()
            Text("Optional Lesson Links", fontWeight = FontWeight.SemiBold)
            Text(if (selectedIds.isEmpty()) "No linked lessons selected." else "${selectedIds.size} lesson${if(selectedIds.size==1)"" else "s"} selected", color = if(selectedIds.isEmpty()) IlluminedThemeTokens.SecondaryText else IlluminedThemeTokens.Blue, fontSize = 13.sp)
            categories.forEach { category ->
                val expanded = category.name in expandedCategories
                val count = category.lessons.count { it.id in selectedIds }
                Surface(onClick = { expandedCategories = if(expanded) expandedCategories-category.name else expandedCategories+category.name }, color = IlluminedThemeTokens.Cream, shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment=Alignment.CenterVertically) { InstructorSymbol(if(expanded)InstructorSymbolKind.Collapse else InstructorSymbolKind.Expand,IlluminedThemeTokens.Blue,Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Column { Text(category.name, fontWeight=FontWeight.SemiBold, fontSize=15.sp); Text(if(count==0) "${category.lessons.size} lessons" else "$count of ${category.lessons.size} selected", color=IlluminedThemeTokens.SecondaryText, fontSize=12.sp) } } }
                if(expanded) Column(Modifier.padding(start=12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) { category.lessons.forEach { lesson -> val selected=lesson.id in selectedIds; Surface(onClick={selectedIds=if(selected)selectedIds-lesson.id else selectedIds+lesson.id},color=if(selected)IlluminedThemeTokens.Blue.copy(.08f) else Color.White.copy(.72f),shape=RoundedCornerShape(10.dp)){Row(Modifier.fillMaxWidth().padding(10.dp)){Text(if(selected)"●" else "○",color=if(selected)IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText);Spacer(Modifier.width(10.dp));Text(lesson.title,fontSize=14.sp,modifier=Modifier.weight(1f))}} } }
            }
            HorizontalDivider()
            Text("Optional Assigned Readings", fontWeight = FontWeight.SemiBold)
            Text("Add one or more readings when you want students to open and complete text-based assignments.", color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)
            if(readings.isEmpty()) Text("No readings added.",color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)
            readings.forEachIndexed { index, reading -> Surface(color=Color.White.copy(.72f),shape=RoundedCornerShape(12.dp)){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Reading",color=IlluminedThemeTokens.Blue,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));TextButton(onClick={readings=readings.filterIndexed{i,_->i!=index}}){Text("Remove",color=Color.Red)}};OutlinedTextField(reading.title,{new->readings=readings.toMutableList().also{it[index]=reading.copy(title=new)}},Modifier.fillMaxWidth(),label={Text("Reading Title")});OutlinedTextField(reading.text,{new->readings=readings.toMutableList().also{it[index]=reading.copy(text=new)}},Modifier.fillMaxWidth(),label={Text("Paste full reading text")},minLines=8,maxLines=16)}} }
            OutlinedButton(onClick={readings=readings+AssignmentReading(java.util.UUID.randomUUID().toString(),"","")},Modifier.fillMaxWidth()){Text("Add Reading")}
            if(partialReading) Text("Please add both a title and full text for each reading.",color=Color.Red,fontSize=13.sp)
            Row(verticalAlignment=Alignment.CenterVertically){Text("Visible to Students",fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Switch(active,{active=it})}
        } }
        item { Button(onClick={saving=true;onSave(title,instructions,due,links,readings,active){saving=false}},enabled=title.isNotBlank()&&!partialReading&&InstructorEditorOperationPolicy.canInteract(saving),modifier=Modifier.fillMaxWidth()){Text(InstructorEditorOperationPolicy.saveLabel(saving, "Save Assignment"))} }
        if(onDelete!=null)item{OutlinedButton(onClick={confirmingDelete=true},enabled=InstructorEditorOperationPolicy.canInteract(saving),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.Red)){Text("Delete Assignment")}}
    }
    if (confirmingDelete && onDelete != null) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text("Delete this assignment?") },
        confirmButton = {
            TextButton(onClick = {
                confirmingDelete = false
                saving = true
                onDelete { saving = false }
            }) { Text("Delete", color = Color.Red) }
        },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
    )
}

@Composable
private fun DiscussionManager(profile: UserProfile, onBack: () -> Unit) {
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
            error = { error = it.localizedMessage ?: "Discussion boards could not be loaded." },
        )
        onDispose { listener?.remove() }
    }

    if (creating || editor != null) {
        DiscussionEditor(
            value = editor,
            onCancel = { creating = false; editorId = null },
            onSave = { title, prompt, lessonId, lessonTitle, required, active, finished ->
                val failed: (Throwable) -> Unit = {
                    finished()
                    error = it.localizedMessage ?: "Discussion could not be saved."
                }
                if (editor == null) repository.createDiscussion(
                    profile, title, prompt, lessonId, lessonTitle, required, active,
                    { finished(); creating = false }, failed,
                ) else repository.updateDiscussion(
                    profile, editor.id, title, prompt, lessonId, lessonTitle, required, active,
                    { finished(); editorId = null }, failed,
                )
            },
            onDelete = editor?.let { value ->
                { finished ->
                    repository.deleteDiscussion(profile, value.id, { finished(); editorId = null }, {
                        finished()
                        error = it.localizedMessage ?: "Discussion could not be deleted."
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
            item { ManagerHeader("Discussion Boards", "Create discussion prompts and connect them to lessons.", onBack, classId.isNotBlank()) { creating = true } }
            if (prompts.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStatePresentation.discussions) } }
            items(prompts, key = { it.id }) { item ->
                InstructorListCard(
                    onClick = { editorId = item.id },
                    description = "${item.title}. ${if (item.isVisible) "Visible to students" else "Hidden from students"}. ${item.lessonTitle}",
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            InstructorSymbol(if(item.isVisible)InstructorSymbolKind.Bubble else InstructorSymbolKind.EyeSlash,if(item.isVisible)IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText,Modifier.size(21.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){InstructorSymbol(InstructorSymbolKind.Book,IlluminedThemeTokens.Gold,Modifier.size(14.dp));Text(item.lessonTitle, color = IlluminedThemeTokens.Gold, fontSize = 13.sp, maxLines = 2)}
                            }
                            InstructorSymbol(InstructorSymbolKind.Chevron,IlluminedThemeTokens.SecondaryText,Modifier.size(10.dp,18.dp))
                        }
                        Text(item.prompt, color = IlluminedThemeTokens.SecondaryText, maxLines = 3)
                        Text(
                            InstructorDiscussionPolicy.statusText(item.isVisible),
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
            title = { Text("Discussion Error") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun DiscussionEditor(
    value: DiscussionPrompt?,
    onCancel: () -> Unit,
    onSave: (String, String, String, String, Boolean, Boolean, () -> Unit) -> Unit,
    onDelete: (((() -> Unit) -> Unit))?,
) {
    val context = LocalContext.current
    val categories = remember { LessonCatalog.load(context).getOrNull().orEmpty() }
    val lessons = remember(categories) { categories.flatMap { it.lessons } }
    var title by rememberSaveable(value?.id) { mutableStateOf(value?.title.orEmpty()) }
    var prompt by rememberSaveable(value?.id) { mutableStateOf(value?.prompt.orEmpty()) }
    var lessonId by rememberSaveable(value?.id) { mutableStateOf(value?.lessonId.orEmpty()) }
    var expandedCategories by remember(value?.id) { mutableStateOf(emptySet<String>()) }
    var required by rememberSaveable(value?.id) { mutableStateOf(value?.requiredForAssignment ?: true) }
    var active by rememberSaveable(value?.id) { mutableStateOf(value?.isVisible ?: true) }
    var saving by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    BackHandler { if (!saving) onCancel() }
    val lesson = lessons.firstOrNull { it.id == lessonId }
    val canSave = InstructorDiscussionPolicy.canSave(title, prompt, lesson != null, saving)

    LazyColumn(
        Modifier.fillMaxSize().background(instructorBrush()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onCancel, enabled = !saving) { Text("‹ Cancel") }
            Text(if (value == null) "New Discussion" else "Edit Discussion", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        }
        item {
            InstructorCard {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Discussion Title") }, singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), label = { Text("Discussion Prompt") }, minLines = 5, maxLines = 10)
                HorizontalDivider()
                Text("Linked Lesson", fontWeight = FontWeight.SemiBold)
                Text(lesson?.title ?: "Choose one lesson for this discussion.", color = if (lesson == null) IlluminedThemeTokens.SecondaryText else IlluminedThemeTokens.Blue, fontSize = 13.sp)
                categories.forEach { category ->
                    val expanded = category.name in expandedCategories
                    val selectedHere = category.lessons.any { it.id == lessonId }
                    Surface(
                        onClick = { expandedCategories = if (expanded) expandedCategories - category.name else expandedCategories + category.name },
                        color = IlluminedThemeTokens.Cream,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            InstructorSymbol(if(expanded)InstructorSymbolKind.Collapse else InstructorSymbolKind.Expand,IlluminedThemeTokens.Blue,Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(category.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(if (selectedHere) "Selected in this category" else "${category.lessons.size} lessons", color = IlluminedThemeTokens.SecondaryText, fontSize = 12.sp)
                            }
                        }
                    }
                    if (expanded) Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        category.lessons.forEach { option ->
                            val selected = option.id == lessonId
                            Surface(onClick = { lessonId = option.id }, color = if (selected) IlluminedThemeTokens.Blue.copy(.08f) else Color.White.copy(.72f), shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.fillMaxWidth().padding(10.dp)) {
                                    Text(if (selected) "●" else "○", color = if (selected) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText)
                                    Spacer(Modifier.width(10.dp))
                                    Text(option.title, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Required for Assignment", fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Switch(required, { required = it }, enabled = !saving) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Visible to Students", fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Switch(active, { active = it }, enabled = !saving) }
            }
        }
        item {
            Button(
                onClick = { saving = true; onSave(title, prompt, lessonId, lesson!!.title, required, active) { saving = false } },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saving) "Saving..." else "Save Discussion") }
        }
        if (onDelete != null) item {
            OutlinedButton(onClick = { confirmingDelete = true }, enabled = !saving, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("Delete Discussion") }
        }
    }
    if (confirmingDelete && onDelete != null) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text("Delete this discussion?") },
        confirmButton = { TextButton(onClick = { confirmingDelete = false; saving = true; onDelete { saving = false } }) { Text("Delete", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
    )
}

@Composable
private fun StudentProgressManager(profile: UserProfile, onBack: () -> Unit) {
    val repository = remember { InstructorRepository() }
    val context = LocalContext.current
    val classId = profile.selectedClassId
    val totalLessons = remember { LessonCatalog.load(context.applicationContext).getOrNull().orEmpty().sumOf { it.lessons.size } }
    val prayerNamesById = remember { CommonPrayerCatalog.namesById(context.applicationContext) }
    var students by remember { mutableStateOf(emptyList<UserProfile>()) }
    var completions by remember { mutableStateOf(emptyList<AssignmentCompletion>()) }
    var selected by remember { mutableStateOf<UserProfile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selected != null) { selected = null }
    DisposableEffect(classId) {
        val listeners = mutableListOf<ListenerRegistration>()
        if (classId.isNotBlank()) {
            listeners += repository.listenStudents(classId, { students = it.sortedBy { student -> student.displayName.lowercase() } }, { problem -> error = InstructorErrorPresentation.message(problem, "Student progress could not be loaded.") })
            listeners += repository.listenAssignmentCompletions(classId, { completions = it }, { problem -> error = InstructorErrorPresentation.message(problem, "Completed readings could not be loaded.") })
        }
        onDispose { listeners.forEach { it.remove() } }
    }
    InstructorErrorAlert(InstructorErrorPresentation.StudentProgressTitle, error) { error = null }
    selected?.let { student ->
        StudentProgressDetail(student, totalLessons, prayerNamesById, InstructorReadinessCalculator.completedReadingNames(student.userId, completions)) { selected = null }
        return
    }
    val average = if (students.isEmpty()) 0 else students.sumOf { it.completedLessons.size.coerceAtMost(totalLessons) } / students.size
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = onBack) { Text("‹ Back") }
            InstructorCard {
                Text("Student Progress", fontSize=22.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)
                Text("Read-only progress for students in ${classId.ifBlank { "your class" }}.",color=IlluminedThemeTokens.SecondaryText)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { ProgressStatPill("Students","${students.size}",IlluminedThemeTokens.Blue,Modifier.weight(1f));ProgressStatPill("Avg. Lessons","$average",IlluminedThemeTokens.Gold,Modifier.weight(1f)) }
            }
        }
        if (students.isEmpty()) item { InstructorCard { InstructorEmptyStateContent(InstructorEmptyStatePresentation.students) } }
        items(students,key={it.userId.ifBlank{it.displayName}}) { student ->
            val completed=student.completedLessons.size.coerceAtMost(totalLessons);val fraction=if(totalLessons==0)0f else completed.toFloat()/totalLessons
            val prayers=student.memorizedPrayerIds.mapNotNull{prayerNamesById[it]}.sortedBy{it.lowercase()};val readings=InstructorReadinessCalculator.completedReadingNames(student.userId,completions)
            InstructorListCard(onClick={selected=student},description="${student.displayName}. $completed of $totalLessons lessons. ${student.earnedBadges.size} badges. ${prayers.size} prayers. ${readings.size} readings"){
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Row(verticalAlignment=Alignment.Top){InstructorSymbol(InstructorSymbolKind.Person,IlluminedThemeTokens.Blue,Modifier.size(34.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(student.displayName,fontSize=18.sp,fontWeight=FontWeight.SemiBold);if(student.email.isNotBlank())Text(student.email,color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,maxLines=1)};Text("$completed/$totalLessons",color=IlluminedThemeTokens.Blue,fontWeight=FontWeight.SemiBold)}
                    LinearProgressIndicator(progress={fraction},Modifier.fillMaxWidth(),color=IlluminedThemeTokens.Gold)
                    Row{InstructorMetadataRow(InstructorSymbolKind.Rosette,"${student.earnedBadges.size} badges",IlluminedThemeTokens.SecondaryText);Spacer(Modifier.weight(1f));InstructorMetadataRow(InstructorSymbolKind.Book,"${prayers.size} prayers",IlluminedThemeTokens.SecondaryText)}
                    InstructorMetadataRow(InstructorSymbolKind.Document,"${readings.size} readings",IlluminedThemeTokens.SecondaryText)
                    if(prayers.isNotEmpty())Text(prayers.take(2).joinToString(),color=IlluminedThemeTokens.SecondaryText,fontSize=12.sp,maxLines=1)
                }
            }
        }
    }
}

@Composable
private fun StudentProgressDetail(student: UserProfile,totalLessons:Int,prayerNamesById:Map<String,String>,completedReadingNames:List<String>,onBack:()->Unit){
    val completed=student.completedLessons.size.coerceAtMost(totalLessons);val incomplete=(totalLessons-completed).coerceAtLeast(0);val fraction=if(totalLessons==0)0f else completed.toFloat()/totalLessons
    val prayers=student.memorizedPrayerIds.mapNotNull{prayerNamesById[it]}.sortedBy{it.lowercase()}
    LazyColumn(Modifier.fillMaxSize().background(instructorBrush()),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{TextButton(onClick=onBack){Text("‹ Student Progress")};InstructorCard{Text(student.displayName,fontSize=26.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue);if(student.email.isNotBlank())Text(student.email,color=IlluminedThemeTokens.SecondaryText);LinearProgressIndicator(progress={fraction},Modifier.fillMaxWidth(),color=IlluminedThemeTokens.Gold);Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){ProgressStatPill("Completed","$completed",IlluminedThemeTokens.Blue,Modifier.weight(1f));ProgressStatPill("Uncompleted","$incomplete",IlluminedThemeTokens.Gold,Modifier.weight(1f))}}}
        item{InstructorCard{Text("Formation Summary",fontSize=18.sp,fontWeight=FontWeight.SemiBold);ProgressDetailRow("Badges Earned","${student.earnedBadges.size}");ProgressDetailRow("Rosary Mysteries","${student.completedMysteries.size}");ProgressDetailRow("Prayers Memorized","${prayers.size}");ProgressDetailRow("Readings Completed","${completedReadingNames.size}");ProgressDetailRow("Current Lesson Index","${student.currentLessonIndex}")}}
        item{InstructorCard{Text("Memorized Prayers",fontSize=18.sp,fontWeight=FontWeight.SemiBold);if(prayers.isEmpty())Text("No memorized prayers yet.",color=IlluminedThemeTokens.SecondaryText) else prayers.forEach{InstructorCheckRow(it)}}}
        item{InstructorCard{Text("Completed Readings",fontSize=18.sp,fontWeight=FontWeight.SemiBold);if(completedReadingNames.isEmpty())Text("No completed readings yet.",color=IlluminedThemeTokens.SecondaryText) else completedReadingNames.forEach{InstructorCheckRow(it)}}}
        item{InstructorCard{Text("Completed Lesson IDs",fontSize=18.sp,fontWeight=FontWeight.SemiBold);if(student.completedLessons.isEmpty())Text("No completed lessons yet.",color=IlluminedThemeTokens.SecondaryText) else student.completedLessons.sorted().forEach{Text(it,color=IlluminedThemeTokens.SecondaryText,fontSize=13.sp)}}}
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
        TextButton(onClick = onBack) { Text("‹ Back") }
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
            text = { Text(it) },
            confirmButton = { TextButton(onClick = clear) { Text("OK") } },
        )
    }
}
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
