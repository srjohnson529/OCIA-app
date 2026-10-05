package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.functions.FirebaseFunctions
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.util.UUID
import java.util.Locale

private fun updateT(en: String, es: String) = if (Locale.getDefault().language == "es") es else en

@Composable
fun InstructorUpdatesExperience(profile: UserProfile?, adminTools: Boolean = false, onBack: () -> Unit) {
    val canManage = adminTools && profile?.isAdmin == true
    var manage by remember { mutableStateOf(false) }
    if (manage && canManage) {
        androidx.activity.compose.BackHandler { manage = false }
        UpdateManagementExperience { manage = false }
        return
    }
    val allowed = profile?.isInstructor == true || profile?.isAdmin == true
    var updates by remember { mutableStateOf(emptyList<QueryDocumentSnapshot>()) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var requestId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var sending by remember { mutableStateOf(false) }
    var showOnStartup by remember { mutableStateOf(true) }
    var sendPush by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var reading by remember(profile?.userId, adminTools) { mutableStateOf<QueryDocumentSnapshot?>(null) }
    var openedLatest by remember(profile?.userId, adminTools) { mutableStateOf(false) }
    DisposableEffect(allowed, profile?.userId, adminTools) {
        val listener = if (allowed) FirebaseFirestore.getInstance().collection("instructorUpdates").orderBy("createdAt", Query.Direction.DESCENDING).limit(100).addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                updates = snapshot.toList().filter { it.getBoolean("withdrawn") != true }
                if (!adminTools && !openedLatest && updates.isNotEmpty()) {
                    openedLatest = true
                    reading = updates.first()
                }
            }
            if (error != null) status = error.localizedMessage.orEmpty()
        } else null
        onDispose { listener?.remove() }
    }
    LazyColumn(Modifier.fillMaxSize().background(IlluminedThemeTokens.Cream), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text(updateT("Back", "Atrás")) } }
        if (allowed) {
            item { UpdateCard {
                Text(updateT("From Illumined", "De Illumined"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text(updateT("App news and information from Illumined. Push alerts follow your notification settings.", "Noticias e información de Illumined. Las alertas respetan tus ajustes de notificaciones."))
                if (canManage) {
                    Button(onClick = { manage = true }) { Text(updateT("Update Management · Drafts & Scheduling", "Gestión de novedades · Borradores y programación")) }
                    OutlinedTextField(title, { title = it }, label = { Text(updateT("Title", "Título")) }, modifier = Modifier.fillMaxWidth(), enabled = !sending)
                    OutlinedTextField(message, { message = it }, label = { Text(updateT("Message", "Mensaje")) }, modifier = Modifier.fillMaxWidth(), minLines = 5, enabled = !sending)
                    Text("${title.length}/120 · ${message.length}/2000", fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth()) { Text(updateT("Show at instructor startup", "Mostrar al iniciar para instructores"), Modifier.weight(1f)); Switch(showOnStartup, { showOnStartup = it }, enabled = !sending) }
                    Row(Modifier.fillMaxWidth()) { Text(updateT("Send push notification", "Enviar notificación push"), Modifier.weight(1f)); Switch(sendPush, { sendPush = it }, enabled = !sending) }
                    Button(onClick = { confirm = true }, enabled = !sending && title.isNotBlank() && message.isNotBlank() && title.length <= 120 && message.length <= 2000, modifier = Modifier.fillMaxWidth()) { Text(updateT("Send to all instructors", "Enviar a todos los instructores")) }
                }
                if (status.isNotEmpty()) Text(status)
            } }
            items(updates, key = { it.id }) { update -> UpdateCard {
                Text(update.getString("title").orEmpty(), fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Button(
                    onClick = { reading = update },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue, contentColor = Color.White)
                ) { Text(updateT("Read update", "Leer novedad"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            } }
            if (updates.isEmpty()) item { Text(updateT("No instructor updates yet.", "Aún no hay novedades.")) }
        }
    }
    if (allowed) reading?.let { update ->
        AlertDialog(
            onDismissRequest = { reading = null }, containerColor = IlluminedThemeTokens.Blue,
            titleContentColor = Color.White, textContentColor = Color.White,
            title = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(updateT("ILLUMINED UPDATE", "NOVEDADES DE ILLUMINED"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(update.getString("title").orEmpty(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.width(86.dp).height(3.dp).background(IlluminedThemeTokens.Gold))
            } },
            text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text(update.getString("message").orEmpty(), fontSize = 18.sp, lineHeight = 27.sp)
            } },
            confirmButton = { Button(onClick = { reading = null }, colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Gold, contentColor = Color.Black)) { Text(updateT("Close", "Cerrar")) } }
        )
    }
    if (confirm && canManage) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(updateT("Send to all registered instructors?", "¿Enviar a todos los instructores registrados?")) }, text = { Text("$title\n\n$message") }, dismissButton = { TextButton(onClick = { confirm = false }) { Text(updateT("Cancel", "Cancelar")) } }, confirmButton = { TextButton(onClick = {
        confirm = false; sending = true
        FirebaseFunctions.getInstance("us-central1").getHttpsCallable("publishInstructorUpdate").call(mapOf("title" to title, "message" to message, "requestId" to requestId, "showOnStartup" to showOnStartup, "sendPush" to sendPush))
            .addOnSuccessListener {
                val state = (it.data as? Map<*, *>)?.get("status")
                status = if (state == "inbox-only") updateT("Published without a push notification.", "Publicado sin notificación push.") else if (state == "sent") updateT("Published. Push requests accepted.", "Publicado. Solicitudes de envío aceptadas.") else updateT("Published to the inbox. Push delivery may be incomplete; do not resend.", "Publicado en la bandeja. El envío puede estar incompleto; no lo repitas.")
                title = ""; message = ""; requestId = UUID.randomUUID().toString(); sending = false
            }.addOnFailureListener { status = it.localizedMessage.orEmpty(); sending = false }
    }) { Text(updateT("Send update", "Enviar novedad")) } })
}

@Composable
private fun UpdateCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
