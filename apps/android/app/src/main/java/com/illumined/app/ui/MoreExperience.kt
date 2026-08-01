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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
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
import com.illumined.app.data.UserProfile
import com.illumined.app.data.ScheduleItem
import com.illumined.app.data.Assignment
import com.illumined.app.data.DiscussionPrompt
import com.illumined.app.ui.theme.IlluminedThemeTokens
import org.json.JSONObject
import java.text.DateFormat

private enum class MorePage { MENU, AWARDS, CHAT, ACCOUNT, NOTIFICATIONS, GAMES, INSTRUCTOR, ADMIN }
internal data class Badge(val id: String, val name: String, val description: String, val symbolName: String?)

internal fun knownEarnedBadgeCount(badges: List<Badge>, earnedIds: Set<String>) = badges.count { it.id in earnedIds }

@Composable
fun MoreExperience(userId: String, email: String, profile: UserProfile?, schedule: List<ScheduleItem>, assignments: List<Assignment>, prompts: List<DiscussionPrompt>, onSignOut: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(MorePage.MENU) }
    BackHandler(enabled = page != MorePage.MENU) { page = MorePage.MENU }
    when (page) {
        MorePage.MENU -> MoreMenu(profile) { page = it }
        MorePage.AWARDS -> AwardsPage(profile, onBack = { page = MorePage.MENU })
        MorePage.CHAT -> ChatPage(userId, profile, onBack = { page = MorePage.MENU })
        MorePage.ACCOUNT -> AccountPage(email, profile, onSignOut, onBack = { page = MorePage.MENU })
        MorePage.NOTIFICATIONS -> NotificationSettingsExperience(profile) { page = MorePage.MENU }
        MorePage.GAMES -> FormationGamesExperience { page = MorePage.MENU }
        MorePage.INSTRUCTOR -> if (profile?.isInstructor == true) InstructorExperience(
            profile, schedule, assignments, prompts, onBack = { page = MorePage.MENU },
        ) else InformationalPage("Instructor Tools", "Instructor access is required.", { page = MorePage.MENU })
        MorePage.ADMIN -> if (profile?.isAdmin == true) AccessCodeExperience(
            profile, parishMode = true, onBack = { page = MorePage.MENU },
        ) else InformationalPage("Admin Tools", "Administrator access is required.", { page = MorePage.MENU })
    }
}

