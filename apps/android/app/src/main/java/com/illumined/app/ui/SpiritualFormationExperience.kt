package com.illumined.app.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.illumined.app.R
import com.illumined.app.ui.theme.IlluminedThemeTokens
import org.json.JSONObject

private data class FormationPrayer(val id: String, val title: String, val text: String)
private data class FormationHtml(val title: String, val html: String)
internal data class RosaryPrayers(val signOfTheCross:String,val apostlesCreed:String,val ourFather:String,val hailMary:String,val gloryBe:String,val fatimaPrayer:String,val hailHolyQueen:String,val concludingPrayer:String)
internal data class RosaryMystery(val title:String,val scripture:String)
internal data class RosarySet(val id: String, val title: String, val descriptionHtml: String, val mysteries:List<RosaryMystery>)
internal data class RosaryStep(val title:String,val text:String,val decadeCount:Int?=null)
private data class FormationCatalog(
    val prayers: List<FormationPrayer>,
    val lectio: FormationHtml,
    val examination: FormationHtml,
    val practices: List<FormationHtml>,
    val mysteries: List<RosarySet>,
    val rosaryPrayers: RosaryPrayers,
    val hours: List<FormationHtml>,
    val hoursDescription: String,
)

internal data class BreviaryLink(val title: String, val subtitle: String, val symbol: String, val url: String)

internal val breviaryLinks = listOf(
    BreviaryLink("iBreviary", "Full daily breviary with all hours", "book.closed", "https://www.ibreviary.com/m2/breviario.php"),
    BreviaryLink("Office of Readings", "Longer readings and psalmody", "text.book.closed", "https://www.ibreviary.com/m2/breviario.php?s=ufficio_delle_letture"),
    BreviaryLink("Morning Prayer", "Lauds for today", "sunrise", "https://www.ibreviary.com/m2/breviario.php?s=lodi"),
    BreviaryLink("Daytime Prayer", "Midday prayer from the daily office", "sun.max", "https://www.ibreviary.com/m2/breviario.php?s=ora_media"),
    BreviaryLink("Evening Prayer", "Vespers for today", "sunset", "https://www.ibreviary.com/m2/breviario.php?s=vespri"),
    BreviaryLink("Night Prayer", "Compline before rest", "moon.stars", "https://www.ibreviary.com/m2/breviario.php?s=compieta"),
    BreviaryLink("Divine Office Audio", "Pray with audio and spoken office", "speaker.wave.2", "https://divineoffice.org/"),
    BreviaryLink("Sing the Hours", "Chanted Liturgy of the Hours on YouTube", "music.note.tv", "https://www.youtube.com/@SingtheHours/videos"),
)

private data class FormationMenuRow(val title: String, val subtitle: String, val symbol: SpiritualFormationSymbolKind, val action: () -> Unit)

