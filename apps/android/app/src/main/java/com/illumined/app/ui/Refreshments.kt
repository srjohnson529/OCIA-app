package com.illumined.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.illumined.app.ui.theme.IlluminedThemeTokens

private data class RefreshmentDay(val day: String, val topics: String, val uid: String, val name: String, val notes: String, val revision: Long)
private class RefreshmentStore(private val classId: String) {
    var days by mutableStateOf(listOf<RefreshmentDay>())
    var people by mutableStateOf(listOf<Pair<String,String>>())
    var teacher by mutableStateOf(false)
    var zone by mutableStateOf("")
    var busy by mutableStateOf(false)
    var failed by mutableStateOf(false)
    var loaded by mutableStateOf(false)
    var enabled by mutableStateOf(false)
    private var alive = true
    private var sequence = 0
    private val listeners = mutableListOf<ListenerRegistration>()
    private var slots: ListenerRegistration? = null
    fun start() {
        listeners += FirebaseFirestore.getInstance().document("classrooms/$classId/settings/refreshments").addSnapshotListener { snapshot,error ->
            if(alive) {
                slots?.remove();slots=null
                if(error!=null || snapshot?.getBoolean("enabled")==false) { sequence++;enabled=false;days=emptyList() }
                else {slots=FirebaseFirestore.getInstance().collection("refreshmentSignups").whereEqualTo("classId",classId).addSnapshotListener { _,_ -> if(alive)load() };load()}
            }
        }
        listOf("classSchedule").forEach { collection ->
            listeners += FirebaseFirestore.getInstance().collection(collection).whereEqualTo("classId",classId)
                .addSnapshotListener { _, error -> if(alive) { if(error != null) failed=true else load() } }
        }
        load()
    }
    fun stop() { alive=false;slots?.remove(); listeners.forEach { it.remove() } }
    @Suppress("UNCHECKED_CAST")
    fun load() {
        val request=++sequence
        FirebaseFunctions.getInstance().getHttpsCallable("classroomRefreshments").call(mapOf("classId" to classId))
            .addOnSuccessListener { response -> if(alive && request==sequence) {
                val data=response.data as? Map<String,Any?> ?: return@addOnSuccessListener
                days=(data["days"] as? List<Map<String,Any?>>).orEmpty().map { row ->
                    RefreshmentDay(row["day"] as? String ?: "", (row["topics"] as? List<String>).orEmpty().joinToString(" · "), row["volunteerId"] as? String ?: "", row["volunteerName"] as? String ?: "",row["notes"] as? String ?: "",(row["revision"] as? Number)?.toLong() ?: 0)
                }
                people=(data["students"] as? List<Map<String,String>>).orEmpty().map { (it["id"] ?: "") to (it["name"] ?: "") }
                enabled=data["enabled"]!=false
                teacher=data["isInstructor"]==true; zone=data["timeZone"] as? String ?: ""; loaded=true; failed=false
            }}.addOnFailureListener { if(alive && request==sequence) failed=true }
    }
    fun save(row: RefreshmentDay, uid: String, notes: String, cancel: Boolean=false, manualName: String="") {
        if(busy) return
        busy=true;failed=false
        FirebaseFunctions.getInstance().getHttpsCallable("classroomRefreshments").call(mapOf("classId" to classId,"action" to if(cancel) "cancel" else "save", "day" to row.day,"revision" to row.revision,"volunteerId" to uid,"notes" to notes,"manualName" to manualName))
            .addOnSuccessListener { if(alive) {busy=false;load()} }
            .addOnFailureListener { if(alive) {busy=false;failed=true} }
    }
}

