package com.illumined.app.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.illumined.app.ui.theme.IlluminedThemeTokens
import org.json.JSONObject

internal fun classroomT(en: String, es: String) = if (java.util.Locale.getDefault().language == "es") es else en
internal data class ClassroomChoice(val classId: String, val parishName: String, val city: String, val className: String, val image: String? = null) {
    companion object {
        fun parse(data: Map<*, *>): ClassroomChoice? {
            return ClassroomChoice(data["classId"] as? String ?: return null, data["parishName"] as? String ?: return null,
                data["city"] as? String ?: return null, data["className"] as? String ?: return null, data["image"] as? String)
        }
    }
}
internal class ClassroomChoiceStore(context: Context) {
    private val prefs = context.getSharedPreferences("classroom-discovery", Context.MODE_PRIVATE)
    fun load(): ClassroomChoice? = runCatching {
        val data = JSONObject(prefs.getString("choice", null) ?: return null)
        ClassroomChoice(data.getString("classId"), data.getString("parishName"), data.getString("city"), data.getString("className"))
    }.getOrNull()
    fun save(room: ClassroomChoice) { prefs.edit().remove("start-classroom").putString("choice", JSONObject(mapOf("classId" to room.classId, "parishName" to room.parishName, "city" to room.city, "className" to room.className)).toString()).apply() }
    fun clear() { prefs.edit().remove("choice").remove("start-classroom").apply() }
}
private fun callClassroom(name: String, data: Map<String, Any>, success: (Map<*, *>) -> Unit, failure: (String) -> Unit) {
    FirebaseFunctions.getInstance("us-central1").getHttpsCallable(name).call(data)
        .addOnSuccessListener { success(it.data as? Map<*, *> ?: emptyMap<Any, Any>()) }
        .addOnFailureListener { failure(it.localizedMessage ?: classroomT("Please try again.", "Inténtalo de nuevo.")) }
}

@Composable
internal fun ClassroomSearch(onSelect: (ClassroomChoice) -> Unit) {
    var parish by rememberSaveable { mutableStateOf("") }; var city by rememberSaveable { mutableStateOf("") }
    var rooms by remember { mutableStateOf(emptyList<ClassroomChoice>()) }; var selected by remember { mutableStateOf<ClassroomChoice?>(null) }
    var busy by remember { mutableStateOf(false) }; var searched by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(classroomT("Find My Classroom", "Encontrar mi aula"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        Text(classroomT("Enter your parish name and city. Your instructor will approve your request to join.", "Introduce tu parroquia y ciudad. Tu instructor aprobará la solicitud de ingreso."), color = IlluminedThemeTokens.SecondaryText)
        OutlinedTextField(parish, { parish = it }, Modifier.fillMaxWidth(), label = { Text(classroomT("Parish name", "Nombre de la parroquia")) }, singleLine = true)
        OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), label = { Text(classroomT("City", "Ciudad")) }, singleLine = true)
        Button(onClick = {
            busy = true; error = null; rooms = emptyList(); selected = null; searched = false
            callClassroom("findClassrooms", mapOf("parishName" to parish, "city" to city), {
                rooms = (it["classrooms"] as? List<*>)?.mapNotNull { value -> (value as? Map<*, *>)?.let(ClassroomChoice::parse) }.orEmpty()
                searched = true; busy = false
            }, { error = it; busy = false })
        }, enabled = !busy && parish.trim().length >= 2 && city.trim().length >= 2, modifier = Modifier.fillMaxWidth()) { Text(classroomT("Find Classroom", "Buscar aula")) }
        if (busy) CircularProgressIndicator()
        if (searched && rooms.isEmpty()) Text(classroomT("No matching classrooms. Check the spelling, or ask your instructor to enable discovery or share a QR invitation.", "No se encontraron aulas. Revisa la ortografía o pide a tu instructor que habilite la búsqueda o comparta una invitación QR."))
        rooms.forEach { room -> OutlinedButton(onClick = { selected = room }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth()) { ProfilePhoto(room.image); Text(room.parishName, fontWeight = FontWeight.SemiBold); Text("${room.city} · ${room.className}") }
        } }
        selected?.let { room ->
            Text("${room.parishName} — ${room.city}\n${room.className}", fontWeight = FontWeight.SemiBold)
            Button(onClick = { onSelect(room) }, modifier = Modifier.fillMaxWidth()) { Text(classroomT("Continue to Account Setup", "Continuar a la cuenta")) }
        }
        error?.let { Text(it, color = Color.Red) }
    }
}