@Composable
fun SpiritualFormationExperience(
    memorizedPrayerIds: Set<String>,
    completedMysteryIds: Set<String>,
    onSetPrayerMemorized: (String, Boolean, () -> Unit, () -> Unit) -> Unit,
    onCompleteMystery: (String, () -> Unit, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val result = remember { runCatching { loadFormationCatalog(context.resources.openRawResource(R.raw.spiritual_formation).bufferedReader().use { it.readText() }) } }
    var route by rememberSaveable { mutableStateOf(FormationRoute.MENU) }
    val catalog = result.getOrNull()
    if (catalog == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Formation unavailable", color = Color.Red)
        }
        return
    }

    val destination = FormationRoute.parse(route)
    BackHandler(enabled = destination.kind != FormationRoute.MENU) {
        route = destination.back
    }
    when (destination.kind) {
        FormationRoute.MENU -> FormationMenu(
            onPrayers = { route = FormationRoute.PRAYER_HUB },
            onExamination = { route = FormationRoute.EXAMINATION },
            onMass = { route = FormationRoute.MASS_GUIDE },
            onPractices = { route = FormationRoute.PRACTICES },
        )
        FormationRoute.PRAYER_HUB -> FormationList("Prayer", { route = FormationRoute.MENU }, listOf(
            FormationMenuRow("Common Prayers", "${catalog.prayers.size} prayers", SpiritualFormationSymbolKind.Book) { route = FormationRoute.COMMON_PRAYERS },
            FormationMenuRow("Guided Rosary", "Pray the mysteries step by step", SpiritualFormationSymbolKind.RosaryGrid) { route = FormationRoute.ROSARY },
            FormationMenuRow("Guided Lectio Divina", "Read, meditate, pray, contemplate", SpiritualFormationSymbolKind.TextBook) { route = FormationRoute.detail(FormationRoute.HTML, "lectio", FormationRoute.PRAYER_HUB) },
            FormationMenuRow("Liturgy of the Hours", "The daily prayer of the Church", SpiritualFormationSymbolKind.Clock) { route = FormationRoute.detail(FormationRoute.HTML, "hours", FormationRoute.PRAYER_HUB) },
        ))
        FormationRoute.COMMON_PRAYERS -> FormationCards("Common Prayers", { route = FormationRoute.PRAYER_HUB }) {
            items(catalog.prayers, key = { it.id }) { prayer ->
                CommonPrayerCard(prayer.title, prayer.id in memorizedPrayerIds) {
                    route = FormationRoute.detail(FormationRoute.PRAYER, prayer.id, FormationRoute.COMMON_PRAYERS)
                }
            }
        }
        FormationRoute.ROSARY -> FormationCards("Guided Rosary", { route = FormationRoute.PRAYER_HUB }) {
            items(catalog.mysteries, key = { it.title }) { mystery ->
                FormationMenuCard(mystery.title, if (mystery.id in completedMysteryIds) "Completed" else "Five mysteries and Scripture reflections", SpiritualFormationSymbolKind.RosaryGrid) { route = FormationRoute.detail(FormationRoute.MYSTERY, mystery.id, FormationRoute.ROSARY) }
            }
        }
        FormationRoute.PRACTICES -> FormationCards("Spiritual Practices", { route = FormationRoute.MENU }) {
            itemsIndexed(catalog.practices, key = { _, practice -> practice.title }) { index, practice ->
                FormationMenuCard(practice.title, "Catholic habits and faithful living", SpiritualFormationSymbolKind.Walking) { route = FormationRoute.detail(FormationRoute.HTML, "practice-$index", FormationRoute.PRACTICES) }
            }
        }
        FormationRoute.EXAMINATION -> ExaminationExperience { route = FormationRoute.MENU }
        FormationRoute.MASS_GUIDE -> MassGuideExperience { route = FormationRoute.MENU }
        FormationRoute.PRAYER -> catalog.prayers.firstOrNull { it.id == destination.id }?.let { prayer ->
            PrayerDetail(prayer, prayer.id in memorizedPrayerIds, onBack = { route = destination.back }, onSetPrayerMemorized = onSetPrayerMemorized)
        } ?: LaunchedEffect(route) { route = FormationRoute.COMMON_PRAYERS }
        FormationRoute.HTML -> {
            if (destination.id == "hours") {
                LiturgyOfHoursPage(catalog.hoursDescription, onBack = { route = destination.back })
                return
            }
            val section = when (destination.id) {
                "lectio" -> catalog.lectio
                else -> destination.id.removePrefix("practice-").toIntOrNull()?.let(catalog.practices::getOrNull)
            }
            section?.let { HtmlFormationPage(it, destination.id == "lectio", onBack = { route = destination.back }) }
                ?: LaunchedEffect(route) { route = destination.back }
        }
        FormationRoute.MYSTERY -> catalog.mysteries.firstOrNull { it.id == destination.id }?.let { mystery ->
            RosaryMysteryPage(mystery, completed = mystery.id in completedMysteryIds, onBack = { route = destination.back }, onComplete = onCompleteMystery)
        } ?: LaunchedEffect(route) { route = FormationRoute.ROSARY }
        else -> LaunchedEffect(route) { route = FormationRoute.MENU }
    }
}

