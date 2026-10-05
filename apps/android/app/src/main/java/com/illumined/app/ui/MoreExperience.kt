package com.illumined.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ListenerRegistration
import com.illumined.app.R
import com.illumined.app.data.ChatMessage
import com.illumined.app.data.ChatRepository
import com.illumined.app.data.AccountDeletionRepository
import com.illumined.app.data.UserProfile
import com.illumined.app.data.ScheduleItem
import com.illumined.app.data.Assignment
import com.illumined.app.data.DiscussionPrompt
import com.illumined.app.ui.theme.IlluminedThemeTokens
import org.json.JSONObject
import java.text.DateFormat

private enum class MorePage { MENU, UPDATES, GUIDES, AWARDS, CHAT, ACCOUNT, NOTIFICATIONS, GAMES, INSTRUCTOR, ADMIN }
internal data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val symbolName: String?,
    val nameEs: String = "",
    val descriptionEs: String = "",
) {
    val localizedName get() = moreT(name, nameEs.ifBlank { name })
    val localizedDescription get() = moreT(description, descriptionEs.ifBlank { description })
}

private fun moreT(english: String, spanish: String): String =
    if (java.util.Locale.getDefault().language == "es") spanish else english

internal fun knownEarnedBadgeCount(badges: List<Badge>, earnedIds: Set<String>) = badges.count { it.id in earnedIds }

@Composable
fun MoreExperience(userId: String, email: String, profile: UserProfile?, schedule: List<ScheduleItem>, assignments: List<Assignment>, prompts: List<DiscussionPrompt>, onSignOut: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(MorePage.MENU) }
    BackHandler(enabled = page != MorePage.MENU) { page = MorePage.MENU }
    val tour = LocalInstructorWalkthrough.current
    val shownPage = if (tour?.active == true) when(tour.screen) {
        "instructor-tools", "classroom-codes" -> MorePage.INSTRUCTOR
        "more" -> MorePage.MENU
        else -> page
    } else page
    when (shownPage) {
        MorePage.MENU -> MoreMenu(profile) { page = it }
        MorePage.UPDATES -> InstructorUpdatesExperience(profile) { page = MorePage.MENU }
        MorePage.GUIDES -> Column(Modifier.fillMaxSize().background(moreBrush()).verticalScroll(rememberScrollState()).padding(16.dp)) {
            PageHeading(moreT("My Guides", "Mis guías")) { page = MorePage.MENU }
            RitePreparationDashboard(profile?.selectedClassId.orEmpty(), userId, library = true)
        }
        MorePage.AWARDS -> AwardsPage(profile, onBack = { page = MorePage.MENU })
        MorePage.CHAT -> ChatPage(userId, profile, onBack = { page = MorePage.MENU })
        MorePage.ACCOUNT -> AccountPage(email, profile, onSignOut, onBack = { page = MorePage.MENU })
        MorePage.NOTIFICATIONS -> NotificationSettingsExperience(profile) { page = MorePage.MENU }
        MorePage.GAMES -> FormationGamesExperience { page = MorePage.MENU }
        MorePage.INSTRUCTOR -> if (profile?.isInstructor == true) InstructorExperience(
            profile, schedule, assignments, prompts, onBack = { page = MorePage.MENU },
        ) else InformationalPage(moreT("Instructor Tools", "Herramientas del instructor"), moreT("Instructor access is required.", "Se requiere acceso de instructor."), { page = MorePage.MENU })
        MorePage.ADMIN -> if (profile?.isAdmin == true) AccessCodeExperience(
            profile, parishMode = true, onBack = { page = MorePage.MENU },
        ) else InformationalPage(moreT("Admin Tools", "Herramientas administrativas"), moreT("Administrator access is required.", "Se requiere acceso de administrador."), { page = MorePage.MENU })
    }
}