@Composable
internal fun RefreshmentSignup(classId: String, userId: String, initiallyOpen: Boolean = false, onClose: () -> Unit = {}) {
    if(classId.isBlank()) return
    val es=LocalConfiguration.current.locales[0].language=="es"
    fun t(en:String, spanish:String)=if(es) spanish else en
    val store=remember(classId,userId){RefreshmentStore(classId)}
    var open by remember(classId,userId){mutableStateOf(initiallyOpen)}
    DisposableEffect(store){store.start();onDispose {store.stop()}}
    LaunchedEffect(store.enabled,store.loaded){if(!store.enabled && store.loaded){open=false;if(initiallyOpen)onClose()}}
    if(!store.enabled) return
    HorizontalDivider()
    TextButton(onClick={open=true;store.load()}) {
        Text(t("Refreshments","Refrigerios")+" · "+(store.days.firstOrNull()?.name?.takeIf {it.isNotBlank()} ?: t("View sign-up sheet","Ver inscripciones")),fontSize=13.sp,color=IlluminedThemeTokens.SecondaryText)
    }
    if(open) Dialog(onDismissRequest={open=false;onClose()}) {
        Surface(shape=MaterialTheme.shapes.large,color=IlluminedThemeTokens.Cream) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text(t("Refreshment Sign-Up","Inscripción para refrigerios"),fontSize=22.sp,color=IlluminedThemeTokens.Blue)
                Text(t("One volunteer per date. Change only your own entry; instructors can manage all entries.","Una persona por fecha. Edita solo tu inscripción; los instructores pueden gestionar todas."),fontSize=13.sp)
                Text(t("Reminder: 9 a.m. the day before class","Recordatorio: 9 a. m. del día anterior")+" · "+store.zone,fontSize=12.sp)
                if(store.failed) {Text(t("Unable to save or refresh. This spot may have changed. Retry.","No se pudo guardar o actualizar. El lugar pudo cambiar. Reintenta."),color=MaterialTheme.colorScheme.error);TextButton(onClick={store.load()}){Text(t("Refresh","Actualizar"))}}
                if(!store.loaded&&!store.failed) CircularProgressIndicator()
                if(store.loaded&&store.days.isEmpty()) Text(t("No upcoming classes. Ask your instructor to add the schedule.","No hay clases próximas. Pide al instructor que agregue el horario."))
                LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(store.days,key={it.day}) { row ->
                        val occupied=row.uid.isNotBlank()||row.name.isNotBlank()
                        var selected by remember(row.day,row.revision){mutableStateOf(if(occupied)row.uid else userId)}
                        var manualName by remember(row.day,row.revision){mutableStateOf(if(row.uid.isBlank())row.name else "")}
                        var expanded by remember {mutableStateOf(false)}
                        var cancel by remember {mutableStateOf(false)}
                        Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp)) {
                            Text(java.time.LocalDate.parse(row.day).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)),color=IlluminedThemeTokens.Blue)
                            Text(row.topics);Text(row.name.ifBlank {t("Available","Disponible")})
                            if(store.teacher||!occupied||row.uid==userId) {
                                if(store.teacher) Box {
                                    TextButton(onClick={expanded=true},enabled=!store.busy){Text(if(selected.isBlank())t("Enter a name manually","Escribir un nombre") else store.people.find {it.first==selected}?.second ?: t("Choose volunteer","Elegir persona"))}
                                    DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {store.people.forEach {person->DropdownMenuItem(text={Text(person.second)},onClick={selected=person.first;expanded=false})};DropdownMenuItem(text={Text(t("Enter a name manually","Escribir un nombre"))},onClick={selected="";expanded=false})}
                                }
                                if(store.teacher&&selected.isBlank()) {
                                    OutlinedTextField(value=manualName,onValueChange={if(it.length<=120)manualName=it},label={Text(t("Volunteer name","Nombre de la persona voluntaria"))},enabled=!store.busy,modifier=Modifier.fillMaxWidth())
                                    Text(t("No automatic reminder: this name is not linked to an app account.","Sin recordatorio automático: este nombre no está vinculado a una cuenta."),fontSize=12.sp)
                                }
                                Column(Modifier.fillMaxWidth().padding(top=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                    Button(onClick={store.save(row,selected,"",manualName=if(selected.isBlank())manualName else "")},enabled=!store.busy&&(selected.isNotBlank()||manualName.isNotBlank()),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=IlluminedThemeTokens.Blue)) {
                                        Text(if(occupied)t("Save changes","Guardar cambios") else if(store.teacher)t("Save volunteer","Guardar voluntario") else t("Sign me up","Inscribirme"))
                                    }
                                    if(occupied) OutlinedButton(onClick={cancel=true},enabled=!store.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.error)) {Text(t("Cancel sign-up","Cancelar inscripción"))}
                                }
                            }
                        }}
                        if(cancel) AlertDialog(onDismissRequest={cancel=false},title={Text(t("Are you sure","¿Estás seguro?"))},confirmButton={TextButton(onClick={cancel=false;store.save(row,"","",true)}){Text(t("Yes","Sí"))}},dismissButton={TextButton(onClick={cancel=false}){Text(t("Cancel","Cancelar"))}})
                    }
                }
                TextButton(onClick={open=false;onClose()}){Text(t("Close","Cerrar"))}
            }
        }
    }
}

@Composable
internal fun RefreshmentSetting(classId: String) {
    val es=LocalConfiguration.current.locales[0].language=="es"
    var enabled by remember(classId){mutableStateOf<Boolean?>(null)}
    var busy by remember(classId){mutableStateOf(false)}
    var failed by remember(classId){mutableStateOf(false)}
    val alive=remember(classId){booleanArrayOf(true)}
    DisposableEffect(classId){
        val listener=FirebaseFirestore.getInstance().document("classrooms/$classId/settings/refreshments").addSnapshotListener { snapshot,error ->
            if(alive[0]){failed=error!=null;enabled=if(error==null) snapshot?.getBoolean("enabled")!=false else null}
        }
        onDispose{alive[0]=false;listener.remove()}
    }
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(if(es) "Inscripción para refrigerios" else "Refreshment sign-up",modifier=Modifier.weight(1f))
            Switch(checked=enabled==true,enabled=enabled!=null&&!busy,onCheckedChange={next->
                busy=true;failed=false
                FirebaseFunctions.getInstance().getHttpsCallable("classroomRefreshments").call(mapOf("classId" to classId,"action" to "configure","enabled" to next))
                    .addOnSuccessListener{if(alive[0]){enabled=next;busy=false}}
                    .addOnFailureListener{if(alive[0]){failed=true;busy=false}}
            })
        }
        Text(if(es) "Al desactivarla, se oculta del panel de estudiantes y se pausan los recordatorios. Las inscripciones se conservan." else "Turning this off hides it from student dashboards and pauses reminders. Existing sign-ups are kept.",fontSize=13.sp)
        if(failed)Text(if(es) "No se pudo actualizar la configuración. Inténtalo de nuevo." else "Unable to update this setting. Please retry.",color=MaterialTheme.colorScheme.error)
    }
}
