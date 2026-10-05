package com.illumined.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.illumined.app.data.ChatMessage
import com.illumined.app.data.InstructorConversation
import com.illumined.app.data.InstructorInboxRepository
import com.illumined.app.data.InboxStudent
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.text.DateFormat

@Composable
internal fun InstructorInboxExperience(userId: String, profile: UserProfile) {
    val es = java.util.Locale.getDefault().language == "es"
    fun t(en: String, spanish: String) = if (es) spanish else en
    val room = profile.selectedClassId
    val repository = remember { InstructorInboxRepository() }
    val preferences = LocalContext.current.getSharedPreferences("instructor-inbox", Context.MODE_PRIVATE)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var threads by remember(userId, room, profile.isInstructor) { mutableStateOf(emptyList<InstructorConversation>()) }
    var selected by remember(userId, room, profile.isInstructor) { mutableStateOf<String?>(null) }
    var recipient by remember(userId, room, profile.isInstructor) { mutableStateOf<InboxStudent?>(null) }
    var choosing by remember(userId, room, profile.isInstructor) { mutableStateOf(false) }
    var students by remember(userId, room, profile.isInstructor) { mutableStateOf(emptyList<InboxStudent>()) }
    var loadingStudents by remember(userId, room, profile.isInstructor) { mutableStateOf(false) }
    var messages by remember(userId, room, profile.isInstructor) { mutableStateOf(emptyList<ChatMessage>()) }
    val drafts = remember(userId, room, profile.isInstructor) { mutableStateMapOf<String, String>() }
    var error by remember(userId, room, profile.isInstructor) { mutableStateOf<String?>(null) }
    var sending by remember(userId, room, profile.isInstructor) { mutableStateOf(false) }
    var limit by remember(selected) { mutableStateOf(100L) }
    val listState = rememberLazyListState()
    val draftKey = selected ?: recipient?.let { "new:${it.id}" } ?: "new"
    val draft = drafts[draftKey].orEmpty()
    DisposableEffect(userId, room, profile.isInstructor, choosing) {
        var active = true
        val listener = if (choosing && profile.isInstructor) {
            loadingStudents = true
            repository.students(room, { if (active) { students = it; loadingStudents = false } }, {
                if (active) { students = emptyList(); loadingStudents = false; error = t("Student list unavailable. Please try again.", "Lista de estudiantes no disponible. Inténtalo de nuevo.") }
            })
        } else null
        onDispose { active = false; listener?.remove() }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(userId, room, profile.isInstructor, profile.removedClassIds, profile.inactiveClassIds) {
        var active = true
        val listener = repository.conversations(room, userId, profile.isInstructor, { result ->
            if (active) { threads = result; if (!profile.isInstructor) selected = result.firstOrNull()?.id }
        }, { if (active) { threads = emptyList(); messages = emptyList(); selected = null; error = t("Inbox unavailable. Please try again.", "Bandeja no disponible. Inténtalo de nuevo.") } })
        onDispose { active = false; listener.remove() }
    }
    DisposableEffect(userId, room, selected, limit) {
        var active = true; messages = emptyList()
        val listener = selected?.let { id -> repository.messages(id, limit, { if (active) messages = it }, {
            if (active) { messages = emptyList(); error = t("Messages unavailable. Please try again.", "Mensajes no disponibles. Inténtalo de nuevo.") }
        }) }
        onDispose { active = false; listener?.remove() }
    }
    LaunchedEffect(messages, resumed) {
        if (resumed && selected != null) messages.lastOrNull()?.timestamp?.let {
            preferences.edit().putLong("$userId:$selected", it.seconds * 1_000_000_000L + it.nanoseconds).apply()
        }
        if (messages.isNotEmpty() && limit == 100L) listState.animateScrollToItem(messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t("Private to the student and all instructors assigned to this classroom. Other students cannot see these messages.",
            "Privado para el estudiante y todos los instructores de esta clase. Los demás estudiantes no pueden ver estos mensajes."), style = MaterialTheme.typography.bodySmall)
        if (profile.isInstructor && selected == null && recipient == null) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Button(onClick = { choosing = true }) { Text(t("New message", "Nuevo mensaje")) } }
                if (choosing) {
                    item { Text(t("Choose a student", "Elegir un estudiante"), style = MaterialTheme.typography.titleMedium) }
                    if (loadingStudents) item { CircularProgressIndicator() }
                    else if (students.isEmpty()) item { Text(t("No active students in this classroom.", "No hay estudiantes activos en esta clase.")) }
                    items(students, key = { "student:${it.id}" }) { student ->
                        OutlinedButton(onClick = {
                            val existing = threads.firstOrNull { it.studentId == student.id }
                            selected = existing?.id; recipient = if (existing == null) student else null; choosing = false
                        }, modifier = Modifier.fillMaxWidth()) { MemberProfilePhoto(student.id); Spacer(Modifier.width(8.dp)); Text(student.name) }
                    }
                    item { TextButton(onClick = { choosing = false }) { Text(t("Cancel", "Cancelar")) } }
                }
                if (threads.isEmpty()) item { Text(t("No student conversations yet.", "Todavía no hay conversaciones de estudiantes.")) }
                items(threads, key = { it.id }) { thread ->
                    val latest = thread.updatedAt?.let { it.seconds * 1_000_000_000L + it.nanoseconds } ?: 0L
                    val unread = thread.lastSenderId != userId && latest > preferences.getLong("$userId:${thread.id}", 0)
                    OutlinedButton(onClick = { selected = thread.id }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Row { MemberProfilePhoto(thread.studentId); Spacer(Modifier.width(8.dp)); Text((if (unread) "● " else "") + thread.studentName) }
                            thread.updatedAt?.let { Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(it.toDate()), style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        } else {
            if (profile.isInstructor) {
                TextButton(onClick = { selected = null; recipient = null }, enabled = !sending) { Text(t("All conversations", "Todas las conversaciones")) }
                Text(recipient?.name ?: threads.firstOrNull { it.id == selected }?.studentName.orEmpty(), style = MaterialTheme.typography.titleMedium)
            }
            LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 18.dp)) {
                if (messages.size >= limit) item { TextButton(onClick = { limit += 100 }) { Text(t("Load earlier messages", "Cargar mensajes anteriores")) } }
                if (messages.isEmpty()) item { Text(t("Send a message to start the conversation.", "Envía un mensaje para iniciar la conversación.")) }
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message, message.senderId == userId)
                }
            }
            ChatComposer(draft, { drafts[draftKey] = it }, sending,
                !sending && draft.isNotBlank(), t("Private message", "Mensaje privado")) {
                    sending = true
                    val existing = selected ?: threads.firstOrNull { it.studentId == (recipient?.id ?: userId) }?.id
                    repository.send(room, userId, profile.displayName, existing, draft, { id ->
                        drafts.remove(draftKey); selected = id; recipient = null; sending = false
                    }, { sending = false; error = t("Message not sent. Your draft is saved; try again.", "No se envió el mensaje. Se conservó tu borrador; inténtalo de nuevo.") }, recipient)
            }
        }
        error?.let { message ->
            AlertDialog(onDismissRequest = { error = null }, title = { Text(t("Chat Error", "Error del chat")) }, text = { Text(message) },
                confirmButton = { TextButton(onClick = { error = null }) { Text(t("OK", "Aceptar")) } })
        }
    }
}