private enum class ExaminationStage { INTRO, PRAYER, CHECKLIST, SUMMARY }

@Composable
private fun ExaminationExperience(onExit:()->Unit){
    var stage by remember { mutableStateOf(ExaminationStage.INTRO) }
    var checked by remember { mutableStateOf(emptySet<String>()) }
    val checkedItems=ExaminationCatalog.sections.flatMapIndexed{sectionIndex,section->section.items.mapIndexed{itemIndex,text->"$sectionIndex-$itemIndex" to text}}.filter{it.first in checked}.map{it.second}
    BackHandler {
        stage = when (stage) {
            ExaminationStage.INTRO -> { onExit(); ExaminationStage.INTRO }
            ExaminationStage.PRAYER -> ExaminationStage.INTRO
            ExaminationStage.CHECKLIST -> ExaminationStage.PRAYER
            ExaminationStage.SUMMARY -> ExaminationStage.CHECKLIST
        }
    }
    when(stage){
        ExaminationStage.INTRO -> ExaminationIntroPage(onExit) { stage = ExaminationStage.PRAYER }
        ExaminationStage.PRAYER -> ExaminationPrayerPage(
            onBack = { stage = ExaminationStage.INTRO },
            onBegin = { stage = ExaminationStage.CHECKLIST },
        )
        ExaminationStage.CHECKLIST -> ExaminationChecklistPage(
            checked = checked,
            onToggle = { id -> checked = if (id in checked) checked - id else checked + id },
            onBack = { stage = ExaminationStage.PRAYER },
            onComplete = { stage = ExaminationStage.SUMMARY },
        )
        ExaminationStage.SUMMARY -> ExaminationSummaryPage(
            checkedItems = checkedItems,
            onBack = { stage = ExaminationStage.CHECKLIST },
            onFinish = { checked = emptySet(); onExit() },
        )
    }
}