@Composable
private fun MoreMenu(profile: UserProfile?, navigate: (MorePage) -> Unit) {
    val rows = buildList {
        add(Triple("Awards", "View badges, achievements, and memorized prayers.", MorePage.AWARDS))
        add(Triple("Chat", "Open your OCIA classroom conversation.", MorePage.CHAT))
        add(Triple("Account", "View your profile and sign out.", MorePage.ACCOUNT))
        add(Triple("Notifications", "Turn on class announcements, assignments, and discussion alerts.", MorePage.NOTIFICATIONS))
        add(Triple("Games", "Practice virtue terms with matching and quiz games.", MorePage.GAMES))
        if (profile?.isInstructor == true) add(Triple("Instructor Tools", "Manage announcements, schedule, assignments, and student progress.", MorePage.INSTRUCTOR))
        if (profile?.isAdmin == true) add(Triple("Admin Tools", "Create first-instructor setup codes for new parishes.", MorePage.ADMIN))
    }
    LazyColumn(Modifier.fillMaxSize().background(moreBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(rows) { (title, subtitle, destination) -> MoreCard(title, subtitle) { navigate(destination) } }
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
            (0 until a.length()).map { i -> a.getJSONObject(i).let { Badge(it.getString("id"), it.getString("name"), it.getString("description"), it.optString("symbolName").takeIf(String::isNotBlank)) } }
        }
        val prayerNames = JSONObject(context.resources.openRawResource(R.raw.spiritual_formation).bufferedReader().use { it.readText() }).getJSONArray("commonPrayers").let { a ->
            (0 until a.length()).associate { i -> a.getJSONObject(i).let { it.getString("id") to it.getString("title") } }
        }
        badges to prayerNames
    } }
    val badges = catalog.getOrNull()?.first.orEmpty()
    val prayerNames = catalog.getOrNull()?.second.orEmpty()
    val earned = profile?.earnedBadges.orEmpty()
    val memorizedNames = profile?.memorizedPrayerIds.orEmpty().mapNotNull(prayerNames::get).sortedBy(String::lowercase)
    val earnedCount = knownEarnedBadgeCount(badges, earned)
    Column(Modifier.fillMaxSize().background(moreBrush())) {
        PageHeading("Awards", onBack)
        if (catalog.isFailure) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Achievements Unavailable", color = Color.Red) }; return@Column }
        LazyVerticalGrid(columns = GridCells.Adaptive(155.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Achievement Board", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text("$earnedCount of ${badges.size} badges earned", color = IlluminedThemeTokens.SecondaryText)
                        LinearProgressIndicator(progress = { if (badges.isEmpty()) 0f else earnedCount.toFloat() / badges.size }, Modifier.fillMaxWidth(), color = IlluminedThemeTokens.Gold)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) { LessonSymbol(LessonSymbolKind.BookClosed, IlluminedThemeTokens.Gold, Modifier.size(23.dp)) }
                            Spacer(Modifier.width(12.dp)); Column { Text("Prayer Memorization", fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text("${memorizedNames.size} of ${maxOf(prayerNames.size, memorizedNames.size)} common prayers memorized", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText) }
                        }
                        LinearProgressIndicator(progress = { if (prayerNames.isEmpty()) 0f else memorizedNames.size.toFloat() / maxOf(prayerNames.size, memorizedNames.size) }, Modifier.fillMaxWidth(), color = IlluminedThemeTokens.Gold)
                        if (memorizedNames.isEmpty()) Text("No common prayers marked memorized yet.", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                        else { Text("Memorized", fontSize = 14.sp, fontWeight = FontWeight.SemiBold); memorizedNames.forEach { name -> Row(verticalAlignment = Alignment.CenterVertically) { LessonSymbol(LessonSymbolKind.CheckCircle, IlluminedThemeTokens.Blue, Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text(name, fontSize = 13.sp, color = IlluminedThemeTokens.Blue) } } }
                    }
                }
            }
            items(badges, key = { it.id }) { badge -> val isEarned = badge.id in earned
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(if (isEarned) .96f else .76f), shadowElevation = if (isEarned) 12.dp else 6.dp, border = BorderStroke(1.dp, if (isEarned) IlluminedThemeTokens.Gold.copy(.35f) else Color.Gray.copy(.18f))) {
                    Column(Modifier.fillMaxWidth().heightIn(min = 230.dp).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(68.dp).background(if (isEarned) IlluminedThemeTokens.Gold.copy(.18f) else Color.Gray.copy(.12f), CircleShape), contentAlignment = Alignment.Center) { AwardSymbol(if (isEarned) awardSymbolKind(badge.symbolName) else AwardSymbolKind.Lock, if (isEarned) IlluminedThemeTokens.Gold else IlluminedThemeTokens.SecondaryText, Modifier.size(30.dp)) }
                        Text(badge.name, textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (isEarned) IlluminedThemeTokens.Ink else IlluminedThemeTokens.SecondaryText)
                        Text(badge.description, textAlign = TextAlign.Center, fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                        Text(if (isEarned) "Earned" else "Locked", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isEarned) Color(0xFF2E7D32) else Color.Gray, modifier = Modifier.background((if (isEarned) Color(0xFF2E7D32) else Color.Gray).copy(.12f), RoundedCornerShape(30.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatPage(userId: String, profile: UserProfile?, onBack: () -> Unit) {
    val repository = remember { ChatRepository() }; val classId = profile?.classIds?.firstOrNull().orEmpty()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf(emptyList<ChatMessage>()) }; var draft by rememberSaveable { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }; var sending by remember { mutableStateOf(false) }
    DisposableEffect(classId) { var registration: ListenerRegistration? = null; if (classId.isNotBlank()) registration = repository.listen(classId, { messages = it }, { error = it.localizedMessage ?: "Chat could not be loaded." }); onDispose { registration?.remove() } }
    LaunchedEffect(messages.lastOrNull()?.id) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    Column(Modifier.fillMaxSize().background(moreBrush())) {
        PageHeading("Chat", onBack)
        Column(Modifier.fillMaxWidth().background(Color.White.copy(.88f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                HomeSymbol(HomeSymbolKind.ClassMembers, IlluminedThemeTokens.Gold, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (classId.isBlank()) "Classroom" else classId, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("OCIA classroom conversation", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)
                }
            }
            HorizontalDivider(color = IlluminedThemeTokens.Gold.copy(.22f))
        }
        LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (messages.isEmpty()) item { Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { ChatSymbol(ChatSymbolKind.MessageBadge, IlluminedThemeTokens.Gold, Modifier.size(34.dp)); Text("No messages yet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Text("Start the conversation with your OCIA class.", fontSize = 15.sp, textAlign = TextAlign.Center, color = IlluminedThemeTokens.SecondaryText) } } }
            items(messages, key = { it.id }) { message -> ChatBubble(message, message.senderId == userId) }
        }
        Column(Modifier.fillMaxWidth().background(Color.White.copy(.9f))) {
            HorizontalDivider(color = IlluminedThemeTokens.Gold.copy(.18f))
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(draft, { draft = it }, Modifier.weight(1f), placeholder = { Text("Message your class") }, minLines = 1, maxLines = 4, shape = RoundedCornerShape(18.dp))
                Spacer(Modifier.width(10.dp)); Button(onClick = { val text = draft; draft = ""; sending = true; repository.send(classId, profile?.displayName.orEmpty(), text, { sending = false }, { sending = false; error = it.localizedMessage ?: "Message could not be sent." }) }, enabled = ChatPresentation.canSend(draft, profile != null, sending), modifier = Modifier.size(42.dp).semantics { contentDescription = "Send message" }, contentPadding = PaddingValues(0.dp), shape = CircleShape) { ChatSymbol(ChatSymbolKind.PaperPlane, Color.White, Modifier.size(18.dp)) }
            }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text("Chat Error") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
}

@Composable
private fun ChatBubble(message: ChatMessage, mine: Boolean) {
    Row(Modifier.fillMaxWidth()) { if (mine) Spacer(Modifier.weight(1f)); Column(Modifier.widthIn(max = 290.dp), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(message.senderName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (mine) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Ink); Text(message.timestamp?.toDate()?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(it) }.orEmpty(), fontSize = 11.sp, color = IlluminedThemeTokens.SecondaryText) }
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
private fun AccountPage(email: String, profile: UserProfile?, onSignOut: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(moreBrush()).verticalScroll(rememberScrollState())) {
        PageHeading("Account", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (profile != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccountSymbol(AccountSymbolKind.Avatar, IlluminedThemeTokens.Blue, Modifier.size(48.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(profile.displayName, fontSize = 24.sp, fontWeight = FontWeight.SemiBold); Text(profile.classIds.firstOrNull().orEmpty().ifBlank { "No class assigned" }, fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText) }
                        }
                        HorizontalDivider()
                        AccountDetailRow(AccountSymbolKind.Person, "Name", profile.displayName)
                        AccountDetailRow(AccountSymbolKind.Envelope, "Email", profile.email.ifBlank { email })
                        AccountDetailRow(AccountSymbolKind.ClassMembers, "Class", profile.classIds.firstOrNull().orEmpty().ifBlank { "Not assigned" })
                        AccountDetailRow(if (profile.isInstructor) AccountSymbolKind.Instructor else AccountSymbolKind.Student, "Role", MorePresentation.roleText(profile.isInstructor))
                    }
                }
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Formation", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        AccountDetailRow(AccountSymbolKind.Book, "Lessons Completed", profile.completedLessons.size.toString())
                        AccountDetailRow(AccountSymbolKind.Rosette, "Badges Earned", profile.earnedBadges.size.toString())
                    }
                }
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { AccountSymbol(AccountSymbolKind.ProfileAlert, IlluminedThemeTokens.Gold, Modifier.size(38.dp)); Text("Profile Needed", fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text("Your profile will appear here after setup.", textAlign = TextAlign.Center, color = IlluminedThemeTokens.SecondaryText) }
                }
            }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red.copy(.18f)), shape = RoundedCornerShape(16.dp)) { AccountSymbol(AccountSymbolKind.SignOut, Color.Red, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Sign Out", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        }
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
@Composable private fun PageHeading(title: String, onBack: () -> Unit) { Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("‹ Back") }; Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) } }
private fun moreBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