@Composable
internal fun ClassroomQRButton(transparent: Boolean = false, onInvite: (IlluminedInviteLink) -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    // Embedded capture returns decoded text only; it never opens a detected URL or saves an image.
    val camera = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val invite = IlluminedInviteLink.parse(result.contents)
            if (invite != null) { error = null; onInvite(invite) }
            else error = classroomT("Could not read an Illumined QR invitation. Frame the code closely and try again, or enter its invitation code.", "No se pudo leer la invitación QR. Acerca el código y vuelve a intentarlo, o introduce el código de invitación.")
        }
    }
    val launchCamera = {
        error = null
        try {
            camera.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(classroomT("Scan your classroom invitation", "Escanea la invitación de tu aula"))
                .setBeepEnabled(false).setBarcodeImageEnabled(false).setOrientationLocked(false))
        } catch (_: Exception) { error = classroomT("Camera unavailable. Enter your invitation code instead.", "Cámara no disponible. Introduce el código de invitación.") }
    }
    if (transparent) {
        TextButton(onClick = launchCamera, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD7EDFF))) {
            Text(classroomT("QR Code", "Código QR"), modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    } else OutlinedButton(onClick = launchCamera, modifier = Modifier.fillMaxWidth()) {
        Text(classroomT("Scan Classroom QR Code", "Escanear código QR del aula"))
    }
    error?.let { Text(it, color = if (transparent) Color.White else Color.Red) }
}

@Composable
internal fun ClassroomEnrollmentSetup(onApproved: () -> Unit) {
    val context = LocalContext.current; val store = remember { ClassroomChoiceStore(context) }
    var room by remember { mutableStateOf(store.load()) }; var name by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }; var requestClassId by remember { mutableStateOf("") }; var parish by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val approved by rememberUpdatedState(onApproved)
    DisposableEffect(uid) {
        val listener = uid?.let { FirebaseFirestore.getInstance().collection("classroomJoinRequests").document(it).addSnapshotListener { snapshot, failure ->
            loading = false
            if (failure != null) error = failure.localizedMessage
            else {
                status = snapshot?.getString("status").orEmpty(); requestClassId = snapshot?.getString("classId").orEmpty(); parish = snapshot?.getString("parishName").orEmpty()
                if (status == "approved" && (room == null || requestClassId == room?.classId)) approved()
            }
        } }
        onDispose { listener?.remove() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(classroomT("Your Classroom Request", "Tu solicitud de ingreso"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        if (loading) CircularProgressIndicator()
        else if (status == "pending") {
            Text(parish, fontWeight = FontWeight.SemiBold)
            Text(classroomT("Awaiting instructor approval. Your classroom will open when your request is approved.", "Esperando la aprobación del instructor. Tu aula se abrirá cuando se apruebe tu solicitud."))
            OutlinedButton(onClick = {
                busy = true; error = null
                callClassroom("reviewClassroomEnrollment", mapOf("classId" to requestClassId, "studentId" to (uid ?: ""), "action" to "cancel"), { busy = false }, { error = it; busy = false })
            }, enabled = !busy) { Text(classroomT("Cancel Request", "Cancelar solicitud")) }
        } else {
            if (status == "declined") Text(classroomT("Your request was declined. Contact the parish for help or choose another classroom.", "La solicitud fue rechazada. Contacta a la parroquia o elige otra aula."))
            room?.let { selected ->
                Text("${selected.parishName} · ${selected.city}\n${selected.className}")
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(classroomT("Your Name", "Tu nombre")) })
                Button(onClick = {
                    busy = true; error = null
                    offerSetupPhotos(context)
                    callClassroom("requestClassroomEnrollment", mapOf("classId" to selected.classId, "displayName" to name), { busy = false; if (it["status"] == "approved") approved() }, { error = it; busy = false })
                }, enabled = !busy && name.trim().length >= 2, modifier = Modifier.fillMaxWidth()) { Text(classroomT("Request to Join", "Solicitar ingreso")) }
            }
            ClassroomSearch { room = it; store.save(it) }
        }
        error?.let { Text(it, color = Color.Red) }
    }
}