@Composable
private fun ExaminationIntroPage(onBack: () -> Unit, onBegin: () -> Unit) = FormationCards(null, null) {
    item { TextButton(onClick = onBack) { Text("‹ Back") } }
    item { ExaminationCard {
        Text("Examination of Conscience", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        ExaminationIntro("I. What is an Examination of Conscience?", "An examination of conscience is a prayerful self-reflection on our thoughts, words, deeds, and omissions, measured against God’s commandments and the teaching of the Church. Its purpose is to recognize sins honestly, acknowledge God’s mercy, prepare for Confession, and form the conscience over time.")
        ExaminationIntro("II. Why is it Important?", "A good confession requires that we know and confess our sins honestly. Regular examination also fosters humility, self-awareness, growth in holiness, and a better alignment of conscience with God’s will.")
        ExaminationIntro("III. When and How Often?", "A thorough examination should be done before sacramental confession. A brief daily examen can be prayed at the end of the day. A deeper examination can also be helpful before retreats, spiritual direction, or major decisions.")
        ExaminationIntro("IV. Dispositions for a Good Examination", "Begin prayerfully. Ask the Holy Spirit for light and honesty. Avoid self-justification. Call sins what they are. Keep hope in God’s mercy, avoid despair, and renew your desire to amend your life.")
    } }
    item { Button(onClick = onBegin, modifier = Modifier.fillMaxWidth().height(54.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.PlayCircle, Color.White, Modifier.size(19.dp), IlluminedThemeTokens.Blue); Spacer(Modifier.width(8.dp)); Text("Begin Examination", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) } }
    item { Text("Private: your checked items are only kept on this screen while you pray. They are not saved, uploaded, or shared with your instructor.", color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
}

@Composable
private fun ExaminationPrayerPage(onBack: () -> Unit, onBegin: () -> Unit) = FormationCards(null, null) {
    item { TextButton(onClick = onBack) { Text("‹ Back") } }
    item { ExaminationCard { Text("Prayer Before Examination", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(ExaminationCatalog.preExamPrayer, fontSize = 18.sp, lineHeight = 28.sp, color = IlluminedThemeTokens.Ink) } }
    item { ExaminationCard { Text("Examination of Conscience", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text("Move prayerfully through the commandments, the deadly sins, sins of omission, and final questions about love. Check only what helps you prepare honestly before God.", fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.SecondaryText) } }
    item { Button(onClick = onBegin, modifier = Modifier.fillMaxWidth().height(54.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.Checklist, Color.White, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("Begin Checklist", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) } }
}

@Composable
private fun ExaminationChecklistPage(checked: Set<String>, onToggle: (String) -> Unit, onBack: () -> Unit, onComplete: () -> Unit) = FormationCards(null, null) {
    item { TextButton(onClick = onBack) { Text("‹ Back") } }
    item { ExaminationCard { Text("Examination tool", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text("Check the items that you prayerfully recognize. This list is private and disappears when you leave the examination.", fontSize = 15.sp, lineHeight = 23.sp, color = IlluminedThemeTokens.SecondaryText) } }
    itemsIndexed(ExaminationCatalog.sections, key = { index, _ -> index }) { sectionIndex, section ->
        ExaminationCard {
            Text(section.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
            section.items.forEachIndexed { itemIndex, label ->
                val id = "$sectionIndex-$itemIndex"
                TextButton(onClick = { onToggle(id) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        SpiritualFormationSymbol(if (id in checked) SpiritualFormationSymbolKind.SquareOn else SpiritualFormationSymbolKind.SquareOff, if (id in checked) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText, Modifier.size(21.dp))
                        Spacer(Modifier.width(12.dp)); Text(label, Modifier.weight(1f), fontSize = 15.sp, color = IlluminedThemeTokens.Ink)
                    }
                }
            }
        }
    }
    item { Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(54.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.CheckSeal, Color.White, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("Complete Examination", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) } }
}

@Composable
private fun ExaminationSummaryPage(checkedItems: List<String>, onBack: () -> Unit, onFinish: () -> Unit) = FormationCards(null, null) {
    item { TextButton(onClick = onBack) { Text("‹ Back") } }
    item { ExaminationCard { Text("Private Examination Summary", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text("Examination tool", fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.SecondaryText); Text("Use this only for your own prayer and preparation. Nothing on this page is saved or shared.", fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText) } }
    item { ExaminationCard { Text("Items Checked", fontSize = 18.sp, fontWeight = FontWeight.SemiBold); if (checkedItems.isEmpty()) Text("No items were checked.", color = IlluminedThemeTokens.SecondaryText) else checkedItems.forEach { Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(7.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.CheckCircle, IlluminedThemeTokens.Blue, Modifier.size(15.dp)); Text(it, Modifier.weight(1f), fontSize = 14.sp, color = IlluminedThemeTokens.Blue) } } } }
    item { ExaminationCard { Text("Act of Contrition", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(ExaminationCatalog.actOfContrition, fontSize = 18.sp, lineHeight = 28.sp, color = IlluminedThemeTokens.Ink) } }
    item { OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish and Clear Private Checklist") } }
}

@Composable private fun ExaminationCard(content:@Composable ColumnScope.()->Unit){Surface(shape=RoundedCornerShape(16.dp),color=Color.White.copy(.94f),shadowElevation=6.dp,border=androidx.compose.foundation.BorderStroke(1.dp,IlluminedThemeTokens.Gold.copy(.22f))){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}}
@Composable private fun ExaminationIntro(title:String,text:String){Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Text(title,fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Ink);Text(text,fontSize=16.sp,lineHeight=24.sp,color=IlluminedThemeTokens.SecondaryText)}}

@Composable
private fun FormationMenu(onPrayers: () -> Unit, onExamination: () -> Unit, onMass: () -> Unit, onPractices: () -> Unit) =
    FormationList(null, null, listOf(
        FormationMenuRow("Prayers", "Common prayers, rosary, lectio divina, and the hours", SpiritualFormationSymbolKind.Prayers, onPrayers),
        FormationMenuRow("Examination of Conscience", "Prayerful review and preparation for confession", SpiritualFormationSymbolKind.Search, onExamination),
        FormationMenuRow("Guide to the Mass", "Walk through the order, prayers, readings, and Eucharistic Prayer", SpiritualFormationSymbolKind.Church, onMass),
        FormationMenuRow("Spiritual Practices", "Works of mercy, precepts, habits, and Catholic living", SpiritualFormationSymbolKind.Walking, onPractices),
    ))

@Composable
private fun FormationList(title: String?, onBack: (() -> Unit)?, rows: List<FormationMenuRow>) {
    FormationCards(title, onBack) { items(rows) { row -> FormationMenuCard(row.title, row.subtitle, row.symbol, row.action) } }
}

@Composable
private fun FormationCards(title: String?, onBack: (() -> Unit)?, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(formationBrush()), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (onBack != null || title != null) item {
            if (onBack != null) TextButton(onClick = onBack) { Text("‹ Back") }
            if (title != null) Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
        }
        content()
    }
}

@Composable
private fun FormationMenuCard(title: String, subtitle: String, symbol: SpiritualFormationSymbolKind, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                SpiritualFormationSymbol(symbol, IlluminedThemeTokens.Gold, Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                Text(subtitle, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
            }
            LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
        }
    }
}

/** Mirrors the iOS CommonPrayerRow completion treatment. */
@Composable
private fun CommonPrayerCard(title: String, memorized: Boolean, onClick: () -> Unit) {
    val accent = if (memorized) IlluminedThemeTokens.Blue else IlluminedThemeTokens.Gold
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(accent.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                FormationGameSymbol(
                    if (memorized) FormationGameSymbolKind.CheckCircleFilled else FormationGameSymbolKind.EmptyCircle,
                    accent,
                    Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                if (memorized) Text("Memorized", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
            }
            LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
        }
    }
}

@Composable
private fun PrayerDetail(prayer: FormationPrayer, memorized: Boolean, onBack: () -> Unit,
    onSetPrayerMemorized: (String, Boolean, () -> Unit, () -> Unit) -> Unit) {
    var saving by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(formationBrush()).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        Text(prayer.title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp) {
            Text(prayer.text, Modifier.padding(20.dp), fontSize = 18.sp, lineHeight = 29.sp, color = IlluminedThemeTokens.Ink)
        }
        Button(onClick = { saving = true; onSetPrayerMemorized(prayer.id, !memorized, { saving = false }, { saving = false }) },
            enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(if (memorized) "Mark as Not Memorized" else "Mark as Memorized")
        }
    }
}

@Composable
private fun HtmlFormationPage(section: FormationHtml, showsDailyGospel: Boolean = false, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(formationBrush()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        Text(section.title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context -> WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT); webViewClient = WebViewClient()
        } }, update = { it.loadDataWithBaseURL(null, styledHtml(section.html), "text/html", "UTF-8", null) })
        if (showsDailyGospel) Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { HomeSymbol(HomeSymbolKind.CalendarBadgeClock, IlluminedThemeTokens.Blue, Modifier.size(20.dp)); Text("Daily Gospel", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
                Text("Use today's Gospel as the scripture passage for Lectio Divina. The official USCCB daily readings page updates each day with the Church's lectionary readings.", fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.Ink)
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MassGuideCatalog.dailyReadingsUrl))) }, modifier = Modifier.fillMaxWidth().height(54.dp)) { MassGuideSymbol(MassGuideSymbolKind.ExternalLink, Color.White, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Open Today's Gospel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun LiturgyOfHoursPage(description: String, onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize().background(formationBrush()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("‹ Back") } }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp,
            ) {
                Text(description, Modifier.padding(18.dp), fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.SecondaryText)
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.Link, IlluminedThemeTokens.Blue, Modifier.size(20.dp)); Text("Open Today's Breviary", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
                    Text("Use these links to pray the current Liturgy of the Hours outside the app. The pages update daily.", fontSize = 15.sp, lineHeight = 22.sp, color = IlluminedThemeTokens.SecondaryText)
                    breviaryLinks.forEach { link ->
                        Surface(
                            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))) } },
                            modifier = Modifier.fillMaxWidth().border(1.dp, IlluminedThemeTokens.Gold.copy(.16f), RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp), color = Color.White.copy(.72f),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                                    SpiritualFormationSymbol(breviarySymbol(link.symbol), IlluminedThemeTokens.Gold, Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(link.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                                    Text(link.subtitle, fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText)
                                }
                                MassGuideSymbol(MassGuideSymbolKind.ExternalLink, IlluminedThemeTokens.Blue, Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RosaryMysteryPage(
    mystery: RosarySet,
    completed: Boolean,
    onBack: () -> Unit,
    onComplete: (String, () -> Unit, () -> Unit) -> Unit,
) {
    var started by rememberSaveable(mystery.id) { mutableStateOf(false) }
    val catalogContext = LocalContext.current
    val catalog = remember { runCatching { loadFormationCatalog(catalogContext.resources.openRawResource(R.raw.spiritual_formation).bufferedReader().use { it.readText() }) }.getOrNull() }
    val sequence = remember(mystery.id, catalog) { catalog?.let { buildRosarySequence(it.rosaryPrayers,mystery) }.orEmpty() }
    if(started && sequence.isNotEmpty()) { GuidedRosaryPage(mystery.id,sequence,onBack={started=false},onComplete=onComplete);return }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(formationBrush()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        Text(mystery.title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context -> WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT); webViewClient = WebViewClient()
        } }, update = { it.loadDataWithBaseURL(null, styledHtml(mystery.descriptionHtml), "text/html", "UTF-8", null) })
        if (error) Text("Your Rosary progress could not be saved.", color = Color.Red)
        Button(
            onClick = { started=true },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(if(completed) "Pray Again" else "Start Rosary") }
    }
}

