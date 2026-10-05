package com.illumined.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.illumined.app.ui.theme.IlluminedThemeTokens

internal object ParishTimeZones {
    val common = listOf("America/New_York","America/Chicago","America/Denver","America/Phoenix","America/Los_Angeles","America/Anchorage","Pacific/Honolulu","America/Puerto_Rico","Europe/Madrid")
    private val names = mapOf("America/New_York" to ("Eastern Time (New York)" to "Hora del Este (Nueva York)"),"America/Chicago" to ("Central Time (Chicago)" to "Hora Central (Chicago)"),"America/Denver" to ("Mountain Time (Denver)" to "Hora de la Montaña (Denver)"),"America/Phoenix" to ("Arizona Time (Phoenix)" to "Hora de Arizona (Phoenix)"),"America/Los_Angeles" to ("Pacific Time (Los Angeles)" to "Hora del Pacífico (Los Ángeles)"),"America/Anchorage" to ("Alaska Time (Anchorage)" to "Hora de Alaska (Anchorage)"),"Pacific/Honolulu" to ("Hawaii Time (Honolulu)" to "Hora de Hawái (Honolulu)"),"America/Puerto_Rico" to ("Puerto Rico Time" to "Hora de Puerto Rico"),"Europe/Madrid" to ("Spain Time (Madrid)" to "Hora de España (Madrid)"))
    fun label(id: String, locale: Locale): String {
        names[id]?.let { return if(locale.language == "es") it.second else it.first }
        val city = id.substringAfterLast('/').replace('_',' ')
        val name = runCatching { ZoneId.of(id).getDisplayName(java.time.format.TextStyle.FULL, locale) }.getOrDefault(id)
        return "$name ($city)"
    }
}
@Composable internal fun ParishTimeZonePicker(value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val spanish = locale.language == "es"
    var show by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    Text(if(spanish) "Zona horaria de la parroquia" else "Parish time zone", fontWeight = FontWeight.SemiBold)
    OutlinedButton(onClick = { search = ""; show = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(ParishTimeZones.label(value,locale) + " ▾", modifier = Modifier.fillMaxWidth(), color = IlluminedThemeTokens.Blue)
    }
    if(show) {
        val all = (ParishTimeZones.common + ZoneId.systemDefault().id + value + ZoneId.getAvailableZoneIds().sorted()).distinct()
        val filtered = all.filter { search.isBlank() || ParishTimeZones.label(it,locale).contains(search,true) || it.contains(search,true) }
        AlertDialog(onDismissRequest = { show = false },
            title = { Text(if(spanish) "Zona horaria" else "Time Zone") },
            text = {
                Column {
                    OutlinedTextField(search,{search=it},label={Text(if(spanish) "Buscar ciudad o zona" else "Search city or time zone")},modifier=Modifier.fillMaxWidth())
                    TextButton(onClick = { onChange(ZoneId.systemDefault().id); show = false }) { Text(if(spanish) "Usar zona del dispositivo" else "Use device time zone") }
                    LazyColumn(Modifier.heightIn(max=360.dp)) {
                        items(filtered,key={it}) { id ->
                            TextButton(onClick={onChange(id);show=false},modifier=Modifier.fillMaxWidth()) {
                                Text((if(value==id) "✓ " else "")+ParishTimeZones.label(id,locale),modifier=Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick={show=false}) { Text(if(spanish) "Cancelar" else "Cancel") } }
        )
    }
}
@Composable internal fun ParishReminderTimePicker(value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val time = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.of(9,0))
    val format = if(DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    OutlinedButton(onClick = {
        TimePickerDialog(context,{_,hour,minute -> onChange(String.format(Locale.US,"%02d:%02d",hour,minute))},time.hour,time.minute,DateFormat.is24HourFormat(context)).show()
    },modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(if(locale.language=="es") "Hora del recordatorio diario" else "Daily reminder time",fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
            Text(time.format(DateTimeFormatter.ofPattern(format,locale)))
        }
    }
}
@Composable internal fun ParishDatePicker(label: String, value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val date = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
    OutlinedButton(onClick = {
        DatePickerDialog(context,{_,year,month,day->onChange(LocalDate.of(year,month+1,day).toString())},date.year,date.monthValue-1,date.dayOfMonth).show()
    },enabled=enabled,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(label,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
            Text(if(value.isBlank()) if(locale.language=="es") "Elegir fecha" else "Choose date" else date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)))
        }
    }
}
