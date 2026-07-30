package com.illumined.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ListenerRegistration
import com.illumined.app.data.*
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.text.DateFormat

@Composable
fun DiscussionExperience(
    userId: String,
    profile: UserProfile?,
    prompts: List<DiscussionPrompt>,
    assignments: List<Assignment>,
    loadError: String?,
    onCompleteAssignment: (Assignment, () -> Unit, () -> Unit) -> Unit,
) {
    var selected by remember { mutableStateOf<DiscussionPrompt?>(null) }
    BackHandler(enabled = selected != null) { selected = null }
    if (selected != null) DiscussionBoard(
        selected!!, userId, profile,
        linkedAssignments = matchingDiscussionAssignments(selected!!.lessonId, assignments),
        onCompleteAssignment = onCompleteAssignment,
        onBack = { selected = null },
    )
    else DiscussionPromptList(prompts, loadError) { selected = it }
}

internal fun matchingDiscussionAssignments(lessonId: String, assignments: List<Assignment>): List<Assignment> =
    assignments.filter { assignment -> assignment.lessonLinks.any { it.lessonId == lessonId } }

@Composable
private fun DiscussionPromptList(prompts: List<DiscussionPrompt>, error: String?, select: (DiscussionPrompt) -> Unit) {
    if (error != null) {
        DiscussionUnavailable(DiscussionPresentation.errorTitle, error, DiscussionSymbolKind.Warning)
        return
    }
    if (prompts.isEmpty()) {
        DiscussionUnavailable(DiscussionPresentation.emptyTitle, DiscussionPresentation.emptyDescription, DiscussionSymbolKind.Bubble)
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(discussionBrush()), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { DiscussionCard {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){DiscussionSymbol(DiscussionSymbolKind.Bubble,IlluminedThemeTokens.Blue,Modifier.size(22.dp));Text("Discussion Board", color = IlluminedThemeTokens.Blue, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)}
            Spacer(Modifier.height(8.dp)); Text("Return to discussion assignments, read classmates' responses, and post your own reflections.", color = IlluminedThemeTokens.SecondaryText, lineHeight = 22.sp)
        } }
        items(prompts, key = { it.id }) { prompt -> Surface(onClick = { select(prompt) }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
                    DiscussionBubbleBadge()
                    Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(prompt.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(prompt.lessonTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                        Text(prompt.prompt, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText, maxLines = 3)
                    }; DiscussionSymbol(DiscussionSymbolKind.Chevron, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp, 20.dp))
                }
            } }
    }
}