@Composable
private fun GuidedRosaryPage(
    mysteryId: String,
    sequence: List<RosaryStep>,
    onBack: () -> Unit,
    onComplete: (String, () -> Unit, () -> Unit) -> Unit,
) {
    var index by rememberSaveable(mysteryId) { mutableIntStateOf(0) }
    index = index.coerceIn(0, sequence.lastIndex)
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val step = sequence[index]

    fun advanceRosary() {
        if (index < sequence.lastIndex) {
            index++
        } else {
            saving = true
            error = false
            onComplete(
                mysteryId,
                { saving = false; index = 0; onBack() },
                { saving = false; error = true },
            )
        }
    }

    Column(
        Modifier.fillMaxSize().background(formationBrush()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, enabled = !saving, modifier = Modifier.align(Alignment.Start)) {
            Text("‹ Back")
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val availableHeight = this.maxHeight
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = availableHeight),
                    contentAlignment = Alignment.Center,
                ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !saving) { advanceRosary() },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(.94f),
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            step.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IlluminedThemeTokens.Blue,
                        )
                        Text(
                            step.text,
                            fontSize = 20.sp,
                            lineHeight = 30.sp,
                            color = IlluminedThemeTokens.Ink,
                        )
                        step.decadeCount?.let {
                            Text("$it / 10", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Gold)
                        }
                        Text("Step ${index + 1} of ${sequence.size}", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText)
                    }
                }
                }
            }
        }

        if (error) Text("Your Rosary progress could not be saved.", color = Color.Red)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { index = (index - 1).coerceAtLeast(0) }, enabled = index > 0 && !saving) {
                Text("Back")
            }
        }
    }
}