@Composable
internal fun ClassroomListingEditor(classId: String) {
    var parish by remember(classId) { mutableStateOf("") }; var city by remember(classId) { mutableStateOf("") }; var name by remember(classId) { mutableStateOf("") }
    var enabled by remember(classId) { mutableStateOf(false) }; var loaded by remember(classId) { mutableStateOf(false) }; var busy by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(classId) { callClassroom("manageClassroomListing", mapOf("classId" to classId), {
        parish = it["parishName"] as? String ?: ""; city = it["city"] as? String ?: ""; name = it["className"] as? String ?: ""; enabled = it["enabled"] == true; loaded = true
    }, { message = it }) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(classroomT("Help Students Find Your Classroom", "Ayuda a encontrar tu aula"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        Text(classroomT("Search displays your parish, city, classroom name, and optional classroom image. Approve requests in Classroom Management → Join Requests. Shared invitation links and QR codes allow direct enrollment.", "La búsqueda muestra la parroquia, ciudad, nombre e imagen opcional del aula. Aprueba solicitudes en Detalles de estudiantes. Las invitaciones y códigos QR permiten el ingreso directo."))
        OutlinedTextField(parish, { parish = it }, Modifier.fillMaxWidth(), label = { Text(classroomT("Parish name", "Parroquia")) })
        OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), label = { Text(classroomT("City", "Ciudad")) })
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(classroomT("Classroom name", "Nombre del aula")) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(classroomT("Show in classroom search", "Mostrar en la búsqueda"), modifier = Modifier.weight(1f)); Switch(enabled, { enabled = it }) }
        Button(onClick = {
            busy = true; message = null
            callClassroom("manageClassroomListing", mapOf("classId" to classId, "action" to "save", "parishName" to parish, "city" to city, "className" to name, "enabled" to enabled), { busy = false; message = classroomT("Listing saved.", "Datos guardados.") }, { message = it; busy = false })
        }, enabled = loaded && !busy && listOf(parish, city, name).all { it.trim().length >= 2 }, modifier = Modifier.fillMaxWidth()) { Text(classroomT("Save Classroom Listing", "Guardar datos del aula")) }
        message?.let { Text(it) }
    }
}

@Composable
internal fun ClassroomApprovalQueue(classId: String) {
    var requests by remember(classId) { mutableStateOf(emptyList<Map<String, String>>()) }; var error by remember { mutableStateOf<String?>(null) }
    var loading by remember(classId) { mutableStateOf(true) }; var busy by remember { mutableStateOf(false) }; var selected by remember { mutableStateOf<Map<String, String>?>(null) }; var action by remember { mutableStateOf("approve") }
    DisposableEffect(classId) {
        val listener = FirebaseFirestore.getInstance().collection("classroomJoinRequests").whereEqualTo("classId", classId).addSnapshotListener { snapshot, failure ->
            loading = false
            if (failure != null) error = failure.localizedMessage
            else requests = snapshot?.documents.orEmpty().filter { it.getString("status") == "pending" }.map { mapOf("id" to it.id, "name" to it.getString("displayName").orEmpty(), "email" to it.getString("email").orEmpty()) }.sortedBy { it["name"]?.lowercase() }
        }; onDispose { listener.remove() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(classroomT("Requests to Join", "Solicitudes de ingreso"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        if (loading) CircularProgressIndicator() else if (requests.isEmpty()) Text(classroomT("No pending requests.", "No hay solicitudes pendientes."))
        requests.forEach { request ->
            Text(request["name"].orEmpty(), fontWeight = FontWeight.SemiBold); Text(request["email"].orEmpty())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { action = "approve"; selected = request }, enabled = !busy) { Text(classroomT("Approve", "Aprobar")) }
                OutlinedButton(onClick = { action = "decline"; selected = request }, enabled = !busy) { Text(classroomT("Decline", "Rechazar")) }
            }; HorizontalDivider()
        }
        error?.let { Text(it, color = Color.Red) }
    }
    selected?.let { request -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(if (action == "approve") classroomT("Approve student?", "¿Aprobar estudiante?") else classroomT("Decline request?", "¿Rechazar solicitud?")) }, text = { Text("${request["name"]}\n${request["email"]}") }, confirmButton = {
        TextButton(onClick = {
            selected = null; busy = true; error = null
            callClassroom("reviewClassroomEnrollment", mapOf("classId" to classId, "studentId" to request["id"].orEmpty(), "action" to action), { busy = false }, { error = it; busy = false })
        }) { Text(classroomT("Confirm", "Confirmar")) }
    }, dismissButton = { TextButton(onClick = { selected = null }) { Text(classroomT("Cancel", "Cancelar")) } }) }
}
