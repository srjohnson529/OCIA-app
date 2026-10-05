package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.functions.FirebaseFunctions
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.util.UUID

private fun adminT(en: String, es: String) = if (java.util.Locale.getDefault().language == "es") es else en
private fun Map<*, *>.text(key: String) = this[key] as? String ?: ""
private fun Map<*, *>.flag(key: String) = this[key] == true
private fun Map<*, *>.strings(key: String) = (this[key] as? List<*>)?.filterIsInstance<String>().orEmpty()

@Composable
fun AdminDirectoryExperience(profile: UserProfile, onBack: () -> Unit) {
    if (!profile.isAdmin) return
    val functions = remember { FirebaseFunctions.getInstance("us-central1") }
    val clipboard = LocalClipboardManager.current
    var kind by remember { mutableStateOf("classes") }
    var search by remember { mutableStateOf("") }
    var appliedSearch by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(emptyList<Map<*, *>>()) }
    var cursor by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("all") }
    var classMenu by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<Map<String, Any>?>(null) }
    var prompt by remember { mutableStateOf("") }
    var lastKey by remember { mutableStateOf("") }
    var requestId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    fun load(more: Boolean = false) {
        if (busy) return
        busy = true; status = ""
        if (!more) appliedSearch = search
        functions.getHttpsCallable("adminDirectory").call(mapOf("kind" to kind, "search" to appliedSearch, "cursor" to if (more) cursor else ""))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *> ?: emptyMap<String, Any>()
                val fetched = (data["items"] as? List<*>)?.filterIsInstance<Map<*, *>>().orEmpty()
                rows = (if (more) rows + fetched else fetched).distinctBy { it.text("id") }.sortedWith(compareBy<Map<*, *>> { it.text("name").lowercase() }.thenBy { it.text("id") }); cursor = data.text("cursor"); busy = false
                if (rows.isEmpty()) status = adminT("No matches on this page. Continue searching if more pages are available.", "No hay resultados en esta página. Continúa si hay más páginas.")
            }.addOnFailureListener { busy = false; status = it.localizedMessage.orEmpty() }
    }
    fun perform(data: Map<String, Any>) {
        if (busy) return
        busy = true; status = ""; link = ""
        functions.getHttpsCallable("adminClassSupport").call(data).addOnSuccessListener {
            busy = false; link = (it.data as? Map<*, *>)?.text("link").orEmpty()
            status = adminT("Completed.", "Completado.")
            if(data["action"] != "studentLink") load()
            lastKey = ""; requestId = UUID.randomUUID().toString()
        }.addOnFailureListener { busy = false; status = it.localizedMessage.orEmpty() }
    }
    fun prepare(action: String, classId: String, label: String, extras: Map<String, Any> = emptyMap()) {
        reason = ""
        val key = "$action:$classId:${extras["userId"]}:$reason"
        if (key != lastKey) { lastKey = key; requestId = UUID.randomUUID().toString() }
        pending = mapOf("action" to action,"classId" to classId,"reason" to reason,"requestId" to requestId) + extras
        prompt = label
    }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize().background(IlluminedThemeTokens.Cream), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text(adminT("Back", "Atrás")) } }
        item { AdminSupportCard {
            Text(adminT("Parish & Account Support", "Parroquias y soporte de cuentas"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
            Row { TextButton(enabled = !busy, onClick = { kind = "classes"; selectedClass = ""; link = ""; rows = emptyList(); search = ""; load() }) { Text(adminT("Classrooms", "Aulas")) }; TextButton(enabled = !busy, onClick = { kind = "accounts"; selectedClass = ""; link = ""; rows = emptyList(); search = ""; load() }) { Text(adminT("Accounts", "Cuentas")) } }
            Text(if (kind == "classes") adminT("Classroom directory", "Directorio de aulas") else adminT("Account directory", "Directorio de cuentas"), fontWeight = FontWeight.Bold)
            OutlinedTextField(search, { search = it }, enabled = !busy, label = { Text(adminT("Search name, email, or ID", "Buscar nombre, correo o ID")) }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { load() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(adminT("Search / Refresh", "Buscar / Actualizar")) }
            if(kind == "classes") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("all" to adminT("All", "Todas"), "active" to adminT("Active", "Activas"), "archived" to adminT("Archived", "Archivadas")).forEach { (value,label) ->
                        FilterChip(selected = classFilter == value, onClick = { classFilter = value; selectedClass = "" }, label = { Text(label) })
                    }
                }
                val available = rows.filter { classFilter == "all" || it.flag("isArchived") == (classFilter == "archived") }
                Box {
                    OutlinedButton(onClick = { classMenu = true }, enabled = !busy && available.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text(available.firstOrNull { it.text("id") == selectedClass }?.let { it.text("name") + " · " + it.text("id") } ?: adminT("Choose a classroom (A–Z)", "Elegir aula (A–Z)"))
                    }
                    DropdownMenu(classMenu, { classMenu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                        available.forEach { row -> DropdownMenuItem(text = { Column { Text(row.text("name"), fontWeight = FontWeight.Bold); Text(row.text("id"), fontSize = 12.sp) } }, onClick = { selectedClass = row.text("id"); classMenu = false; link = "" }) }
                    }
                }
                Text(adminT("Loaded classrooms: ", "Aulas cargadas: ") + rows.size + if(cursor.isNotBlank()) adminT(" · More available below", " · Hay más disponibles abajo") else "", fontSize = 13.sp)
            }
            Text(adminT("Changes require confirmation and are recorded. Restoring access preserves progress. The previous owner remains an instructor after a transfer.", "Los cambios requieren confirmación y se registran. Restaurar el acceso conserva el progreso. El titular anterior sigue como instructor tras una transferencia."), fontSize = 13.sp)
            if (busy) CircularProgressIndicator()
            if (status.isNotBlank()) Text(status)
            if (link.isNotBlank()) { Text(link); Button(onClick = { clipboard.setText(AnnotatedString(link)) }) { Text(adminT("Copy invitation link", "Copiar enlace de invitación")) } }
        } }
        items(if(kind == "classes") rows.filter { it.text("id") == selectedClass && (classFilter == "all" || it.flag("isArchived") == (classFilter == "archived")) } else rows, key = { it.text("id") }) { row -> AdminSupportCard {
            val classId = row.text("id")
            Text(row.text("name"), fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
            Text(classId, fontSize = 12.sp)
            if (kind == "classes") {
                val archived = row.flag("isArchived")
                Text(if (archived) adminT("Archived", "Archivada") else adminT("Active", "Activa"))
                Text(adminT("Students: ", "Estudiantes: ") + (row["students"] as? Number)?.toInt())
                Text(adminT("Instructors: ", "Instructores: ") + row.strings("instructorNames").joinToString())
                Text(if (row.flag("ownerMissing")) adminT("Needs an assigned owner", "Necesita un titular asignado") else adminT("Owner: ", "Titular: ") + row.text("ownerName"))
                TextButton(enabled = !busy, onClick = { prepare(if (archived) "restoreClass" else "archive", classId, adminT("Change classroom status: ", "Cambiar estado del aula: ") + row.text("name"), mapOf("expectedArchived" to archived)) }) { Text(if (archived) adminT("Restore classroom", "Restaurar aula") else adminT("Archive classroom", "Archivar aula")) }
                if (!archived) {
                    var expanded by remember { mutableStateOf(false) }
                    Box { TextButton(enabled = !busy, onClick = { expanded = true }) { Text(adminT("Transfer ownership", "Transferir titularidad")) }
                        DropdownMenu(expanded, { expanded = false }) {
                            (row["instructors"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.filter { it.text("id") != row.text("ownerId") }?.forEach { instructor ->
                                DropdownMenuItem(text = { Text(instructor.text("name")) }, onClick = { expanded = false; prepare("transfer",classId,adminT("Transfer ownership to ", "Transferir titularidad a ") + instructor.text("name"),mapOf("userId" to instructor.text("id"),"expectedOwner" to row.text("ownerId"))) })
                            }
                        }
                    }
                    TextButton(enabled = !busy, onClick = { perform(mapOf("action" to "studentLink","classId" to classId)) }) { Text(adminT("Get student invitation", "Obtener invitación de estudiante")) }
                    TextButton(enabled = !busy, onClick = { prepare("inviteInstructor",classId,adminT("Create a one-use invitation granting instructor access to ", "Crear una invitación de un solo uso con acceso de instructor a ") + row.text("name")) }) { Text(adminT("New instructor invitation", "Nueva invitación de instructor")) }
                }
            } else {
                Text(row.text("email"))
                Text(if (row.flag("isAdmin")) adminT("Administrator", "Administrador") else if (row.flag("isInstructor")) adminT("Instructor", "Instructor") else adminT("Student", "Estudiante"))
                Text(adminT("Classrooms: ", "Aulas: ") + row.strings("classIds").joinToString())
                listOf("inactiveClassIds" to adminT("Inactive: ", "Inactivas: "), "removedClassIds" to adminT("Removed: ", "Retiradas: "), "archivedClassIds" to adminT("Archived: ", "Archivadas: ")).forEach { (field,label) ->
                    if(row.strings(field).isNotEmpty()) Text(label + row.strings(field).joinToString(), fontSize = 13.sp)
                }
                if (!row.flag("isAdmin")) (row.strings("removedClassIds") + row.strings("inactiveClassIds") + row.strings("archivedClassIds")).distinct().forEach { restoreClass ->
                    TextButton(enabled = !busy, onClick = { prepare("restoreAccess",restoreClass,adminT("Restore access for ", "Restaurar acceso para ") + row.text("name"),mapOf("userId" to row.text("id"))) }) { Text(adminT("Restore access: ", "Restaurar acceso: ") + restoreClass) }
                }
            }
        } }
        if (cursor.isNotBlank()) item { Button(enabled = !busy, onClick = { load(true) }) { Text(adminT("Continue search / Load more", "Continuar búsqueda / Cargar más")) } }
    }
    if (pending != null) AlertDialog(onDismissRequest = { pending = null }, title = { Text(prompt) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(reason, { if(it.length <= 500) reason = it }, label = { Text(adminT("Reason for this change", "Motivo del cambio")) }, modifier = Modifier.fillMaxWidth()); Text(adminT("No accounts or progress will be deleted. Invitation links grant access to their recipient.", "No se eliminarán cuentas ni progreso. Los enlaces de invitación otorgan acceso a su destinatario.")) } }, confirmButton = { TextButton(enabled = reason.isNotBlank() && !busy, onClick = { val data = pending?.plus("reason" to reason.trim()); pending = null; if (data != null) perform(data) }) { Text(adminT("Confirm change", "Confirmar cambio")) } }, dismissButton = { TextButton(onClick = { pending = null }) { Text(adminT("Cancel", "Cancelar")) } })
}

@Composable private fun AdminSupportCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}