@Composable
private fun MoreMenu(profile: UserProfile?, navigate: (MorePage) -> Unit) {
    val rows = buildList {
        if (profile?.isInstructor == true) add(Triple(moreT("Instructor Tools", "Herramientas del instructor"), moreT("Manage announcements, schedule, assignments, and student progress.", "Administra anuncios, calendario, tareas y progreso de estudiantes."), MorePage.INSTRUCTOR))
        add(Triple(moreT("Chat", "Chat"), moreT("Open your OCIA classroom conversation.", "Abre la conversación de tu clase de OICA."), MorePage.CHAT))
        add(Triple(moreT("Account", "Cuenta"), moreT("View your profile and sign out.", "Consulta tu perfil y cierra sesión."), MorePage.ACCOUNT))
        add(Triple(moreT("My Guides", "Mis guías"), moreT("Revisit your class preparation guides.", "Consulta las guías de preparación de tu clase."), MorePage.GUIDES))
        add(Triple(moreT("Awards", "Premios"), moreT("View badges, achievements, and memorized prayers.", "Consulta insignias, logros y oraciones memorizadas."), MorePage.AWARDS))
        add(Triple(moreT("Games", "Juegos"), moreT("Practice virtue terms with matching and quiz games.", "Practica términos de virtudes con juegos de asociación y cuestionarios."), MorePage.GAMES))
        if (profile?.isAdmin == true) add(Triple(moreT("Admin Tools", "Herramientas administrativas"), moreT("Create first-instructor setup codes for new parishes.", "Crea códigos de configuración para el primer instructor de una parroquia nueva."), MorePage.ADMIN))
    }
    val tour = LocalInstructorWalkthrough.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    fun anchor(destination: MorePage) = if(destination == MorePage.INSTRUCTOR) "more" else "more-" + destination.name.lowercase()
    LaunchedEffect(tour?.target) {
        if(tour?.active == true && tour.screen == "more") {
            val index = rows.indexOfFirst { anchor(it.third) == tour.target }
            if(index >= 0) listState.animateScrollToItem(index)
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(moreBrush()), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(rows) { (title, subtitle, destination) ->
            Box(Modifier.walkthroughAnchor(anchor(destination))) {
                MoreCard(title, subtitle) { navigate(destination) }
            }
        }
    }
}

@Composable
private fun MoreCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                MoreMenuSymbol(moreMenuSymbol(title), IlluminedThemeTokens.Gold, Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp)); Text(subtitle, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
            }
            LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
        }
    }
}