@Composable
private fun DiscussionUnavailable(title: String, description: String, symbol: DiscussionSymbolKind) {
    Box(Modifier.fillMaxSize().background(discussionBrush()).padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DiscussionSymbol(symbol, IlluminedThemeTokens.Gold, Modifier.size(38.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = IlluminedThemeTokens.SecondaryText, lineHeight = 21.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun DiscussionBubbleBadge() {
    val gold = IlluminedThemeTokens.Gold
    Box(Modifier.size(42.dp).background(gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
        DiscussionSymbol(DiscussionSymbolKind.Bubble, gold, Modifier.size(23.dp))
    }
}

@Composable
internal fun DiscussionBoard(
    prompt: DiscussionPrompt,
    userId: String,
    profile: UserProfile?,
    linkedAssignments: List<Assignment>,
    onCompleteAssignment: (Assignment, () -> Unit, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val repository = remember { DiscussionRepository() }; val classId = profile?.classIds?.firstOrNull().orEmpty()
    var posts by remember { mutableStateOf(emptyList<DiscussionPost>()) }; var replies by remember { mutableStateOf(emptyList<DiscussionReply>()) }
    var draft by rememberSaveable(prompt.id) { mutableStateOf("") }; var working by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val myPost = posts.firstOrNull { it.authorId == userId }
    DisposableEffect(prompt.id, classId) {
        var postListener: ListenerRegistration? = null; var replyListener: ListenerRegistration? = null
        if (classId.isNotBlank()) { postListener = repository.listenPosts(prompt.id, classId, { posts = it }, { error = "Responses could not be loaded." }); replyListener = repository.listenReplies(prompt.id, classId, { replies = it }, { error = "Replies could not be loaded." }) }
        onDispose { postListener?.remove(); replyListener?.remove() }
    }
    Column(Modifier.fillMaxSize().background(discussionBrush())) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("‹ Back") }; Text("Discussion", fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { DiscussionCard { Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){DiscussionSymbol(DiscussionSymbolKind.Bubble,IlluminedThemeTokens.Blue,Modifier.size(20.dp));Text(prompt.title, color = IlluminedThemeTokens.Blue, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)}; Text(prompt.lessonTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Gold); Text(prompt.prompt, lineHeight = 23.sp); if (prompt.requiredForAssignment) Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){DiscussionSymbol(DiscussionSymbolKind.CheckSeal,IlluminedThemeTokens.SecondaryText,Modifier.size(16.dp));Text("Post a response to complete the discussion assignment.", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText)} } }
            if (posts.isEmpty()) item { DiscussionCard { Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){DiscussionSymbol(DiscussionSymbolKind.Bubble,IlluminedThemeTokens.Gold,Modifier.size(18.dp));Text("No Responses Yet", fontWeight = FontWeight.SemiBold)}; Text("Be the first to respond to this prompt.", color = IlluminedThemeTokens.SecondaryText) } }
            items(posts, key = { it.id }) { post -> PostCard(post, replies.filter { it.postId == post.id }, post.authorId == userId, prompt, profile, repository, userId) { error = it } }
        }
        if (myPost == null) Column(Modifier.fillMaxWidth().background(Color.White.copy(.92f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth(), placeholder = { Text("Write your response") }, minLines = 2, maxLines = 6, shape = RoundedCornerShape(16.dp))
            Button(onClick = {
                val message = draft
                working = true
                repository.post(prompt, profile!!, posts, message, {
                    draft = ""
                    if (linkedAssignments.isNotEmpty()) {
                        var remaining = linkedAssignments.size
                        var completionFailed = false
                        linkedAssignments.forEach { assignment ->
                            onCompleteAssignment(assignment, {
                                remaining -= 1
                                if (remaining == 0) {
                                    working = false
                                    if (completionFailed) error = "Your response was posted, but some assignment progress could not be updated."
                                }
                            }, {
                                completionFailed = true
                                remaining -= 1
                                if (remaining == 0) {
                                    working = false
                                    error = "Your response was posted, but some assignment progress could not be updated."
                                }
                            })
                        }
                    } else {
                        working = false
                    }
                }, { working = false; error = "Response could not be posted." })
            }, enabled = draft.isNotBlank() && classId.isNotBlank() && !working, modifier = Modifier.fillMaxWidth().height(50.dp)) { if (working) Text("Posting…") else Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){DiscussionSymbol(DiscussionSymbolKind.PaperPlane,Color.White,Modifier.size(18.dp));Text("Post Response",fontWeight=FontWeight.SemiBold)} }
        } else Column(Modifier.fillMaxWidth().background(Color.White.copy(.92f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){DiscussionSymbol(DiscussionSymbolKind.CheckSeal,IlluminedThemeTokens.Blue,Modifier.size(18.dp));Text("Your response has been posted.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)}
            Text("You may edit or delete your original response above. To continue the conversation, reply to classmates and instructors.", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text("Discussion Error") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } }) }
}

@Composable
private fun PostCard(post: DiscussionPost, replies: List<DiscussionReply>, mine: Boolean, prompt: DiscussionPrompt, profile: UserProfile?, repository: DiscussionRepository, userId: String, error: (String) -> Unit) {
    var replyOpen by rememberSaveable(post.id) { mutableStateOf(false) }; var reply by rememberSaveable(post.id) { mutableStateOf("") }; var editing by rememberSaveable(post.id) { mutableStateOf(false) }; var edit by rememberSaveable(post.id, post.message) { mutableStateOf(post.message) }; var working by remember { mutableStateOf(false) }; var deleting by remember { mutableStateOf(false) }
    DiscussionCard {
        Row(Modifier.fillMaxWidth()) { Text(post.authorName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (mine) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Ink); Spacer(Modifier.weight(1f)); Text(post.createdAt?.toDate()?.let { DateFormat.getDateInstance().format(it) }.orEmpty(), fontSize = 11.sp, color = IlluminedThemeTokens.SecondaryText) }
        if (editing) { OutlinedTextField(edit, { edit = it }, Modifier.fillMaxWidth(), placeholder = { Text("Edit your response") }, minLines = 3, maxLines = 8, shape = RoundedCornerShape(12.dp)); Row { TextButton(onClick = { editing = false; edit = post.message }, enabled = !working) { Text("Cancel") }; Spacer(Modifier.weight(1f)); TextButton(onClick = { working = true; repository.updatePost(post, profile!!, edit, { working = false; editing = false }, { working = false; error("Response could not be updated.") }) }, enabled = profile != null && DiscussionInteractionPolicy.canSubmit(edit, working)) { if(working) Text("Saving…") else Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){DiscussionSymbol(DiscussionSymbolKind.Check,IlluminedThemeTokens.Blue,Modifier.size(14.dp));Text("Save")} } } }
        else Text(post.message, fontSize = 16.sp, lineHeight = 23.sp)
        Row { TextButton(onClick = { replyOpen = !replyOpen }, enabled = !deleting) { DiscussionAction(DiscussionSymbolKind.Reply,if(replyOpen) "Cancel Reply" else "Reply",IlluminedThemeTokens.Blue) }; if (mine && !editing) { TextButton(onClick = { editing = true; replyOpen = false }, enabled = !deleting) { DiscussionAction(DiscussionSymbolKind.Pencil,"Edit",IlluminedThemeTokens.Blue) }; TextButton(onClick = { deleting = true; repository.deletePost(post, prompt.id, { deleting = false }, { deleting = false; error("Response could not be deleted.") }) }, enabled = !deleting) { if(deleting) Text("Deleting…") else DiscussionAction(DiscussionSymbolKind.Trash,"Delete",Color.Red) } } }
        if (replies.isNotEmpty()) Row(Modifier.padding(start = 14.dp).height(IntrinsicSize.Min)) { Box(Modifier.width(2.dp).fillMaxHeight().background(IlluminedThemeTokens.Gold.copy(.24f))); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { replies.forEach { response -> Surface(color = IlluminedThemeTokens.Blue.copy(.06f), shape = RoundedCornerShape(12.dp)) { Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row { Text(response.authorName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (response.authorId == userId) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Ink); Spacer(Modifier.weight(1f)); Text(response.createdAt?.toDate()?.let { DateFormat.getDateInstance().format(it) }.orEmpty(), fontSize = 10.sp, color = IlluminedThemeTokens.SecondaryText) }; Text(response.message, fontSize = 14.sp, lineHeight = 19.sp) } } } } }
        if (replyOpen) { OutlinedTextField(reply, { reply = it }, Modifier.fillMaxWidth(), placeholder = { Text("Write a reply") }, minLines = 1, maxLines = 4, shape = RoundedCornerShape(12.dp)); OutlinedButton(onClick = { working = true; repository.reply(post, prompt, profile!!, reply, { reply = ""; replyOpen = false; working = false }, { working = false; error("Reply could not be posted.") }) }, enabled = profile != null && DiscussionInteractionPolicy.canSubmit(reply, working), modifier = Modifier.fillMaxWidth()) { Text(if (working) "Posting…" else "Post Reply") } }
    }
}

@Composable private fun DiscussionAction(kind:DiscussionSymbolKind,label:String,color:Color){Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){DiscussionSymbol(kind,color,Modifier.size(15.dp));Text(label,color=color)}}

@Composable private fun DiscussionCard(content: @Composable ColumnScope.() -> Unit) { Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) } }
private fun discussionBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
