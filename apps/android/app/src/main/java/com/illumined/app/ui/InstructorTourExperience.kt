package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.illumined.app.R
import com.illumined.app.ui.theme.IlluminedThemeTokens
import org.json.JSONArray

private fun tourT(en:String,es:String)=if(java.util.Locale.getDefault().language=="es")es else en

@Composable
fun InstructorTourExperience(userId:String,onClose:()->Unit) {
    val context=LocalContext.current
    val prefs=remember(userId){context.getSharedPreferences("instructor-tour",0)}
    val key="v1-$userId"
    val steps=remember { runCatching { JSONArray(context.resources.openRawResource(R.raw.instructor_tour).bufferedReader().use{it.readText()}) }.getOrElse{JSONArray()} }
    var step by remember(userId) { mutableIntStateOf(prefs.getInt("$key-step",0).coerceIn(0,(steps.length()-1).coerceAtLeast(0))) }
    var tried by remember { mutableStateOf(false) }
    val scroll=rememberScrollState()
    LaunchedEffect(step) { scroll.scrollTo(0);prefs.edit().putInt("$key-step",step).putBoolean("$key-offered",true).apply() }
    Column(Modifier.fillMaxSize().background(IlluminedThemeTokens.Cream).verticalScroll(scroll).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Text(tourT("Explore Illumined","Explora Illumined"),fontSize=26.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)
        Text(tourT("DEMO · Sample content only. Nothing changes your classroom.","DEMOSTRACIÓN · Solo contenido de ejemplo. Tu aula no se modifica."),fontSize=14.sp)
        if(steps.length()>0) {
            val data=steps.getJSONObject(step)
            fun value(name:String):String { val array=data.getJSONArray(name);return array.getString(if(java.util.Locale.getDefault().language=="es")1 else 0) }
            Text(tourT("Step ","Paso ")+"${step+1} / ${steps.length()}",fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={(step+1).toFloat()/steps.length()},modifier=Modifier.fillMaxWidth(),color=IlluminedThemeTokens.Gold)
            Card(colors=CardDefaults.cardColors(containerColor=Color.White)) { Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(value("title"),fontSize=24.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)
                Text(value("body"),fontSize=18.sp,lineHeight=27.sp)
            } }
            Card(colors=CardDefaults.cardColors(containerColor=Color.White)) { Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text(value("sample"),fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Blue)
                Box(Modifier.width(80.dp).height(3.dp).background(IlluminedThemeTokens.Gold))
                Text(value(if(tried)"after" else "before"),fontSize=17.sp,lineHeight=25.sp)
                Button(onClick={tried=!tried},colors=ButtonDefaults.buttonColors(containerColor=IlluminedThemeTokens.Blue)) {Text(if(tried)tourT("Reset example","Reiniciar ejemplo") else value("action"))}
            } }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(enabled=step>0,onClick={step--;tried=false}) {Text(tourT("Back","Atrás"))}
                Button(onClick={if(step==steps.length()-1){prefs.edit().putInt("$key-step",0).putBoolean("$key-completed",true).apply();onClose()}else{step++;tried=false}}) {Text(if(step==steps.length()-1)tourT("Finish tour","Finalizar recorrido") else tourT("Next","Siguiente"))}
            }
            TextButton(onClick=onClose) {Text(tourT("Skip / Continue later","Omitir / Continuar después"))}
            TextButton(onClick={step=0;tried=false}) {Text(tourT("Restart tour","Reiniciar recorrido"))}
            Text(tourT("Your place is saved for this account on this device.","Tu lugar se guarda para esta cuenta en este dispositivo."),fontSize=13.sp)
        }else{Text(tourT("The demo could not be loaded.","No se pudo cargar la demostración."));TextButton(onClick=onClose){Text(tourT("Close","Cerrar"))}}
    }
}