@Composable
private fun AwardsPage(profile: UserProfile?, onBack: () -> Unit) {
    val context = LocalContext.current
    val catalog = remember { runCatching {
        val badges = JSONObject(context.resources.openRawResource(R.raw.achievements).bufferedReader().use { it.readText() }).getJSONArray("badges").let { a ->
            (0 until a.length()).map { i -> a.getJSONObject(i).let {
                Badge(
                    id = it.getString("id"),
                    name = it.getString("name"),
                    description = it.getString("description"),
                    symbolName = it.optString("symbolName").takeIf(String::isNotBlank),
                    nameEs = it.optString("nameEs"),
                    descriptionEs = it.optString("descriptionEs"),
                )
            } }
        }
        val prayerNames = JSONObject(context.resources.openRawResource(R.raw.spiritual_formation).bufferedReader().use { it.readText() }).getJSONArray("commonPrayers").let { a ->
            (0 until a.length()).associate { i -> a.getJSONObject(i).let {
                it.getString("id") to moreT(it.getString("title"), it.optString("titleEs", it.getString("title")))
            } }
        }
        badges to prayerNames
    } }
    val badges = catalog.getOrNull()?.first.orEmpty()
    val prayerNames = catalog.getOrNull()?.second.orEmpty()
    val earned = profile?.earnedBadges.orEmpty()
    val memorizedNames = profile?.memorizedPrayerIds.orEmpty().mapNotNull(prayerNames::get).sortedBy(String::lowercase)
    val earnedCount = knownEarnedBadgeCount(badges, earned)
    Column(Modifier.fillMaxSize().background(moreBrush())) {
        PageHeading(moreT("Awards", "Premios"), onBack)
        if (catalog.isFailure) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(moreT("Achievements Unavailable", "Logros no disponibles"), color = Color.Red) }; return@Column }
        LazyVerticalGrid(columns = GridCells.Adaptive(155.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(moreT("Achievement Board", "Tablero de logros"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(moreT("$earnedCount of ${badges.size} badges earned", "$earnedCount de ${badges.size} insignias obtenidas"), color = IlluminedThemeTokens.SecondaryText)
                        LinearProgressIndicator(progress = { if (badges.isEmpty()) 0f else earnedCount.toFloat() / badges.size }, Modifier.fillMaxWidth(), color = IlluminedThemeTokens.Gold)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) { LessonSymbol(LessonSymbolKind.BookClosed, IlluminedThemeTokens.Gold, Modifier.size(23.dp)) }
                            Spacer(Modifier.width(12.dp)); Column { Text(moreT("Prayer Memorization", "Memorización de oraciones"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(moreT("${memorizedNames.size} of ${maxOf(prayerNames.size, memorizedNames.size)} common prayers memorized", "${memorizedNames.size} de ${maxOf(prayerNames.size, memorizedNames.size)} oraciones comunes memorizadas"), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText) }
                        }
                        LinearProgressIndicator(progress = { if (prayerNames.isEmpty()) 0f else memorizedNames.size.toFloat() / maxOf(prayerNames.size, memorizedNames.size) }, Modifier.fillMaxWidth(), color = IlluminedThemeTokens.Gold)
                        if (memorizedNames.isEmpty()) Text(moreT("No common prayers marked memorized yet.", "Todavía no hay oraciones comunes marcadas como memorizadas."), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                        else { Text(moreT("Memorized", "Memorizadas"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold); memorizedNames.forEach { name -> Row(verticalAlignment = Alignment.CenterVertically) { LessonSymbol(LessonSymbolKind.CheckCircle, IlluminedThemeTokens.Blue, Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text(name, fontSize = 13.sp, color = IlluminedThemeTokens.Blue) } } }
                    }
                }
            }
            items(badges, key = { it.id }) { badge -> val isEarned = badge.id in earned
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(if (isEarned) .96f else .76f), shadowElevation = if (isEarned) 12.dp else 6.dp, border = BorderStroke(1.dp, if (isEarned) IlluminedThemeTokens.Gold.copy(.35f) else Color.Gray.copy(.18f))) {
                    Column(Modifier.fillMaxWidth().heightIn(min = 230.dp).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(68.dp).background(if (isEarned) IlluminedThemeTokens.Gold.copy(.18f) else Color.Gray.copy(.12f), CircleShape), contentAlignment = Alignment.Center) { AwardSymbol(if (isEarned) awardSymbolKind(badge.symbolName) else AwardSymbolKind.Lock, if (isEarned) IlluminedThemeTokens.Gold else IlluminedThemeTokens.SecondaryText, Modifier.size(30.dp)) }
                        Text(badge.localizedName, textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (isEarned) IlluminedThemeTokens.Ink else IlluminedThemeTokens.SecondaryText)
                        Text(badge.localizedDescription, textAlign = TextAlign.Center, fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                        Text(if (isEarned) moreT("Earned", "Obtenida") else moreT("Locked", "Bloqueada"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isEarned) Color(0xFF2E7D32) else Color.Gray, modifier = Modifier.background((if (isEarned) Color(0xFF2E7D32) else Color.Gray).copy(.12f), RoundedCornerShape(30.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatPage(userId: String, profile: UserProfile?, initialInbox: Boolean = false, onBack: () -> Unit) {
    val repository = remember { ChatRepository() }; val classId = profile?.selectedClassId.orEmpty()
    var instructorInbox by remember(userId, classId) { mutableStateOf(initialInbox) }
    var replying by remember(userId, classId) { mutableStateOf<ChatMessage?>(null) }
    var editing by remember(userId, classId) { mutableStateOf<ChatMessage?>(null) }
    var editText by remember { mutableStateOf("") }
    var deleting by remember(userId, classId) { mutableStateOf<ChatMessage?>(null) }
    val spanish = java.util.Locale.getDefault().language == "es"
    val listState = rememberLazyListState()
    var messages by remember(userId, classId) { mutableStateOf(emptyList<ChatMessage>()) }; var draft by rememberSaveable(userId, classId) { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; var sending by remember { mutableStateOf(false) }
    DisposableEffect(userId, classId) { var active = true; messages = emptyList(); var registration: ListenerRegistration? = null; if (classId.isNotBlank()) registration = repository.listen(classId, { if (active) messages = it }, { if (active) error = it.localizedMessage ?: if (spanish) "No se pudo cargar el chat." else "Chat could not be loaded." }); onDispose { active = false; registration?.remove() } }
    LaunchedEffect(messages.lastOrNull()?.id) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    markClassroomMessagesRead(userId, classId, messages, !instructorInbox)
    Column(Modifier.fillMaxSize().background(moreBrush())) {
        PageHeading("Chat", onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            FilterChip(selected = !instructorInbox, onClick = { instructorInbox = false }, label = { Text(if (spanish) "Chat de la clase" else "Classroom chat") })
            FilterChip(selected = instructorInbox, onClick = { instructorInbox = true }, label = { Text(if (profile?.isInstructor == true) { if (spanish) "Bandeja de entrada" else "Inbox" } else { if (spanish) "Contactar al instructor" else "Message instructor" }) })
        }
        if (instructorInbox && profile != null) {
            InstructorInboxExperience(userId, profile)
        } else {
        Column(Modifier.fillMaxWidth().background(Color.White.copy(.88f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                HomeSymbol(HomeSymbolKind.ClassMembers, IlluminedThemeTokens.Gold, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (classId.isBlank()) (if(spanish) "Clase" else "Classroom") else classId, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(if(spanish) "Conversación de la clase de OCIA" else "OCIA classroom conversation", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                }
            }
            HorizontalDivider(color = IlluminedThemeTokens.Gold.copy(.22f))
        }
        LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (messages.isEmpty()) item { Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { ChatSymbol(ChatSymbolKind.MessageBadge, IlluminedThemeTokens.Gold, Modifier.size(34.dp)); Text(if(spanish) "Todavía no hay mensajes" else "No messages yet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Text(if(spanish) "Inicia la conversación con tu clase de OCIA." else "Start the conversation with your OCIA class.", fontSize = 15.sp, textAlign = TextAlign.Center, color = IlluminedThemeTokens.SecondaryText) } } }
            items(messages, key = { it.id }) { message ->
                Column {
                    message.replyTo?.let { id ->
                        val parent = messages.firstOrNull { it.id == id }
                        Text(parent?.let { "${it.senderName}: ${it.message.take(160)}" } ?: if (spanish) "Respuesta a un mensaje anterior o eliminado" else "Reply to an earlier or deleted message", style = MaterialTheme.typography.bodySmall, color = IlluminedThemeTokens.SecondaryText)
                    }
                    ChatBubble(message, message.senderId == userId)
                    if (message.editedAt != null) Text(if (spanish) "Editado" else "Edited", style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { replying = message }) { Text(if (spanish) "Responder" else "Reply") }
                        listOf("🙏", "❤️", "👍").forEach { emoji ->
                            val count = message.reactions.values.count { it == emoji }
                            TextButton(onClick = { repository.react(message, emoji) { error = it.localizedMessage } }, contentPadding = PaddingValues(4.dp)) {
                                Text(emoji + if (count > 0) " $count" else "")
                            }
                        }
                        if (message.senderId == userId || profile?.isInstructor == true) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.semantics { contentDescription = if(spanish) "Opciones del mensaje" else "Message options" }) { Text("⋯") }
                                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                                    if (message.senderId == userId) DropdownMenuItem(text = { Text(if (spanish) "Editar" else "Edit") }, onClick = { expanded = false; editing = message; editText = message.message })
                                    DropdownMenuItem(text = { Text(if (spanish) "Eliminar" else "Delete") }, onClick = { expanded = false; deleting = message })
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Color.White.copy(.9f))) {
            HorizontalDivider(color = IlluminedThemeTokens.Gold.copy(.18f))
            replying?.let { message -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text((if(spanish) "Respondiendo a: " else "Replying to: ") + message.senderName, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { replying = null }) { Text(if(spanish) "Cancelar" else "Cancel") }
            } }
            ChatComposer(draft, { draft = it }, sending,
                ChatPresentation.canSend(draft, profile != null, sending),
                if(spanish) "Escribe a tu clase" else "Message your class") {
                val text = draft; sending = true
                repository.send(classId, profile?.displayName.orEmpty(), text,
                    { sending = false; draft = ""; replying = null },
                    { sending = false; error = it.localizedMessage ?: if(spanish) "No se pudo enviar el mensaje." else "Message could not be sent." }, replying?.id)
            }
        }
    }
    }
    editing?.let { message -> AlertDialog(onDismissRequest = { editing = null }, title = { Text(if(spanish) "Editar mensaje" else "Edit message") }, text = {
        OutlinedTextField(editText, { if(it.length <= 4000) editText = it }, maxLines = 6)
    }, confirmButton = { TextButton(onClick = { repository.edit(message, editText) { error = it.localizedMessage }; editing = null }, enabled = editText.isNotBlank()) { Text(if(spanish) "Guardar" else "Save") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text(if(spanish) "Cancelar" else "Cancel") } }) }
    deleting?.let { message -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(if(spanish) "¿Eliminar este mensaje para todos?" else "Delete this message for everyone?") }, confirmButton = { TextButton(onClick = { repository.delete(message) { error = it.localizedMessage }; deleting = null }) { Text(if(spanish) "Eliminar" else "Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text(if(spanish) "Cancelar" else "Cancel") } }) }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text(if (spanish) "Error del chat" else "Chat Error") }, text = { Text(localizedUserMessage(message)) }, confirmButton = { TextButton(onClick = { error = null }) { Text(if (spanish) "Aceptar" else "OK") } }) }
}

@Composable
internal fun ChatBubble(message: ChatMessage, mine: Boolean) {
    Row(Modifier.fillMaxWidth()) { if (mine) Spacer(Modifier.weight(1f)); Column(Modifier.widthIn(max = 290.dp), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { MemberProfilePhoto(message.senderId, 28); Text(message.senderName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (mine) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Ink); Text(message.timestamp?.toDate()?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(it) }.orEmpty(), fontSize = 11.sp, color = IlluminedThemeTokens.SecondaryText) }
        Spacer(Modifier.height(5.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = if (mine) IlluminedThemeTokens.Blue else Color.White.copy(.94f), shadowElevation = 4.dp, border = if (mine) null else BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.20f))) { ChatMessageText(message.message, mine) }
    }; if (!mine) Spacer(Modifier.weight(1f)) }
}

@Composable
private fun ChatMessageText(message: String, mine: Boolean) {
    val textColor = if (mine) Color.White else IlluminedThemeTokens.Ink
    val linkColor = if (mine) Color.White else IlluminedThemeTokens.Blue
    val linkedMessage = remember(message, mine) {
        buildAnnotatedString {
            var cursor = 0
            ChatPresentation.linksIn(message).forEach { link ->
                append(message.substring(cursor, link.start))
                withLink(
                    LinkAnnotation.Url(
                        url = link.url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                    ),
                ) {
                    append(message.substring(link.start, link.endExclusive))
                }
                cursor = link.endExclusive
            }
            append(message.substring(cursor))
        }
    }

    Text(
        text = linkedMessage,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        color = textColor,
        fontSize = 17.sp,
        lineHeight = 20.sp,
    )
}

@Composable
internal fun AccountPage(email: String, profile: UserProfile?, onSignOut: () -> Unit, onBack: () -> Unit) {
    val deletionRepository = remember { AccountDeletionRepository() }
    val uriHandler = LocalUriHandler.current
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var deletionPassword by rememberSaveable { mutableStateOf("") }
    var deletionWorking by remember { mutableStateOf(false) }
    var deletionError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(moreBrush()).verticalScroll(rememberScrollState())) {
        PageHeading(moreT("Account", "Cuenta"), onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (profile != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccountPhotoButton(profile.userId)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(profile.displayName, fontSize = 24.sp, fontWeight = FontWeight.SemiBold); Text(profile.selectedClassId.ifBlank { moreT("No class assigned", "No hay una clase asignada") }, fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText) }
                        }
                        HorizontalDivider()
                        AccountDetailRow(AccountSymbolKind.Person, moreT("Name", "Nombre"), profile.displayName)
                        AccountDetailRow(AccountSymbolKind.Envelope, moreT("Email", "Correo electrónico"), profile.email.ifBlank { email })
                        AccountDetailRow(AccountSymbolKind.ClassMembers, moreT("Class", "Clase"), profile.selectedClassId.ifBlank { moreT("Not assigned", "Sin asignar") })
                        AccountDetailRow(if (profile.isInstructor) AccountSymbolKind.Instructor else AccountSymbolKind.Student, moreT("Role", "Rol"), if (profile.isInstructor) moreT("Instructor", "Instructor") else moreT("Student", "Estudiante"))
                    }
                }
                NotificationSettingsExperience(profile, embedded = true, onBack = {})
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { AccountSymbol(AccountSymbolKind.ProfileAlert, IlluminedThemeTokens.Gold, Modifier.size(38.dp)); Text(moreT("Profile Needed", "Se necesita un perfil"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(moreT("Your profile will appear here after setup.", "Tu perfil aparecerá aquí después de configurarlo."), textAlign = TextAlign.Center, color = IlluminedThemeTokens.SecondaryText) }
                }
            }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red.copy(.18f)), shape = RoundedCornerShape(16.dp)) { AccountSymbol(AccountSymbolKind.SignOut, Color.Red, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.action_sign_out), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), border = BorderStroke(1.dp, Color.Red.copy(.18f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(moreT("Delete Account", "Eliminar cuenta"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
                    Text(moreT("Permanently delete your Illumined account and associated personal data.", "Elimina permanentemente tu cuenta de Illumined y los datos personales asociados."), fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText)
                    OutlinedButton(
                        onClick = { deletionPassword = ""; deletionError = null; showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red.copy(.32f)),
                    ) { Text(moreT("Delete My Account", "Eliminar mi cuenta")) }
                    TextButton(onClick = { uriHandler.openUri(AccountDeletionPresentation.WebUrl) }) {
                        Text(moreT("Account deletion information", "Información sobre la eliminación de la cuenta"))
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!deletionWorking) showDeleteConfirmation = false },
            title = { Text(moreT("Permanently Delete Account?", "¿Eliminar la cuenta permanentemente?")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(moreT(AccountDeletionPresentation.Warning, "Esto elimina permanentemente tu cuenta de Illumined, perfil, progreso, mensajes, respuestas de discusión y peticiones de oración. Esta acción no se puede deshacer."))
                    Text(moreT(AccountDeletionPresentation.SharedContentNotice, "Los materiales de toda la clase creados por un instructor pueden seguir disponibles para la clase sin la identidad del instructor."), fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                    OutlinedTextField(
                        value = deletionPassword,
                        onValueChange = { deletionPassword = it; deletionError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(moreT("Password", "Contraseña")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !deletionWorking,
                    )
                    deletionError?.let { Text(localizedUserMessage(it), color = Color.Red, fontSize = 13.sp) }
                    if (deletionWorking) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.Red)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletionWorking = true
                        deletionError = null
                        deletionRepository.deleteAccount(
                            deletionPassword,
                            onSuccess = { deletionWorking = false; showDeleteConfirmation = false; onSignOut() },
                            onError = { deletionWorking = false; deletionError = AccountDeletionPresentation.errorMessage(it) },
                        )
                    },
                    enabled = AccountDeletionPresentation.canDelete(deletionPassword, deletionWorking),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                ) { Text(if (deletionWorking) moreT("Deleting…", "Eliminando…") else moreT("Delete Permanently", "Eliminar permanentemente")) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }, enabled = !deletionWorking) { Text(moreT("Cancel", "Cancelar")) } },
        )
    }
}

@Composable
private fun AccountDetailRow(icon: AccountSymbolKind, title: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) { AccountSymbol(icon, IlluminedThemeTokens.Gold, Modifier.size(15.dp)) }
        Spacer(Modifier.width(12.dp)); Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f)); Text(value, fontSize = 16.sp, color = IlluminedThemeTokens.SecondaryText, textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 190.dp))
    }
}

@Composable private fun InformationalPage(title: String, text: String, onBack: () -> Unit) { Column(Modifier.fillMaxSize().background(moreBrush())) { PageHeading(title, onBack); Box(Modifier.padding(16.dp)) { MoreCard(title, text) {} } } }
@Composable private fun PageHeading(title: String, onBack: () -> Unit) { Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text(moreT("‹ Back", "‹ Volver")) }; Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) } }
private fun moreBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
