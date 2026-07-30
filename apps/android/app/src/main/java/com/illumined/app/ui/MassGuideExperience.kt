package com.illumined.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.illumined.app.ui.theme.IlluminedThemeTokens

@Composable
internal fun MassGuideExperience(onExit: () -> Unit) {
    var stack by rememberSaveable { mutableStateOf(listOf(MassGuideRoute.PARTS)) }
    val screen = MassGuideRoute.resolve(stack.last())
    val back = { if (stack.size > 1) stack = stack.dropLast(1) else onExit() }
    BackHandler(onBack = back)
    // LazyColumn preserves its position at a composition call site. Keying by route creates
    // a fresh list for every Mass Guide destination, so each page begins at the top.
    key(stack.last()) {
        when (screen) {
            MassGuideDestination.Parts -> MassPartsPage(onBack = back, onPart = { stack = stack + MassGuideRoute.part(it.id) })
            is MassGuideDestination.Part -> MassPartPage(
                part = screen.value,
                onBack = back,
                onPrayer = { stack = stack + MassGuideRoute.prayer(it.id) },
                onNext = { stack = stack + MassGuideRoute.part(it.id) },
            )
            is MassGuideDestination.Prayer -> MassPrayerPage(screen.value, back)
        }
    }
}

@Composable
private fun MassPartsPage(onBack: () -> Unit, onPart: (MassGuidePart) -> Unit) = MassList {
    item { TextButton(onClick = onBack) { Text("‹ Back") } }
    item { MassCard {
        MassSymbolLabel(MassGuideSymbolKind.Church, "Order of Mass", 22, IlluminedThemeTokens.Blue)
        Text("The celebration of the Mass consists of four major parts: The Introductory Rite, the Liturgy of the Word, The Liturgy of the Eucharist, and the Concluding Rite. Use this guide to follow along with the Mass and learn more about each part.", fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.SecondaryText)
    } }
    items(MassGuideCatalog.parts, key = { it.id }) { part ->
        Surface(onClick = { onPart(part) }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                MassNumber(part.number)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(part.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    Text(part.subtitle, fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText)
                }
                LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun MassPartPage(part: MassGuidePart, onBack: () -> Unit, onPrayer: (MassPrayerOption) -> Unit, onNext: (MassGuidePart) -> Unit) {
    val context = LocalContext.current
    val readingTitles = remember { setOf("First Reading", "Responsorial Psalm", "Second Reading", "Gospel Acclamation and Gospel") }
    val readings = part.rows.filter { it.title in readingTitles }.takeIf { part.showsDailyReadings }
    val displayRows = if (readings == null) part.rows else part.rows.filterNot { it.title in readingTitles }
    val nextPart = MassGuideCatalog.parts.getOrNull(MassGuideCatalog.parts.indexOfFirst { it.id == part.id } + 1)
    MassList(bottomPadding = 104) {
        item { TextButton(onClick = onBack) { Text("‹ Back") } }
        item { MassCard {
            Row(verticalAlignment = Alignment.CenterVertically) { MassNumber(part.number); Spacer(Modifier.width(12.dp)); MassGuideSymbol(massPartSymbol(part.id), IlluminedThemeTokens.Blue, Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text(part.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
            Text(part.detail, fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.SecondaryText)
        } }
        readings?.let { rows -> item {
            MassCard {
                MassSymbolLabel(MassGuideSymbolKind.Book, "Readings", 21, IlluminedThemeTokens.Ink)
                Text("The Church listens to the Word of God, responds in prayer, and stands to welcome Christ speaking in the Gospel.", fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.SecondaryText)
                rows.forEach { MassRowContent(it, onPrayer, compact = true) }
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MassGuideCatalog.dailyReadingsUrl))) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { MassGuideSymbol(MassGuideSymbolKind.ExternalLink, Color.White, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Open USCCB Daily Readings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        } }
        items(displayRows, key = { it.title }) { MassRowCard(it, onPrayer) }
        if (part.id == "liturgy-eucharist") item { EmbeddedCommunionCard(MassGuideCatalog.communionRite, onPrayer) }
        item {
            if (nextPart != null) Button(onClick = { onNext(nextPart) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { FormationGameSymbol(FormationGameSymbolKind.ArrowCircleFilled, Color.White, Modifier.size(19.dp), IlluminedThemeTokens.Blue); Spacer(Modifier.width(8.dp)); Text("Continue to ${nextPart.title}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            else MassCard { MassSymbolLabel(MassGuideSymbolKind.CheckSeal, "Mass Guide Complete", 20, IlluminedThemeTokens.Blue); Text("You have walked through the full movement of the Mass, from gathering to mission.", fontSize = 15.sp, color = IlluminedThemeTokens.SecondaryText) }
        }
    }
}

@Composable
private fun MassRowCard(row: MassGuideRow, onPrayer: (MassPrayerOption) -> Unit) = MassCard { MassRowContent(row, onPrayer) }

@Composable
private fun MassRowContent(row: MassGuideRow, onPrayer: (MassPrayerOption) -> Unit, compact: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(row.title, Modifier.weight(1f), fontSize = if (compact) 16.sp else 21.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
        row.posture?.let { Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue, modifier = Modifier.background(IlluminedThemeTokens.Blue.copy(.1f), RoundedCornerShape(30.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) }
    }
    Text(row.detail, fontSize = if (compact) 14.sp else 16.sp, lineHeight = if (compact) 21.sp else 24.sp, color = IlluminedThemeTokens.SecondaryText)
    row.response?.let { MassSymbolLabel(MassGuideSymbolKind.QuoteBubble, it, if (compact) 13 else 14, IlluminedThemeTokens.Gold, if (compact) 14 else 15) }
    row.prayerIds.mapNotNull(MassGuideCatalog.prayersById::get).forEach { option ->
        Surface(onClick = { onPrayer(option) }, shape = RoundedCornerShape(if (compact) 12.dp else 14.dp), color = Color.White.copy(if (compact) .78f else .72f), border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(if (compact) .14f else .16f))) {
            Row(Modifier.fillMaxWidth().padding(if (compact) 9.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(if (compact) 30.dp else 32.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) { MassGuideSymbol(massPrayerSymbol(option.id), IlluminedThemeTokens.Gold, Modifier.size(if (compact) 14.dp else 15.dp)) }
                Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(option.title, fontSize = if (compact) 14.sp else 15.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink); if (!compact) Text("Open prayer and guide text", fontSize = 12.sp, color = IlluminedThemeTokens.SecondaryText) }; LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(if (compact) 10.dp else 11.dp))
            }
        }
    }
    if (compact) HorizontalDivider(color = IlluminedThemeTokens.Gold.copy(.16f))
}

@Composable
private fun EmbeddedCommunionCard(part: MassGuidePart, onPrayer: (MassPrayerOption) -> Unit) = MassCard {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(38.dp).background(IlluminedThemeTokens.Blue.copy(.1f), CircleShape), contentAlignment = Alignment.Center) { MassGuideSymbol(massPartSymbol(part.id), IlluminedThemeTokens.Blue, Modifier.size(18.dp)) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(part.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(part.subtitle, fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText) }
    }
    Text(part.detail, fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.SecondaryText)
    part.rows.forEach { row -> Surface(shape = RoundedCornerShape(16.dp), color = IlluminedThemeTokens.Cream.copy(.58f), border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.14f))) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { MassRowContent(row, onPrayer, compact = true) } } }
}

@Composable
private fun MassPrayerPage(option: MassPrayerOption, onBack: () -> Unit) = MassList {
    item { TextButton(onClick = onBack) { Text("‹ Back") } }
    item { MassCard { MassSymbolLabel(massPrayerSymbol(option.id), option.title, 22, IlluminedThemeTokens.Blue); Text(option.summary, fontSize = 15.sp, lineHeight = 23.sp, color = IlluminedThemeTokens.SecondaryText); option.note?.let { Text(it, fontSize = 13.sp, color = IlluminedThemeTokens.Gold) } } }
    item { MassCard { Text(option.textHeading, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(option.fullText, fontSize = 18.sp, lineHeight = 28.sp, color = IlluminedThemeTokens.Ink); option.textNote?.let { Text(it, fontSize = 13.sp, lineHeight = 20.sp, color = IlluminedThemeTokens.SecondaryText) } } }
}

@Composable private fun MassNumber(value: String) { Box(Modifier.size(38.dp).background(IlluminedThemeTokens.Blue, CircleShape), contentAlignment = Alignment.Center) { Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White) } }
@Composable private fun MassSymbolLabel(kind: MassGuideSymbolKind, text: String, textSize: Int, color: Color, iconSize: Int = textSize) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { MassGuideSymbol(kind, color, Modifier.size(iconSize.dp)); Text(text, fontSize = textSize.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f, fill = false)) } }
@Composable private fun MassCard(content: @Composable ColumnScope.() -> Unit) { Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) } }
@Composable private fun MassList(bottomPadding: Int = 16, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) { LazyColumn(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomPadding.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
