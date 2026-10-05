package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.util.UUID

@Composable
internal fun WalkthroughManagement(editor: Boolean, onBack: () -> Unit) {
    val tour = LocalInstructorWalkthrough.current
    val db = remember { FirebaseFirestore.getInstance() }
    var text by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }
    var saved by remember { mutableStateOf<Map<String, Map<String, String>>>(emptyMap()) }
    var revision by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(!editor) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(walkthroughSteps.first()) }
    var group by remember { mutableStateOf("home") }
    var menu by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf(false) }
    val editableSteps = walkthroughSteps.filter { it.target != "lesson-video" }
    androidx.activity.compose.BackHandler {
        if(!working) { if(text != saved) confirm = "leave" else onBack() }
    }
    val defaults = mapOf("title" to selected.title, "body" to selected.body, "titleEs" to selected.titleEs, "bodyEs" to selected.bodyEs)
    val fields = text[selected.target] ?: defaults
    fun sectionLabel(section: String) = when(section) {
        "home" -> appT("Home", "Inicio")
        "categories" -> appT("Lesson Categories", "Categorías de lecciones")
        "category" -> appT("Inside a Category", "Dentro de una categoría")
        "detail" -> appT("Inside a Lesson", "Dentro de una lección")
        "discussion" -> appT("Discussion", "Debate")
        "formation" -> appT("Formation", "Formación")
        "more" -> appT("More", "Más")
        "instructor-tools" -> appT("Instructor Tools", "Herramientas del instructor")
        "classroom-codes" -> appT("Classroom Codes", "Códigos del aula")
        else -> section
    }
    fun save(publish: Boolean = false, preview: Boolean = false) {
        working = true; message = ""
        val snapshot = text
        val expected = revision
        val nextRevision = UUID.randomUUID().toString()
        val payload = mapOf("steps" to snapshot, "revision" to nextRevision, "updatedBy" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty(), "updatedAt" to FieldValue.serverTimestamp())
        db.runTransaction { transaction ->
            val draft = db.document("walkthroughContent/draft")
            check(transaction.get(draft).getString("revision") == expected) { "The draft changed on another device. Reopen the editor before saving." }
            transaction.set(draft, payload)
            if (publish) transaction.set(db.document("walkthroughContent/published"), payload)
        }.addOnSuccessListener {
            revision = nextRevision; saved = snapshot; working = false
            message = appT("Saved successfully.", "Guardado correctamente.")
            if (publish) tour?.published = snapshot
            if (preview) tour?.preview(snapshot, selected.target)
        }.addOnFailureListener { working = false; message = localizedUserMessage(it.localizedMessage.orEmpty()) }
    }
    LaunchedEffect(editor) {
        if (editor) {
            working = true
            db.document("walkthroughContent/draft").get(com.google.firebase.firestore.Source.SERVER).addOnSuccessListener { draft ->
                revision = draft.getString("revision")
                if (draft.exists()) { text = decodeWalkthroughText(draft.get("steps")); saved = text; loaded = true; working = false }
                else db.document("walkthroughContent/published").get(com.google.firebase.firestore.Source.SERVER).addOnSuccessListener { published ->
                    text = decodeWalkthroughText(published.get("steps")); saved = text; loaded = true; working = false
                }.addOnFailureListener { working = false; message = localizedUserMessage(it.localizedMessage.orEmpty()) }
            }.addOnFailureListener { working = false; message = localizedUserMessage(it.localizedMessage.orEmpty()) }
        }
    }
    Column(Modifier.fillMaxSize().background(IlluminedThemeTokens.Cream).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { if (text != saved) confirm = "leave" else onBack() }, enabled = !working) { Text(appT("Back", "Atrás")) }
        WalkthroughAdminCard {
            Text(if(editor) appT("Edit Walkthrough", "Editar recorrido") else appT("Walkthrough Management", "Administrar recorrido"), color = IlluminedThemeTokens.Blue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (!editor) {
                Text(appT("Replay the instructor tour on this device. This does not send an invitation to anyone else.", "Repite el recorrido en este dispositivo sin enviar invitaciones a nadie más."))
                Button(onClick = { tour?.start() }, modifier = Modifier.fillMaxWidth()) { Text(appT("Replay on This Device", "Repetir en este dispositivo")) }
            } else {
                Text(appT("Edit the English and Spanish wording. Navigation and step order stay unchanged. Publishing does not re-offer the tour.", "Edita el texto en inglés y español. La navegación y el orden no cambian. Publicar no vuelve a ofrecer el recorrido."))
                Box {
                    OutlinedButton(onClick = { groups = true }, modifier = Modifier.fillMaxWidth()) { Text(sectionLabel(group)) }
                    DropdownMenu(groups, { groups = false }) { editableSteps.map { it.screen }.distinct().forEach { section -> DropdownMenuItem(text = { Text(sectionLabel(section)) }, onClick = { group = section; selected = editableSteps.first { it.screen == section }; groups = false }) } }
                }
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) { Text(appT(selected.title, selected.titleEs)) }
                    DropdownMenu(menu, { menu = false }, modifier = Modifier.heightIn(max = 360.dp)) { editableSteps.filter { it.screen == group }.forEach { step -> DropdownMenuItem(text = { Text(appT(step.title, step.titleEs)) }, onClick = { selected = step; menu = false }) } }
                }
                listOf("title" to appT("English title", "Título en inglés"), "body" to appT("English description", "Descripción en inglés"), "titleEs" to appT("Spanish title", "Título en español"), "bodyEs" to appT("Spanish description", "Descripción en español")).forEach { (key, label) ->
                    OutlinedTextField(value = fields[key].orEmpty(), onValueChange = { if (it.length <= if(key.startsWith("title")) 100 else 700) text = text + (selected.target to (fields + (key to it))) }, label = { Text(label) }, enabled = loaded && !working, modifier = Modifier.fillMaxWidth(), minLines = if(key.startsWith("title")) 1 else 4)
                }
                if (text != saved) Text(appT("Unsaved changes", "Cambios sin guardar"), color = IlluminedThemeTokens.Blue)
                val valid = text == decodeWalkthroughText(text)
                Button(onClick = { save() }, enabled = loaded && !working && valid, modifier = Modifier.fillMaxWidth()) { Text(appT("Save Draft", "Guardar borrador")) }
                OutlinedButton(onClick = { save(preview = true) }, enabled = loaded && !working && valid, modifier = Modifier.fillMaxWidth()) { Text(appT("Save & Preview", "Guardar y previsualizar")) }
                Button(onClick = { confirm = "publish" }, enabled = loaded && !working && valid, modifier = Modifier.fillMaxWidth()) { Text(appT("Publish Walkthrough Text", "Publicar texto del recorrido")) }
                TextButton(onClick = { text = text - selected.target }, enabled = loaded && !working) { Text(appT("Restore this step’s defaults", "Restaurar texto original de este paso")) }
            }
        }
        if (!editor) WalkthroughAdminCard {
            Text(appT("Re-offer to All Instructors", "Volver a ofrecer a todos los instructores"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IlluminedThemeTokens.Blue)
            Text(appT("Instructors with a compatible app receive an invitation on their next sign-in or launch, even if they previously completed or dismissed it. They can skip for now. A tour already in progress is not interrupted.", "Los instructores con una aplicación compatible recibirán una invitación al iniciar sesión o abrirla, aunque ya hayan terminado o descartado el recorrido. Pueden posponerlo. No se interrumpe un recorrido en curso."))
            Button(onClick = { confirm = "offer" }, enabled = !working, modifier = Modifier.fillMaxWidth()) { Text(appT("Re-offer Walkthrough", "Volver a ofrecer recorrido")) }
        }
        if (working) CircularProgressIndicator()
        if (message.isNotBlank()) Text(message)
    }
    if (confirm.isNotEmpty()) AlertDialog(onDismissRequest = { confirm = "" }, title = { Text(appT("Confirm", "Confirmar")) }, text = { Text(when(confirm) { "leave" -> appT("Discard unsaved changes?", "¿Descartar los cambios sin guardar?"); "publish" -> appT("Publish this wording for instructors using compatible apps?", "¿Publicar este texto para instructores con aplicaciones compatibles?"); else -> appT("Offer the walkthrough again to all instructors?", "¿Volver a ofrecer el recorrido a todos los instructores?") }) }, confirmButton = { TextButton(onClick = {
        val action = confirm; confirm = ""
        when(action) {
            "leave" -> onBack()
            "publish" -> save(publish = true)
            else -> { working = true; db.document("walkthroughSettings/instructors").set(mapOf("revision" to UUID.randomUUID().toString(), "updatedBy" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty(), "updatedAt" to FieldValue.serverTimestamp())).addOnSuccessListener { working = false; message = appT("Walkthrough re-offered.", "Se volvió a ofrecer el recorrido.") }.addOnFailureListener { working = false; message = localizedUserMessage(it.localizedMessage.orEmpty()) } }
        }
    }) { Text(appT("Confirm", "Confirmar")) } }, dismissButton = { TextButton(onClick = { confirm = "" }) { Text(appT("Cancel", "Cancelar")) } })
}

@Composable private fun WalkthroughAdminCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 5.dp, border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(alpha = .25f))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}