internal fun buildRosarySequence(prayers:RosaryPrayers,set:RosarySet):List<RosaryStep> = buildList {
    add(RosaryStep("Sign of the Cross",prayers.signOfTheCross));add(RosaryStep("Apostles' Creed",prayers.apostlesCreed));add(RosaryStep("Our Father",prayers.ourFather));add(RosaryStep("Hail Mary (for Faith)",prayers.hailMary));add(RosaryStep("Hail Mary (for Hope)",prayers.hailMary));add(RosaryStep("Hail Mary (for Charity)",prayers.hailMary));add(RosaryStep("Glory Be",prayers.gloryBe))
    set.mysteries.forEachIndexed{index,mystery->add(RosaryStep("Mystery ${index+1}: ${mystery.title}",mystery.scripture));add(RosaryStep("Our Father",prayers.ourFather));repeat(10){count->add(RosaryStep("Hail Mary",prayers.hailMary,count+1))};add(RosaryStep("Glory Be",prayers.gloryBe));add(RosaryStep("Fatima Prayer",prayers.fatimaPrayer))}
    add(RosaryStep("Hail, Holy Queen",prayers.hailHolyQueen));add(RosaryStep("Concluding Prayer",prayers.concludingPrayer));add(RosaryStep("Final Sign of the Cross",prayers.signOfTheCross));add(RosaryStep("Rosary Completed","You have completed the Holy Rosary. Peace be with you."))
}

