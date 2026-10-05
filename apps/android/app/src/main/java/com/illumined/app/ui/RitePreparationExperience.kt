package com.illumined.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun riteT(en: String, es: String) = if (Locale.getDefault().language == "es") es else en
private val riteFields = listOf(
    Triple("title", "Title", "Título"),
    Triple("meaning", "What it is and why it matters", "Qué es y por qué es importante"),
    Triple("context", "Its place in OCIA", "Su lugar en OCIA"),
    Triple("studentActions", "What you will do", "Qué harás"),
    Triple("ministerActions", "What the celebrant may do or ask", "Qué puede hacer o preguntar el celebrante"),
    Triple("preparation", "How to prepare / parish details", "Cómo prepararte / detalles parroquiales"),
)
internal object RitePreparationDate {
    fun expiry(date: String, zone: String): Date {
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date)) { "Enter a valid rite date (YYYY-MM-DD)." }
        return Date.from(LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneId.of(zone)).toInstant())
    }
}
private data class RiteEntry(
    val id: String, val text: Map<String, String>, val riteDate: String, val timeZone: String,
    val expiresAt: Date, val published: Boolean, val revision: String,
) {
    val title get() = text["title"].orEmpty()
    companion object {
        fun from(doc: DocumentSnapshot): RiteEntry? {
            val expiry = doc.getTimestamp("expiresAt") ?: return null
            val revision = doc.getString("revision") ?: return null
            return RiteEntry(doc.id, riteFields.associate { it.first to doc.getString(it.first).orEmpty() } +
                riteFields.drop(1).mapNotNull { field -> doc.getString(field.first + "Heading")?.let { (field.first + "Heading") to it } }.toMap(),
                doc.getString("riteDate").orEmpty(), doc.getString("timeZone") ?: "UTC",
                expiry.toDate(), doc.getBoolean("published") == true, revision)
        }
    }
}
private class RiteStore {
    private val db = FirebaseFirestore.getInstance()
    var items by mutableStateOf(emptyList<RiteEntry>())
    var receipts by mutableStateOf(emptyMap<String, String>())
    var loaded by mutableStateOf(emptySet<String>())
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    private var listener: ListenerRegistration? = null
    private val receiptListeners = mutableListOf<ListenerRegistration>()
    private var generation = 0
    private var snapshotGeneration = 0
    fun ref(classId: String) = db.collection("classrooms").document(classId).collection("ritePreparations")
    fun remove(item: RiteEntry, classId: String, uid: String, done: (String?) -> Unit) {
        ref(classId).document(item.id).update(mapOf(
            "deleted" to true, "published" to false, "revision" to UUID.randomUUID().toString(),
            "updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp()
        )).addOnSuccessListener { done(null) }.addOnFailureListener { done(it.localizedMessage ?: "Unable to delete guide.") }
    }
    fun stop() {
        generation++
        listener?.remove(); listener = null
        receiptListeners.forEach { it.remove() }; receiptListeners.clear()
        items = emptyList(); receipts = emptyMap(); loaded = emptySet()
    }
    fun listen(classId: String, uid: String, instructor: Boolean) {
        stop(); error = null
        if (classId.isBlank() || uid.isBlank()) return
        loading = true
        val token = generation
        val query: Query = if (instructor) ref(classId) else ref(classId).whereEqualTo("published", true)
        listener = query.addSnapshotListener { snap, failure ->
            if (generation != token) return@addSnapshotListener
            loading = false
            if (failure != null) { error = failure.localizedMessage; items = emptyList(); return@addSnapshotListener }
            error = null
            receiptListeners.forEach { it.remove() }; receiptListeners.clear()
            receipts = emptyMap(); loaded = emptySet()
            val snapToken = ++snapshotGeneration
            items = snap?.documents.orEmpty().filter { it.getBoolean("deleted") != true }.mapNotNull(RiteEntry::from).sortedWith(compareBy({it.riteDate}, {it.id}))
            if (!instructor) items.forEach { entry ->
                receiptListeners += ref(classId).document(entry.id).collection("acknowledgments").document(uid)
                    .addSnapshotListener receipt@{ receipt, problem ->
                        if (generation != token || snapshotGeneration != snapToken) return@receipt
                        if (problem != null) { error = problem.localizedMessage; return@receipt }
                        receipts = receipts + (entry.id to receipt?.getString("revision").orEmpty())
                        loaded = loaded + entry.id
                    }
            }
        }
    }
    fun acknowledge(item: RiteEntry, classId: String, uid: String, done: (String?) -> Unit) {
        val document = ref(classId).document(item.id)
        db.runTransaction { tx ->
            val fresh = tx.get(document)
            check(fresh.getBoolean("published") == true && fresh.getString("revision") == item.revision &&
                (fresh.getTimestamp("expiresAt")?.toDate()?.after(Date()) == true)) {
                riteT("This preparation changed or expired. Reopen it.", "Esta preparación cambió o venció. Vuelve a abrirla.")
            }
            tx.set(document.collection("acknowledgments").document(uid),
                mapOf("userId" to uid, "revision" to item.revision, "acknowledgedAt" to FieldValue.serverTimestamp()))
            true
        }.addOnSuccessListener { receipts = receipts + (item.id to item.revision); done(null) }
            .addOnFailureListener { done(it.localizedMessage ?: "Unable to save.") }
    }
    fun save(existing: RiteEntry?, text: Map<String, String>, date: String, zone: String, published: Boolean,
             classId: String, uid: String, done: (String?) -> Unit) {
        try {
            require(classId.isNotBlank() && uid.isNotBlank()) { "Select a class first." }
            val cleaned = text.mapValues { it.value.trim() }
            require(riteFields.all { !cleaned[it.first].isNullOrBlank() && cleaned[it.first]!!.length <= if(it.first == "title") 160 else 12000 }) {
                riteT("Complete each section (title: 160 characters; each section: 12,000).", "Completa todas las secciones (título: 160 caracteres; cada sección: 12.000).")
            }
            require(riteFields.drop(1).all { field ->
                cleaned[field.first + "Heading"]?.let { it.isNotBlank() && it.length <= 160 } ?: true
            }) { riteT("Section headings must contain 1–160 characters.", "Los títulos de sección deben tener entre 1 y 160 caracteres.") }
            val expiry = RitePreparationDate.expiry(date, zone)
            require(!published || expiry.after(Date())) { riteT("Choose today or a future rite date.", "Elige hoy o una fecha futura.") }
            val document = existing?.let { ref(classId).document(it.id) } ?: ref(classId).document()
            val revision = UUID.randomUUID().toString()
            db.runTransaction { tx ->
                val old = tx.get(document)
                check(existing == null || old.getString("revision") == existing.revision) {
                    riteT("Another instructor changed this preparation. Reopen the editor.", "Otro instructor cambió esta preparación. Vuelve a abrir el editor.")
                }
                val data = cleaned.mapValues { it.value as Any }.toMutableMap()
                data.putAll(mapOf("riteDate" to date, "timeZone" to zone, "expiresAt" to Timestamp(expiry),
                    "published" to published, "revision" to revision, "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to uid, "createdBy" to (old.getString("createdBy") ?: uid)))
                tx.set(document, data)
                true
            }.addOnSuccessListener { done(null) }.addOnFailureListener { done(it.localizedMessage ?: "Unable to save.") }
        } catch (e: Exception) { done(e.localizedMessage ?: "Invalid preparation.") }
    }
    fun acknowledgmentLines(item: RiteEntry, classId: String, done: (List<String>?, String?) -> Unit) {
        ref(classId).document(item.id).collection("acknowledgments").get().addOnSuccessListener { snapshot ->
            val docs = snapshot.documents
            val header = riteT("Current acknowledgments: ", "Confirmaciones actuales: ") + docs.count { it.getString("revision") == item.revision }
            if (docs.isEmpty()) { done(listOf(header), null); return@addOnSuccessListener }
            val rows = arrayOfNulls<String>(docs.size)
            var pending = docs.size
            docs.forEachIndexed { index, doc ->
                db.collection("userProfiles").document(doc.id).get().addOnCompleteListener { result ->
                    val name = if(result.isSuccessful) result.result?.getString("displayName") ?: doc.id else doc.id
                    val status = if(doc.getString("revision") == item.revision) riteT("Acknowledged", "Confirmado") else riteT("Earlier version", "Versión anterior")
                    rows[index] = name + " · " + status + " · " + (doc.getTimestamp("acknowledgedAt")?.toDate()?.toString() ?: "")
                    pending--
                    if (pending == 0) done(listOf(header) + rows.filterNotNull(), null)
                }
            }
        }.addOnFailureListener { done(null, it.localizedMessage) }
    }
}
@Composable private fun RiteCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, contentColor = Color(0xff202020),
        shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable private fun RiteButton(text: String, enabled: Boolean = true, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Blue, contentColor = Color.White)) { Text(text) }
}
@Composable
fun RitePreparationDashboard(classId: String, userId: String, library: Boolean = false) {
    val store = remember(classId, userId) { RiteStore() }
    var selected by remember(classId, userId) { mutableStateOf<RiteEntry?>(null) }
    var now by remember { mutableStateOf(Date()) }
    DisposableEffect(store) {
        store.listen(classId, userId, false)
        onDispose { store.stop() }
    }
    LaunchedEffect(store) { while(true) { now = Date(); delay(30_000) } }
    val active = store.items.filter { it.published && (library || (it.expiresAt.after(now) && it.id in store.loaded && store.receipts[it.id] != it.revision)) }
    val tourPlaceholder = !library && LocalInstructorWalkthrough.current?.target == "guides"
    if(library || active.isNotEmpty() || store.error != null || tourPlaceholder) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(.94f), shadowElevation = 6.dp,
            border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
          Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (library) riteT("My Guides", "Mis guías") else riteT("Preparation Guide", "Guía de preparación"), color = IlluminedThemeTokens.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (library) riteT("Revisit your class guides, including those you have already read.", "Consulta las guías de tu clase, incluidas las que ya has leído.") else riteT("Prepare for your upcoming rite", "Prepárate para tu próximo rito"), color = IlluminedThemeTokens.SecondaryText, fontSize = 12.sp)
                }
                HomeSymbol(HomeSymbolKind.CalendarBadgeClock, IlluminedThemeTokens.Gold, Modifier.size(22.dp))
            }
            active.forEach { entry ->
                val dateText = runCatching {
                    LocalDate.parse(entry.riteDate).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
                }.getOrDefault(entry.riteDate)
                Surface(onClick = { selected = entry }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), color = IlluminedThemeTokens.Blue.copy(.07f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(entry.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                            Text(riteT("Rite date: ", "Fecha del rito: ") + dateText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.SecondaryText)
                            if (library) Text(when {
                                !entry.expiresAt.after(now) -> riteT("Past Event", "Evento pasado")
                                entry.id !in store.loaded -> riteT("Loading…", "Cargando…")
                                store.receipts[entry.id] == entry.revision -> riteT("Acknowledged", "Lectura confirmada")
                                else -> riteT("To Review", "Por revisar")
                            }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                        }
                        HomeSymbol(HomeSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
                    }
                }
            }
            if (library && active.isEmpty() && store.error == null) Text(if (store.loading) riteT("Loading…", "Cargando…") else riteT("No published guides for your class yet.", "Tu clase aún no tiene guías publicadas."))
            if (tourPlaceholder && active.isEmpty()) Text(riteT("Published preparation guides appear here. This empty example is only shown during the tour.", "Las guías publicadas aparecen aquí. Este ejemplo vacío solo se muestra durante el recorrido."), color = IlluminedThemeTokens.SecondaryText)
            store.error?.let { Text(it, color = Color.Red); TextButton(onClick = { store.listen(classId, userId, false) }) { Text(riteT("Retry", "Reintentar")) } }
          }
        }
        Spacer(Modifier.height(14.dp))
    }
    val current = selected?.let { chosen -> store.items.firstOrNull { it.id == chosen.id && it.published } }
    LaunchedEffect(current, store.loading) { if (!store.loading && current == null) selected = null }
    current?.let { item ->
        var busy by remember(item.id) { mutableStateOf(false) }
        var error by remember(item.id) { mutableStateOf<String?>(null) }
        RiteDialog(onClose = { if(!busy)selected = null }, reader = true) {
            val accent = Color(0xffc99c47)
            val dateText = runCatching {
                LocalDate.parse(item.riteDate).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
            }.getOrDefault(item.riteDate)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Text(riteT("PREPARATION GUIDE", "GUÍA DE PREPARACIÓN"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                Text(item.title, fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Box(Modifier.width(90.dp).height(3.dp).background(accent))
                Text(riteT("Rite date: ", "Fecha del rito: ") + dateText, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            }
            riteFields.drop(1).forEach { field ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(item.text[field.first + "Heading"] ?: riteT(field.second, field.third), fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
                    Text(item.text[field.first].orEmpty(), fontSize = 20.sp, lineHeight = 30.sp)
                }
            }
            Text(riteT("Acknowledgment confirms that you have read this preparation", "La confirmación indica que has leído esta preparación, no que asististe ni que recibiste un sacramento."), fontSize = 13.sp, lineHeight = 19.sp)
            error?.let { Text(it, color = Color.Red) }
            if (item.expiresAt.after(now) && item.id in store.loaded && store.receipts[item.id] != item.revision) Button(onClick = {
                busy = true
                store.acknowledge(item, classId, userId) { problem -> busy = false; error = problem; if(problem == null)selected = null }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                Text(riteT("I have read this preparation", "He leído esta preparación"), fontFamily = FontFamily.SansSerif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            } else Text(if (!item.expiresAt.after(now)) riteT("Past Event", "Evento pasado") else if (item.id in store.loaded) riteT("Acknowledged", "Lectura confirmada") else riteT("Loading…", "Cargando…"))
        }
    }
}
@Composable private fun RiteDialog(onClose: () -> Unit, reader: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(if (reader) 1f else .94f).fillMaxHeight(if (reader) .96f else .9f), shape = RoundedCornerShape(20.dp), color = Color.White, contentColor = if (reader) Color.Black else Color(0xff202020)) {
            ProvideTextStyle(if (reader) LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif, color = Color.Black) else LocalTextStyle.current) {
              Column(Modifier.padding(if (reader) 30.dp else 20.dp)) {
                TextButton(onClick = onClose) { Text(riteT("Close", "Cerrar"), fontFamily = if (reader) FontFamily.SansSerif else null, color = if (reader) Color.Black else IlluminedThemeTokens.Blue) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (reader) 22.dp else 14.dp), content = content)
              }
            }
        }
    }
}
@Composable
fun RitePreparationManager(profile: UserProfile, onBack: () -> Unit) {
    val classId = profile.selectedClassId
    val store = remember(classId, profile.userId) { RiteStore() }
    var editing by remember(classId) { mutableStateOf<RiteEntry?>(null) }
    var open by remember(classId) { mutableStateOf(false) }
    var libraryTitle by remember(classId) { mutableStateOf("") }
    var lines by remember(classId) { mutableStateOf<List<String>?>(null) }
    DisposableEffect(store) {
        if(profile.isInstructor)store.listen(classId, profile.userId, true)
        onDispose { store.stop() }
    }
    if(open) {
        RiteEditor(store, editing, libraryTitle, classId, profile.userId) { open = false }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text(riteT("‹ Back", "‹ Atrás")) }
        RiteCard {
            Text(riteT("Preparation Guide", "Guía de preparación"), color = IlluminedThemeTokens.Blue, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(riteT("Create guides to help students prepare for rites and sacraments. Guides appear on their dashboard until acknowledged or the rite date has passed.", "Crea guías para preparar a los estudiantes para los ritos y sacramentos. Aparecen en su inicio hasta confirmar su lectura o pasar la fecha del rito."), fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText)
            RiteButton(riteT("+ New Guide", "+ Nueva guía"), classId.isNotBlank()) { editing = null; libraryTitle = ""; open = true }

        }
        if(store.loading) CircularProgressIndicator()
        store.error?.let { Text(it, color = Color.Red); TextButton(onClick = { store.listen(classId, profile.userId, true) }) { Text(riteT("Retry", "Reintentar")) } }
        if(store.items.isEmpty() && !store.loading)Text(riteT("No preparations yet.", "Todavía no hay preparaciones."))
        store.items.reversed().forEach { item ->
            RiteCard {
                TextButton(onClick = { editing = item; libraryTitle = ""; open = true }) {
                    Text(item.title + " ›", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = IlluminedThemeTokens.Ink, modifier = Modifier.fillMaxWidth())
                }
                Text(item.text["meaning"].orEmpty(), maxLines = 3, fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText)
                Text(item.riteDate + " · " + if(!item.expiresAt.after(Date()))riteT("Past rite", "Rito pasado") else if(item.published)riteT("Published", "Publicado") else riteT("Draft", "Borrador"))
                TextButton(onClick = { editing = item; libraryTitle = ""; open = true }) { Text(riteT("Edit / unpublish", "Editar / retirar publicación")) }
                TextButton(onClick = { store.acknowledgmentLines(item, classId) { result, problem -> lines = result; store.error = problem } }) { Text(riteT("Acknowledgments", "Confirmaciones")) }
            }
        }
    }
    lines?.let { values -> RiteDialog(onClose = { lines = null }) { values.forEach { Text(it) } } }
}
@Composable private fun RiteEditor(store: RiteStore, existing: RiteEntry?, libraryTitle: String, classId: String, uid: String, close: () -> Unit) {
    var text by remember { mutableStateOf(
        riteFields.drop(1).associate { (it.first + "Heading") to riteT(it.second, it.third) } +
            (existing?.text ?: mapOf("title" to libraryTitle))
    ) }
    val templateSpanish = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "es"
    var templateId by remember { mutableStateOf("") }
    var choosingTemplate by remember { mutableStateOf(false) }
    var choosingSacrament by remember { mutableStateOf(false) }
    var choosingAdditional by remember { mutableStateOf(false) }
    var confirmingTemplate by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(existing?.riteDate.orEmpty()) }
    var zone by remember { mutableStateOf(existing?.timeZone ?: ZoneId.systemDefault().id) }
    var published by remember { mutableStateOf(existing?.published ?: false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    if(confirmingTemplate) AlertDialog(
        onDismissRequest = { confirmingTemplate = false },
        title = { Text(riteT("Replace guide text?", "¿Reemplazar el texto de la guía?")) },
        text = { Text(riteT("This replaces the title, section headings, and content. The date and visibility stay unchanged.", "Se reemplazarán el título, los títulos de sección y el contenido. La fecha y la visibilidad no cambiarán.")) },
        confirmButton = { TextButton(onClick = {
            (RiteGuideTemplates.all + SacramentGuideTemplates.all + AdditionalGuideTemplates.all).firstOrNull { it.id == templateId }?.let { text = it.content(templateSpanish).toMap() }
            confirmingTemplate = false
        }) { Text(riteT("Use Template", "Usar plantilla")) } },
        dismissButton = { TextButton(onClick = { confirmingTemplate = false }) { Text(riteT("Cancel", "Cancelar")) } }
    )
    androidx.activity.compose.BackHandler { if(!busy)close() }
    if (confirmingDelete && existing != null) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text(riteT("Delete this guide?", "¿Eliminar esta guía?")) },
        text = { Text(riteT("This removes the guide from your list and student dashboards. Acknowledgment records are retained. This cannot be undone in the app.", "La guía se quitará de tu lista y del inicio de los estudiantes. Se conservarán las confirmaciones de lectura. No se puede deshacer en la aplicación.")) },
        confirmButton = { TextButton(onClick = {
            confirmingDelete = false; busy = true; error = null
            store.remove(existing, classId, uid) { problem -> busy = false; error = problem; if (problem == null) close() }
        }) { Text(riteT("Delete", "Eliminar"), color = Color.Red) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(riteT("Cancel", "Cancelar")) } }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TextButton(onClick = close, enabled = !busy) { Text(riteT("‹ Cancel", "‹ Cancelar")) }
        RiteCard {
        Text(if(existing == null) riteT("New Guide", "Nueva guía") else riteT("Edit Guide", "Editar guía"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        Text(riteT("Rite Preparation", "Preparación para ritos"), fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
        OutlinedButton(onClick = { choosingTemplate = true }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(16.dp)) {
            Text(RiteGuideTemplates.all.firstOrNull { it.id == templateId }?.content(templateSpanish)?.get("title") ?: riteT("Choose a rite", "Elige un rito"),
                modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontSize = 17.sp, lineHeight = 23.sp, color = IlluminedThemeTokens.Blue)
            Spacer(Modifier.width(12.dp))
            HomeSymbol(HomeSymbolKind.ChevronRight, IlluminedThemeTokens.Blue, Modifier.size(14.dp))
        }
        if (choosingTemplate) RiteTemplateChooser(templateId, templateSpanish,
            onDismiss = { choosingTemplate = false },
            onSelect = { templateId = it; choosingTemplate = false })
        Text(riteT("Sacrament Preparation", "Preparación sacramental"), fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
        OutlinedButton(onClick = { choosingSacrament = true }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(16.dp)) {
            Text(SacramentGuideTemplates.all.firstOrNull { it.id == templateId }?.content(templateSpanish)?.get("title") ?: riteT("Choose a sacrament", "Elige un sacramento"),
                modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontSize = 17.sp, lineHeight = 23.sp, color = IlluminedThemeTokens.Blue)
            Spacer(Modifier.width(12.dp))
            HomeSymbol(HomeSymbolKind.ChevronRight, IlluminedThemeTokens.Blue, Modifier.size(14.dp))
        }
        if (choosingSacrament) RiteTemplateChooser(templateId, templateSpanish, sacrament = true,
            onDismiss = { choosingSacrament = false }, onSelect = { templateId = it; choosingSacrament = false })
        Text(riteT("Additional Guides", "Guías adicionales"), fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
        OutlinedButton(onClick = { choosingAdditional = true }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(16.dp)) {
            Text(AdditionalGuideTemplates.all.firstOrNull { it.id == templateId }?.content(templateSpanish)?.get("title") ?: riteT("Choose an additional guide", "Elige una guía adicional"),
                modifier = Modifier.weight(1f), textAlign = TextAlign.Start, fontSize = 17.sp, lineHeight = 23.sp, color = IlluminedThemeTokens.Blue)
            Spacer(Modifier.width(12.dp))
            HomeSymbol(HomeSymbolKind.ChevronRight, IlluminedThemeTokens.Blue, Modifier.size(14.dp))
        }
        if (choosingAdditional) RiteTemplateChooser(templateId, templateSpanish, additional = true,
            onDismiss = { choosingAdditional = false }, onSelect = { templateId = it; choosingAdditional = false })
        RiteButton(riteT("Use Template", "Usar plantilla"), !busy && templateId.isNotBlank()) { confirmingTemplate = true }
        Text(riteT("Editable preparation aids, not an official ritual text. Review wording with your parish before publishing.", "Ayudas de preparación editables, no el texto ritual oficial. Revisa la redacción con tu parroquia antes de publicar."), fontSize = 13.sp)
        riteFields.forEach { field ->
            if(field.first != "title") {
                OutlinedTextField(value = text[field.first + "Heading"] ?: riteT(field.second, field.third),
                    onValueChange = { text = text + ((field.first + "Heading") to it) },
                    label = { Text(riteT("Section heading", "Título de sección")) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(), enabled = !busy)
            }
            OutlinedTextField(value = text[field.first].orEmpty(), onValueChange = { text = text + (field.first to it) },
                label = { Text(riteT(field.second, field.third), fontWeight = FontWeight.SemiBold) }, minLines = if(field.first == "title")1 else 4,
                modifier = Modifier.fillMaxWidth(), enabled = !busy,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black))
        }
        ParishDatePicker(riteT("Rite date", "Fecha del rito"), date, !busy) { date = it }
        ParishTimeZonePicker(zone, !busy) { zone = it }
        Text(riteT("The card expires at midnight after the rite date in this time zone.", "La tarjeta vence a medianoche después de la fecha del rito en esta zona horaria."))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(riteT("Visible to Students", "Visible para estudiantes"), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Switch(checked = published, onCheckedChange = { published = it }, enabled = !busy)
        }
        }
        error?.let { Text(it, color = Color.Red) }
        RiteButton(riteT("Save Guide", "Guardar guía"), !busy) {
            busy = true; error = null
            store.save(existing, text, date.trim(), zone.trim(), published, classId, uid) { problem ->
                busy = false; error = problem; if(problem == null)close()
            }
        }
        if (existing != null) OutlinedButton(onClick = { confirmingDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) {
            Text(riteT("Delete Guide", "Eliminar guía"))
        }
    }
}
@Composable
private fun RiteTemplateChooser(selectedId: String, spanish: Boolean, sacrament: Boolean = false, additional: Boolean = false, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).widthIn(max = 600.dp).fillMaxHeight(.85f),
            shape = RoundedCornerShape(20.dp), color = Color.White, contentColor = IlluminedThemeTokens.Ink) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (additional) riteT("Additional Guides", "Guías adicionales") else if (sacrament) riteT("Choose a sacrament", "Elige un sacramento") else riteT("Choose a rite", "Elige un rito"), fontSize = 22.sp, lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text(riteT("Select a template, then tap Use Template to fill your guide.", "Selecciona una plantilla y luego pulsa Usar plantilla para completar tu guía."),
                    fontSize = 14.sp, lineHeight = 20.sp, color = IlluminedThemeTokens.SecondaryText)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(if (additional) AdditionalGuideTemplates.all else if (sacrament) SacramentGuideTemplates.all else RiteGuideTemplates.all, key = { it.id }) { template ->
                        val selected = template.id == selectedId
                        Surface(shape = RoundedCornerShape(12.dp), color = if (selected) IlluminedThemeTokens.Blue.copy(.08f) else Color(0xfff7f7f4),
                            border = BorderStroke(1.dp, if (selected) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Gold.copy(.24f))) {
                            Row(Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(template.id) })
                                .heightIn(min = 64.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = IlluminedThemeTokens.Blue))
                                Text(template.content(spanish)["title"].orEmpty(), modifier = Modifier.weight(1f),
                                    fontSize = 17.sp, lineHeight = 24.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = IlluminedThemeTokens.Ink)
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(riteT("Cancel", "Cancelar"), color = IlluminedThemeTokens.Blue)
                }
            }
        }
    }
}
