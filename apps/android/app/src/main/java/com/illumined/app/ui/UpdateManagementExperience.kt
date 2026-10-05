package com.illumined.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.functions.FirebaseFunctions
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.text.DateFormat
import java.util.*

private fun managementT(en:String,es:String) = if(Locale.getDefault().language == "es") es else en

@Composable
fun UpdateManagementExperience(onBack:()->Unit) {
    val functions = remember { FirebaseFunctions.getInstance("us-central1") }
    var items by remember { mutableStateOf(emptyList<Map<*,*>>()) }
    var id by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var revision by remember { mutableStateOf(0L) }
    var title by remember { mutableStateOf("") }; var message by remember { mutableStateOf("") }
    var startup by remember { mutableStateOf(true) }; var push by remember { mutableStateOf(true) }
    var scheduled by remember { mutableStateOf(false) }; var expires by remember { mutableStateOf(false) }
    var publishTime by remember { mutableStateOf(System.currentTimeMillis()+3600000) }; var expiry by remember { mutableStateOf(System.currentTimeMillis()+604800000) }
    var busy by remember { mutableStateOf(false) }; var status by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf(false) }; var pending by remember { mutableStateOf<Map<String,Any>?>(null) }
    fun refresh() {
        functions.getHttpsCallable("manageInstructorUpdates").call(mapOf("action" to "list")).addOnSuccessListener { items = ((it.data as? Map<*,*>)?.get("items") as? List<*>)?.filterIsInstance<Map<*,*>>().orEmpty() }.addOnFailureListener { status = it.localizedMessage.orEmpty() }
    }
    fun run(data:Map<String,Any>) {
        busy = true
        functions.getHttpsCallable("manageInstructorUpdates").call(data).addOnSuccessListener {
            busy = false
            val result = it.data as? Map<*,*> ?: emptyMap<String,Any>()
            status = if(data["action"] == "stats") managementT("Acknowledged: ","Confirmado: ")+result["acknowledged"]+managementT(" of current instructors: "," de instructores actuales: ")+result["instructors"]+"\n"+managementT("Push requests accepted: ","Solicitudes push aceptadas: ")+result["accepted"]+" / "+result["attempted"]+managementT(" devices. Not proof of delivery or reading."," dispositivos. No confirma la entrega ni la lectura.") else managementT("Updated.","Actualizado.")
            refresh()
        }.addOnFailureListener { busy = false; status = it.localizedMessage.orEmpty() }
    }
    fun save(publish:Boolean, schedule:Boolean) {
        busy = true
        val data = mapOf("action" to if(schedule) "schedule" else "save","id" to id,"revision" to revision,"title" to title,"message" to message,"showOnStartup" to startup,"sendPush" to push,"publishAtMs" to if(scheduled) publishTime else null,"expiresAtMs" to if(expires) expiry else null)
        functions.getHttpsCallable("manageInstructorUpdates").call(data).addOnSuccessListener {
            busy = false; revision = ((it.data as? Map<*,*>)?.get("revision") as? Number)?.toLong() ?: revision
            if(publish) run(mapOf("action" to "publish","id" to id,"revision" to revision)) else { status = managementT("Saved.","Guardado."); refresh() }
        }.addOnFailureListener { busy = false; status = it.localizedMessage.orEmpty() }
    }
    LaunchedEffect(Unit) { refresh() }
    LazyColumn(Modifier.fillMaxSize().background(IlluminedThemeTokens.Cream),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick=onBack) { Text(managementT("Back","Atrás")) } }
        item { Card { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(managementT("Update Management","Gestión de novedades"),fontSize=24.sp,fontWeight=FontWeight.Bold,color=IlluminedThemeTokens.Blue)
            OutlinedTextField(title,{title=it},enabled=!busy,label={Text(managementT("Title","Título"))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(message,{message=it},enabled=!busy,label={Text(managementT("Message","Mensaje"))},minLines=5,modifier=Modifier.fillMaxWidth())
            Row { Text(managementT("Show at instructor startup","Mostrar al iniciar para instructores"),Modifier.weight(1f)); Switch(startup,{startup=it},enabled=!busy) }
            Row { Text(managementT("Send push notification","Enviar notificación push"),Modifier.weight(1f)); Switch(push,{push=it},enabled=!busy) }
            Row { Text(managementT("Schedule publication","Programar publicación"),Modifier.weight(1f)); Switch(scheduled,{scheduled=it},enabled=!busy) }
            if(scheduled) ManagementDate(managementT("Publish","Publicar"),publishTime,!busy) {publishTime=it}
            Row { Text(managementT("Expire startup card","Vencimiento de la tarjeta inicial"),Modifier.weight(1f)); Switch(expires,{expires=it},enabled=!busy) }
            if(expires) ManagementDate(managementT("Expires","Vence"),expiry,!busy) {expiry=it}
            Text(managementT("Device time zone: ","Zona horaria del dispositivo: ")+TimeZone.getDefault().displayName,fontSize=12.sp)
            Text(managementT("Publication is checked every five minutes. Expiration and withdrawal stop startup display; inbox history remains.","La publicación se comprueba cada cinco minutos. El vencimiento y la retirada detienen la tarjeta inicial; el historial permanece."),fontSize=13.sp)
            Row { TextButton(enabled=!busy,onClick={id=UUID.randomUUID().toString();revision=0;title="";message="";scheduled=false;expires=false;status=""}) {Text(managementT("New draft","Nuevo borrador"))};TextButton(onClick={preview=true}) {Text(managementT("Preview","Vista previa"))} }
            Button(enabled=!busy,onClick={save(false,false)}) {Text(managementT("Save draft","Guardar borrador"))}
            Button(enabled=!busy,onClick={pending=mapOf("action" to "publish-form")}) {Text(if(scheduled) managementT("Schedule update","Programar novedad") else managementT("Publish now","Publicar ahora"))}
            if(busy) CircularProgressIndicator()
            if(status.isNotBlank()) Text(status)
        } } }
        item { TextButton(onClick={refresh()},enabled=!busy) {Text(managementT("Refresh management list","Actualizar lista"))} }
        items(items,key={it["id"].toString()}) { item -> Card { Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            val state=item["state"].toString()
            Text(item["title"].toString(),fontSize=21.sp,fontWeight=FontWeight.Bold,color=IlluminedThemeTokens.Blue);Text(state)
            (item["publishAtMs"] as? Number)?.let {Text(managementT("Publish: ","Publicar: ")+DateFormat.getDateTimeInstance().format(Date(it.toLong())))}
            (item["expiresAtMs"] as? Number)?.let {Text(managementT("Expires: ","Vence: ")+DateFormat.getDateTimeInstance().format(Date(it.toLong())))}
            (item["error"] as? String)?.let {Text(it)}
            if(state in listOf("draft","scheduled","failed")) {
                TextButton(enabled=!busy,onClick={id=item["id"].toString();revision=(item["revision"] as? Number)?.toLong() ?: 0;title=item["title"].toString();message=item["message"].toString();startup=item["showOnStartup"]==true;push=item["sendPush"]!=false;scheduled=item["publishAtMs"] is Number;expires=item["expiresAtMs"] is Number;(item["publishAtMs"] as? Number)?.let {publishTime=it.toLong()};(item["expiresAtMs"] as? Number)?.let {expiry=it.toLong()};status=managementT("Draft opened in the editor above.","Borrador abierto en el editor de arriba.")}) {Text(managementT("Open draft","Abrir borrador"))}
                if(state=="scheduled") TextButton(enabled=!busy,onClick={pending=mapOf("action" to "cancel","id" to item["id"].toString(),"revision" to ((item["revision"] as? Number)?.toLong() ?: 0))}) {Text(managementT("Cancel schedule","Cancelar programación"))}
            }
            if(state in listOf("published","withdrawn")) {
                TextButton(enabled=!busy,onClick={run(mapOf("action" to "stats","id" to item["id"].toString()))}) {Text(managementT("View results","Ver resultados"))}
                if(state=="published") TextButton(enabled=!busy,onClick={pending=mapOf("action" to "withdraw","id" to item["id"].toString())}) {Text(managementT("Withdraw startup card","Retirar tarjeta inicial"))}
            }
        } } }
    }
    if(pending!=null) AlertDialog(onDismissRequest={pending=null},title={Text(managementT("Confirm update action","Confirmar acción"))},text={Text(if(pending?.get("action")=="publish-form") "$title\n\n$message" else managementT("This changes the selected update. Push notifications already sent cannot be recalled.","Esto cambia la novedad seleccionada. No se pueden recuperar las notificaciones ya enviadas."))},confirmButton={TextButton(onClick={val data=pending;pending=null;if(data?.get("action")=="publish-form") save(!scheduled,scheduled) else if(data!=null) run(data)}) {Text(managementT("Confirm","Confirmar"))}},dismissButton={TextButton(onClick={pending=null}) {Text(managementT("Cancel","Cancelar"))}})
    if(preview) AlertDialog(onDismissRequest={preview=false},containerColor=IlluminedThemeTokens.Blue,titleContentColor=Color.White,textContentColor=Color.White,title={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(managementT("ILLUMINED UPDATE · PREVIEW","NOVEDADES DE ILLUMINED · VISTA PREVIA"),fontSize=13.sp);Text(title,fontSize=28.sp,fontWeight=FontWeight.Bold);Box(Modifier.width(86.dp).height(3.dp).background(IlluminedThemeTokens.Gold))}},text={Text(message,Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),fontSize=18.sp,lineHeight=27.sp)},confirmButton={Button(onClick={preview=false},colors=ButtonDefaults.buttonColors(containerColor=IlluminedThemeTokens.Gold,contentColor=Color.Black)){Text(managementT("Close preview","Cerrar vista previa"))}})
}

@Composable private fun ManagementDate(label:String,value:Long,enabled:Boolean,onChange:(Long)->Unit) {
    val context=LocalContext.current
    OutlinedButton(enabled=enabled,onClick={val c=Calendar.getInstance().apply{timeInMillis=value};DatePickerDialog(context,{_,year,month,day->c.set(year,month,day);TimePickerDialog(context,{_,hour,minute->c.set(Calendar.HOUR_OF_DAY,hour);c.set(Calendar.MINUTE,minute);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);onChange(c.timeInMillis)},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),android.text.format.DateFormat.is24HourFormat(context)).show()},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()}) {Text(label+": "+DateFormat.getDateTimeInstance().format(Date(value)))}
}