private fun loadFormationCatalog(text: String): FormationCatalog {
    val root = JSONObject(text)
    val prayers = root.getJSONArray("commonPrayers").let { array -> (0 until array.length()).map { i -> array.getJSONObject(i).let { FormationPrayer(it.getString("id"), it.getString("title"), it.getString("text")) } } }
    fun html(obj: JSONObject) = FormationHtml(obj.getString("title"), obj.optString("contentHTML", obj.optString("description")))
    val rosaryRoot=root.getJSONObject("rosary");val prayerRoot=rosaryRoot.getJSONObject("prayers")
    val rosaryPrayers=RosaryPrayers(prayerRoot.getString("signOfTheCross"),prayerRoot.getString("apostlesCreed"),prayerRoot.getString("ourFather"),prayerRoot.getString("hailMary"),prayerRoot.getString("gloryBe"),prayerRoot.getString("fatimaPrayer"),prayerRoot.getString("hailHolyQueen"),prayerRoot.getString("concludingPrayer"))
    val rosary = rosaryRoot.getJSONArray("mysteries").let { array -> (0 until array.length()).map { i -> array.getJSONObject(i).let {
        val mysteryItems = it.getJSONArray("mysteries")
        val items=(0 until mysteryItems.length()).map{index->mysteryItems.getJSONObject(index).let{item->RosaryMystery(item.getString("title"),item.getString("scripture"))}}
        RosarySet(it.getString("id"), it.getString("title"), it.getString("descriptionHTML"),items)
    } } }
    val practices = root.getJSONArray("spiritualPractices").let { array -> (0 until array.length()).map { i -> html(array.getJSONObject(i)) } }
    val hoursRoot = root.getJSONObject("liturgyOfTheHours")
    val hours = hoursRoot.getJSONArray("hours").let { array -> (0 until array.length()).map { i -> array.getJSONObject(i).let { FormationHtml(it.getString("title"), "<h2>${it.getString("title")}</h2><p>${it.getString("description")}</p>") } } }
    return FormationCatalog(prayers, html(root.getJSONObject("lectioDivina")), html(root.getJSONObject("examinationOfConscience")), practices, rosary, rosaryPrayers, hours, hoursRoot.getString("description"))
}

private fun styledHtml(body: String) = """<html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{font-family:Georgia,serif;color:#1e1c1a;font-size:17px;line-height:1.55;background:#fdfdfc;padding:10px}h1,h2,h3{color:#3b6fa0}a{color:#3b6fa0}</style></head><body>$body</body></html>"""
private fun formationBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
