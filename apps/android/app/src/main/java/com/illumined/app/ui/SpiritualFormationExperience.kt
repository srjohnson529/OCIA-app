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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.illumined.app.R
import com.illumined.app.data.DailyFormationEntry
import com.illumined.app.data.FormationRepository
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens
import org.json.JSONObject

private data class FormationPrayer(
    val id: String,
    val title: String,
    val text: String,
    val titleEs: String = "",
    val textEs: String = "",
) {
    val localizedTitle: String
        get() = if (java.util.Locale.getDefault().language == "es" && titleEs.isNotBlank()) titleEs else title

    val localizedText: String
        get() = if (java.util.Locale.getDefault().language == "es" && textEs.isNotBlank()) textEs else text
}
private data class FormationHtml(val title: String, val html: String, val titleEs: String = "", val htmlEs: String = "") {
    val localizedTitle get() = if (java.util.Locale.getDefault().language == "es" && titleEs.isNotBlank()) titleEs else title
    val localizedHtml get() = if (java.util.Locale.getDefault().language == "es" && htmlEs.isNotBlank()) htmlEs else html
}
internal data class RosaryPrayers(
    val signOfTheCross:String,val apostlesCreed:String,val ourFather:String,val hailMary:String,
    val gloryBe:String,val fatimaPrayer:String,val hailHolyQueen:String,val concludingPrayer:String,
    val signOfTheCrossEs:String="",val apostlesCreedEs:String="",val ourFatherEs:String="",val hailMaryEs:String="",
    val gloryBeEs:String="",val fatimaPrayerEs:String="",val hailHolyQueenEs:String="",val concludingPrayerEs:String="",
) {
    private fun localized(english:String, spanish:String) = if (java.util.Locale.getDefault().language == "es" && spanish.isNotBlank()) spanish else english
    val localizedSignOfTheCross get()=localized(signOfTheCross,signOfTheCrossEs)
    val localizedApostlesCreed get()=localized(apostlesCreed,apostlesCreedEs)
    val localizedOurFather get()=localized(ourFather,ourFatherEs)
    val localizedHailMary get()=localized(hailMary,hailMaryEs)
    val localizedGloryBe get()=localized(gloryBe,gloryBeEs)
    val localizedFatimaPrayer get()=localized(fatimaPrayer,fatimaPrayerEs)
    val localizedHailHolyQueen get()=localized(hailHolyQueen,hailHolyQueenEs)
    val localizedConcludingPrayer get()=localized(concludingPrayer,concludingPrayerEs)
}
internal data class RosaryMystery(val title:String,val scripture:String,val titleEs:String="",val scriptureEs:String="") {
    val localizedTitle get()=if(java.util.Locale.getDefault().language=="es"&&titleEs.isNotBlank()) titleEs else title
    val localizedScripture get()=if(java.util.Locale.getDefault().language=="es"&&scriptureEs.isNotBlank()) scriptureEs else scripture
}
internal data class RosarySet(val id: String, val title: String, val descriptionHtml: String, val mysteries:List<RosaryMystery>,val titleEs:String="",val descriptionHtmlEs:String="") {
    val localizedTitle get()=if(java.util.Locale.getDefault().language=="es"&&titleEs.isNotBlank()) titleEs else title
    val localizedDescriptionHtml get()=if(java.util.Locale.getDefault().language=="es"&&descriptionHtmlEs.isNotBlank()) descriptionHtmlEs else descriptionHtml
}
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

internal data class BreviaryLink(val title: String, val subtitle: String, val symbol: String, val url: String, val titleEs: String = "", val subtitleEs: String = "") {
    val localizedTitle get() = if(java.util.Locale.getDefault().language=="es" && titleEs.isNotBlank()) titleEs else title
    val localizedSubtitle get() = if(java.util.Locale.getDefault().language=="es" && subtitleEs.isNotBlank()) subtitleEs else subtitle
}

internal val breviaryLinks = listOf(
    BreviaryLink("iBreviary", "Full daily breviary with all hours", "book.closed", "https://www.ibreviary.com/m2/breviario.php", "iBreviary", "Breviario diario completo con todas las horas"),
    BreviaryLink("Office of Readings", "Longer readings and psalmody", "text.book.closed", "https://www.ibreviary.com/m2/breviario.php?s=ufficio_delle_letture", "Oficio de Lecturas", "Lecturas más extensas y salmodia"),
    BreviaryLink("Morning Prayer", "Lauds for today", "sunrise", "https://www.ibreviary.com/m2/breviario.php?s=lodi", "Laudes", "Oración de la mañana de hoy"),
    BreviaryLink("Daytime Prayer", "Midday prayer from the daily office", "sun.max", "https://www.ibreviary.com/m2/breviario.php?s=ora_media", "Hora intermedia", "Oración del oficio para el mediodía"),
    BreviaryLink("Evening Prayer", "Vespers for today", "sunset", "https://www.ibreviary.com/m2/breviario.php?s=vespri", "Vísperas", "Oración de la tarde de hoy"),
    BreviaryLink("Night Prayer", "Compline before rest", "moon.stars", "https://www.ibreviary.com/m2/breviario.php?s=compieta", "Completas", "Oración antes del descanso nocturno"),
    BreviaryLink("Divine Office Audio", "Pray with audio and spoken office", "speaker.wave.2", "https://divineoffice.org/", "Oficio Divino en audio", "Reza con audio y el oficio recitado"),
    BreviaryLink("Sing the Hours", "Chanted Liturgy of the Hours on YouTube", "music.note.tv", "https://www.youtube.com/@SingtheHours/videos", "Cantar las Horas", "Liturgia de las Horas cantada en YouTube"),
)

private data class FormationMenuRow(val title: String, val subtitle: String, val symbol: SpiritualFormationSymbolKind, val action: () -> Unit)

@Composable
fun SpiritualFormationExperience(
    profile: UserProfile?,
    memorizedPrayerIds: Set<String>,
    selectedPrayerIds: Set<String>,
    completedMysteryIds: Set<String>,
    onSetPrayerMemorized: (String, Boolean, () -> Unit, () -> Unit) -> Unit,
    onSetPrayerSelected: (String, Boolean, () -> Unit, () -> Unit) -> Unit,
    onCompleteMystery: (String, () -> Unit, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val result = remember { runCatching { loadFormationCatalog(context.resources.openRawResource(R.raw.spiritual_formation).bufferedReader().use { it.readText() }) } }
    var route by rememberSaveable { mutableStateOf(FormationRoute.MENU) }
    val tour = LocalInstructorWalkthrough.current
    LaunchedEffect(tour?.screen, tour?.active) { if(tour?.active == true && tour.screen == "formation") route = FormationRoute.MENU }
    val catalog = result.getOrNull()
    if (catalog == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(examT("Formation unavailable", "Formación no disponible"), color = Color.Red)
        }
        return
    }

    val destination = FormationRoute.parse(route)
    val visibleSelectedIds = SelectedPrayerPresentation.orderedVisibleIds(catalog.prayers.map { it.id }, selectedPrayerIds).toSet()
    val selectedPrayers = catalog.prayers.filter { it.id in visibleSelectedIds }
    BackHandler(enabled = destination.kind != FormationRoute.MENU) {
        route = destination.back
    }
    when (destination.kind) {
        FormationRoute.MENU -> FormationMenu(
            profile = profile,
            onPrayers = { route = FormationRoute.PRAYER_HUB },
            onExamination = { route = FormationRoute.EXAMINATION },
            onMass = { route = FormationRoute.MASS_GUIDE },
            onPractices = { route = FormationRoute.PRACTICES },
        )
        FormationRoute.PRAYER_HUB -> FormationList(examT("Prayer", "Oración"), { route = FormationRoute.MENU }, buildList {
            add(FormationMenuRow(examT("Common Prayers", "Oraciones comunes"), examT("${catalog.prayers.size} prayers", "${catalog.prayers.size} oraciones"), SpiritualFormationSymbolKind.Book) { route = FormationRoute.COMMON_PRAYERS })
            add(FormationMenuRow(if(java.util.Locale.getDefault().language=="es") "Rosario guiado" else "Guided Rosary", if(java.util.Locale.getDefault().language=="es") "Reza los misterios paso a paso" else "Pray the mysteries step by step", SpiritualFormationSymbolKind.RosaryGrid) { route = FormationRoute.ROSARY })
            add(FormationMenuRow(if(java.util.Locale.getDefault().language=="es") "Lectio Divina guiada" else "Guided Lectio Divina", if(java.util.Locale.getDefault().language=="es") "Lee, medita, ora y contempla" else "Read, meditate, pray, contemplate", SpiritualFormationSymbolKind.TextBook) { route = FormationRoute.detail(FormationRoute.HTML, "lectio", FormationRoute.PRAYER_HUB) })
            add(FormationMenuRow(if(java.util.Locale.getDefault().language=="es") "Liturgia de las Horas" else "Liturgy of the Hours", if(java.util.Locale.getDefault().language=="es") "La oración diaria de la Iglesia" else "The daily prayer of the Church", SpiritualFormationSymbolKind.Clock) { route = FormationRoute.detail(FormationRoute.HTML, "hours", FormationRoute.PRAYER_HUB) })
            if (selectedPrayers.isNotEmpty()) {
                add(FormationMenuRow(examT("Selected Prayers", "Oraciones seleccionadas"), examT("${selectedPrayers.size} saved for easy access", "${selectedPrayers.size} guardadas para acceso rápido"), SpiritualFormationSymbolKind.Bookmark) { route = FormationRoute.SELECTED_PRAYERS })
            }
        })
        FormationRoute.COMMON_PRAYERS -> FormationCards(examT("Common Prayers", "Oraciones comunes"), { route = FormationRoute.PRAYER_HUB }) {
            items(catalog.prayers, key = { it.id }) { prayer ->
                CommonPrayerCard(prayer.localizedTitle, prayer.id in memorizedPrayerIds, prayer.id in selectedPrayerIds) {
                    route = FormationRoute.detail(FormationRoute.PRAYER, prayer.id, FormationRoute.COMMON_PRAYERS)
                }
            }
        }
        FormationRoute.SELECTED_PRAYERS -> FormationCards(examT("Selected Prayers", "Oraciones seleccionadas"), { route = FormationRoute.PRAYER_HUB }) {
            if (selectedPrayers.isEmpty()) {
                item { SelectedPrayersEmptyCard() }
            } else {
                items(selectedPrayers, key = { it.id }) { prayer ->
                    SelectedPrayerCard(
                        prayer = prayer,
                        onOpen = { route = FormationRoute.detail(FormationRoute.PRAYER, prayer.id, FormationRoute.SELECTED_PRAYERS) },
                        onRemove = onSetPrayerSelected,
                    )
                }
            }
        }
        FormationRoute.ROSARY -> FormationCards(if(java.util.Locale.getDefault().language=="es") "Rosario guiado" else "Guided Rosary", { route = FormationRoute.PRAYER_HUB }) {
            items(catalog.mysteries, key = { it.title }) { mystery ->
                FormationMenuCard(mystery.localizedTitle, if(java.util.Locale.getDefault().language=="es") (if (mystery.id in completedMysteryIds) "Completado" else "Cinco misterios y reflexiones bíblicas") else (if (mystery.id in completedMysteryIds) "Completed" else "Five mysteries and Scripture reflections"), SpiritualFormationSymbolKind.RosaryGrid) { route = FormationRoute.detail(FormationRoute.MYSTERY, mystery.id, FormationRoute.ROSARY) }
            }
        }
        FormationRoute.PRACTICES -> FormationCards(examT("Spiritual Practices", "Prácticas espirituales"), { route = FormationRoute.MENU }) {
            itemsIndexed(catalog.practices, key = { _, practice -> practice.title }) { index, practice ->
                FormationMenuCard(practice.localizedTitle, examT("Catholic habits and faithful living", "Hábitos católicos y vida fiel"), SpiritualFormationSymbolKind.Walking) { route = FormationRoute.detail(FormationRoute.HTML, "practice-$index", FormationRoute.PRACTICES) }
            }
        }
        FormationRoute.EXAMINATION -> ExaminationExperience { route = FormationRoute.MENU }
        FormationRoute.MASS_GUIDE -> MassGuideExperience { route = FormationRoute.MENU }
        FormationRoute.PRAYER -> catalog.prayers.firstOrNull { it.id == destination.id }?.let { prayer ->
            PrayerDetail(
                prayer = prayer,
                memorized = prayer.id in memorizedPrayerIds,
                selected = prayer.id in selectedPrayerIds,
                onBack = { route = destination.back },
                onSetPrayerMemorized = onSetPrayerMemorized,
                onSetPrayerSelected = onSetPrayerSelected,
            )
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
            section?.let {
                if (destination.id.startsWith("practice-")) SpiritualPracticeReader(it, onBack = { route = destination.back })
                else HtmlFormationPage(it, destination.id == "lectio", onBack = { route = destination.back })
            }
                ?: LaunchedEffect(route) { route = destination.back }
        }
        FormationRoute.MYSTERY -> catalog.mysteries.firstOrNull { it.id == destination.id }?.let { mystery ->
            RosaryMysteryPage(
                mystery = mystery,
                completed = mystery.id in completedMysteryIds,
                onBack = { route = destination.back },
                onRosaryCompleted = { route = FormationRoute.PRAYER_HUB },
                onComplete = onCompleteMystery,
            )
        } ?: LaunchedEffect(route) { route = FormationRoute.ROSARY }
        else -> LaunchedEffect(route) { route = FormationRoute.MENU }
    }
}

private enum class ExaminationStage { HUB, INTRO, PRAYER, CHECKLIST, SUMMARY, DAILY_METHODS, DAILY_DETAIL }
private fun examT(english:String, spanish:String)=if(java.util.Locale.getDefault().language=="es") spanish else english

private data class DailyExamenMethod(
    val id: String,
    val title: String,
    val titleEs: String,
    val subtitle: String,
    val subtitleEs: String,
    val introduction: String,
    val introductionEs: String,
    val steps: List<String>,
    val stepsEs: List<String>,
    val closingPrayer: String,
    val closingPrayerEs: String,
) {
    val localizedTitle get() = examT(title, titleEs)
    val localizedSubtitle get() = examT(subtitle, subtitleEs)
    val localizedIntroduction get() = examT(introduction, introductionEs)
    val localizedSteps get() = if (java.util.Locale.getDefault().language == "es") stepsEs else steps
    val localizedClosingPrayer get() = examT(closingPrayer, closingPrayerEs)
}

private object DailyExamenCatalog {
    val methods = listOf(
        DailyExamenMethod(
            "ignatian",
            "Ignatian Examen",
            "Examen ignaciano",
            "Gratitude, light, review, mercy, and grace for tomorrow",
            "Gratitud, luz, revisión, misericordia y gracia para mañana",
            "Pray slowly through the day in God’s presence. Notice not only failures, but also where God was near and how grace was moving.",
            "Recorre lentamente el día en oración ante la presencia de Dios. Observa no solo las faltas, sino también dónde estuvo Dios cerca y cómo actuó la gracia.",
            listOf(
                "Become aware of God’s presence and rest quietly before Him.",
                "Give thanks for the gifts of this day, naming particular people, moments, and graces.",
                "Ask the Holy Spirit for light to see the day truthfully and with God’s compassion.",
                "Review the day from beginning to end. Notice consolation, resistance, choices, feelings, and invitations from God.",
                "Ask forgiveness where needed, receive God’s mercy, and ask for the grace you need tomorrow.",
            ),
            listOf(
                "Hazte consciente de la presencia de Dios y descansa en silencio ante Él.",
                "Da gracias por los dones de este día, nombrando personas, momentos y gracias concretas.",
                "Pide al Espíritu Santo luz para ver el día con verdad y con la compasión de Dios.",
                "Repasa el día de principio a fin. Observa la consolación, la resistencia, las decisiones, los sentimientos y las invitaciones de Dios.",
                "Pide perdón donde sea necesario, recibe la misericordia de Dios y pide la gracia que necesitas para mañana.",
            ),
            "Lord, thank You for remaining with me through this day. Show me how to receive tomorrow as Your gift and to respond more freely to Your grace. Amen.",
            "Señor, gracias por permanecer conmigo durante este día. Muéstrame cómo recibir el mañana como don tuyo y responder con mayor libertad a tu gracia. Amén.",
        ),
        DailyExamenMethod(
            "francis-de-sales",
            "St. Francis de Sales Evening Examen",
            "Examen vespertino de san Francisco de Sales",
            "A gentle review before rest from Introduction to the Devout Life",
            "Una revisión serena antes del descanso, inspirada en Introducción a la vida devota",
            "St. Francis de Sales recommends recollecting yourself before Christ and closing the day with gratitude, honest review, pardon, and trust.",
            "San Francisco de Sales recomienda recogerse ante Cristo y concluir el día con gratitud, revisión sincera, perdón y confianza.",
            listOf(
                "Place yourself in the presence of Christ and briefly renew a grace or holy desire from your morning prayer.",
                "Thank God for preserving you and accompanying you throughout the day.",
                "Recall where you were, whom you met, and what you did. Review your conduct with simplicity and honesty.",
                "Thank God for whatever was good. Ask pardon for faults in thought, word, deed, or omission, and resolve with grace to do better.",
                "Commend your body and soul, the Church, your family, friends, and all in need to God before resting.",
            ),
            listOf(
                "Ponte en la presencia de Cristo y renueva brevemente una gracia o un deseo santo de tu oración de la mañana.",
                "Da gracias a Dios por haberte guardado y acompañado durante todo el día.",
                "Recuerda dónde estuviste, con quién te encontraste y qué hiciste. Revisa tu conducta con sencillez y sinceridad.",
                "Da gracias a Dios por todo lo bueno. Pide perdón por las faltas de pensamiento, palabra, obra u omisión y propón, con su gracia, obrar mejor.",
                "Antes de descansar, encomienda a Dios tu cuerpo y tu alma, la Iglesia, tu familia, tus amigos y todos los necesitados.",
            ),
            "Jesus, receive all that this day has held. Forgive my faults, strengthen every good desire, and keep me and those I love in Your peace. Amen.",
            "Jesús, recibe todo lo que ha contenido este día. Perdona mis faltas, fortalece todo buen deseo y guarda en tu paz a quienes amo y a mí. Amén.",
        ),
        DailyExamenMethod(
            "benedictine",
            "Benedictine Daily Review",
            "Revisión diaria benedictina",
            "Listen for God through prayer, work, relationships, and humility",
            "Escucha a Dios en la oración, el trabajo, las relaciones y la humildad",
            "Inspired by the Benedictine call to continual conversion, this review listens for God in the ordinary rhythm of the day.",
            "Inspirada en la llamada benedictina a la conversión continua, esta revisión escucha a Dios en el ritmo ordinario del día.",
            listOf(
                "Be still before God and listen: what word, event, or person is He bringing to mind?",
                "Give thanks for the day’s prayer, work, rest, and encounters.",
                "Review how you practiced humility, patience, obedience, hospitality, and care for others.",
                "Notice where self-will, distraction, resentment, or excess disturbed peace and charity.",
                "Choose one small act of conversion for tomorrow and entrust it to God’s help.",
            ),
            listOf(
                "Permanece en silencio ante Dios y escucha: ¿qué palabra, acontecimiento o persona trae Él a tu memoria?",
                "Da gracias por la oración, el trabajo, el descanso y los encuentros del día.",
                "Revisa cómo practicaste la humildad, la paciencia, la obediencia, la hospitalidad y el cuidado de los demás.",
                "Observa dónde la voluntad propia, la distracción, el resentimiento o el exceso perturbaron la paz y la caridad.",
                "Elige un pequeño acto de conversión para mañana y confíalo a la ayuda de Dios.",
            ),
            "God of peace, gather my work and rest into Your love. Teach me to listen, begin again, and seek You faithfully in the ordinary duties of tomorrow. Amen.",
            "Dios de paz, acoge mi trabajo y mi descanso en tu amor. Enséñame a escuchar, comenzar de nuevo y buscarte fielmente en los deberes ordinarios de mañana. Amén.",
        ),
        DailyExamenMethod(
            "gospel-love",
            "Gospel Examen of Love",
            "Examen evangélico del amor",
            "Review the day through love of God and neighbor",
            "Revisa el día desde el amor a Dios y al prójimo",
            "Let Jesus’ two great commandments provide a simple lens for seeing the day and choosing a concrete response of love.",
            "Deja que los dos grandes mandamientos de Jesús te ofrezcan una mirada sencilla para contemplar el día y elegir una respuesta concreta de amor.",
            listOf(
                "Thank God for one moment in which you received or gave love today.",
                "Where did you love God with your attention, trust, prayer, or choices?",
                "Where did you love your neighbor through patience, truth, mercy, generosity, or service?",
                "Where did you withhold love or fail to recognize another person’s dignity? Ask for mercy without discouragement.",
                "Choose one specific way to love God or neighbor tomorrow, and ask for the grace to follow through.",
            ),
            listOf(
                "Da gracias a Dios por un momento en el que hoy recibiste o diste amor.",
                "¿Dónde amaste a Dios con tu atención, confianza, oración o decisiones?",
                "¿Dónde amaste al prójimo mediante la paciencia, la verdad, la misericordia, la generosidad o el servicio?",
                "¿Dónde negaste amor o no reconociste la dignidad de otra persona? Pide misericordia sin desanimarte.",
                "Elige una manera concreta de amar mañana a Dios o al prójimo y pide la gracia de llevarla a cabo.",
            ),
            "Jesus, form my heart after Your own. Heal what was lacking in love today and make me attentive, courageous, and generous tomorrow. Amen.",
            "Jesús, forma mi corazón según el tuyo. Sana lo que hoy faltó al amor y hazme atento, valiente y generoso mañana. Amén.",
        ),
    )
}

@Composable
private fun ExaminationExperience(onExit:()->Unit){
    var stage by remember { mutableStateOf(ExaminationStage.HUB) }
    var selectedDailyMethodId by remember { mutableStateOf<String?>(null) }
    var checked by remember { mutableStateOf(emptySet<String>()) }
    val checkedItems=ExaminationCatalog.sections.flatMapIndexed{sectionIndex,section->section.localizedItems.mapIndexed{itemIndex,text->"$sectionIndex-$itemIndex" to text}}.filter{it.first in checked}.map{it.second}
    BackHandler {
        stage = when (stage) {
            ExaminationStage.HUB -> { onExit(); ExaminationStage.HUB }
            ExaminationStage.INTRO -> ExaminationStage.HUB
            ExaminationStage.PRAYER -> ExaminationStage.INTRO
            ExaminationStage.CHECKLIST -> ExaminationStage.PRAYER
            ExaminationStage.SUMMARY -> ExaminationStage.CHECKLIST
            ExaminationStage.DAILY_METHODS -> ExaminationStage.HUB
            ExaminationStage.DAILY_DETAIL -> ExaminationStage.DAILY_METHODS
        }
    }
    when(stage){
        ExaminationStage.HUB -> ExaminationHubPage(
            onBack = onExit,
            onReconciliation = { stage = ExaminationStage.INTRO },
            onDailyExamen = { stage = ExaminationStage.DAILY_METHODS },
        )
        ExaminationStage.INTRO -> ExaminationIntroPage({ stage = ExaminationStage.HUB }) { stage = ExaminationStage.PRAYER }
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
        ExaminationStage.DAILY_METHODS -> DailyExamenMethodsPage(
            onBack = { stage = ExaminationStage.HUB },
            onSelect = { method -> selectedDailyMethodId = method.id; stage = ExaminationStage.DAILY_DETAIL },
        )
        ExaminationStage.DAILY_DETAIL -> DailyExamenCatalog.methods.firstOrNull { it.id == selectedDailyMethodId }?.let { method ->
            DailyExamenDetailPage(method, onBack = { stage = ExaminationStage.DAILY_METHODS })
        } ?: LaunchedEffect(selectedDailyMethodId) { stage = ExaminationStage.DAILY_METHODS }
    }
}

@Composable
private fun ExaminationHubPage(onBack: () -> Unit, onReconciliation: () -> Unit, onDailyExamen: () -> Unit) =
    FormationCards(examT("Examination of Conscience","Examen de conciencia"), onBack) {
        item {
            FormationMenuCard(
                examT("Daily Examen","Examen diario"),
                examT("Four prayerful ways to review the day with God","Cuatro maneras orantes de revisar el día con Dios"),
                SpiritualFormationSymbolKind.MoonStars,
                onDailyExamen,
            )
        }
        item {
            FormationMenuCard(
                examT("Preparation for Reconciliation","Preparación para la Reconciliación"),
                examT("A thorough, private examination before Confession","Un examen privado y completo antes de la Confesión"),
                SpiritualFormationSymbolKind.Checklist,
                onReconciliation,
            )
        }
    }

@Composable
private fun DailyExamenMethodsPage(onBack: () -> Unit, onSelect: (DailyExamenMethod) -> Unit) =
    FormationCards(examT("Daily Examen","Examen diario"), onBack) {
        item {
            ExaminationCard {
                Text(examT("Review Your Day with God","Revisa tu día con Dios"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                Text(examT("Choose a method and spend a few quiet minutes in gratitude, discernment, mercy, and growth. A daily examen is not a replacement for sacramental Confession.","Elige un método y dedica unos minutos de silencio a la gratitud, el discernimiento, la misericordia y el crecimiento. El examen diario no sustituye la Confesión sacramental."), fontSize = 15.sp, lineHeight = 23.sp, color = IlluminedThemeTokens.SecondaryText)
            }
        }
        items(DailyExamenCatalog.methods, key = { it.id }) { method ->
            FormationMenuCard(method.localizedTitle, method.localizedSubtitle, SpiritualFormationSymbolKind.Search) { onSelect(method) }
        }
    }

@Composable
private fun DailyExamenDetailPage(method: DailyExamenMethod, onBack: () -> Unit) {
    val accent = Color(0xFFEFD08A)
    val entrance = remember(method.id) { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(method.id) { entrance.animateTo(0f, androidx.compose.animation.core.tween(300)) }
    Box(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue))
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onBack,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue)) {
            Column(
                Modifier.fillMaxSize().offset(y = maxHeight * entrance.value)
                    .systemBarsPadding().verticalScroll(rememberScrollState()).padding(30.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Text(examT("DAILY EXAMEN", "EXAMEN DIARIO"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(method.localizedTitle, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(Modifier.width(90.dp).height(3.dp).background(accent))
                Text(method.localizedIntroduction, fontSize = 18.5.sp, lineHeight = 27.5.sp, color = Color.White)
                method.localizedSteps.forEachIndexed { index, step ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(examT("Step ${index + 1}", "Paso ${index + 1}"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = accent)
                        Text(step, fontSize = 18.5.sp, lineHeight = 27.5.sp, color = Color.White)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(examT("Closing Prayer", "Oración final"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = accent)
                    Text(method.localizedClosingPrayer, fontSize = 18.5.sp, lineHeight = 27.5.sp, color = Color.White)
                }
                Button(
                    onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                ) {
                    Text(examT("Close", "Cerrar"), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ExaminationIntroPage(onBack: () -> Unit, onBegin: () -> Unit) = FormationCards(null, null) {
    item { TextButton(onClick = onBack) { Text(examT("‹ Back", "‹ Atrás")) } }
    item { ExaminationCard {
        Text(examT("Examination of Conscience","Examen de conciencia"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
        ExaminationIntro(examT("I. What is an Examination of Conscience?","I. ¿Qué es un examen de conciencia?"), examT("An examination of conscience is a prayerful self-reflection on our thoughts, words, deeds, and omissions, measured against God’s commandments and the teaching of the Church. Its purpose is to recognize sins honestly, acknowledge God’s mercy, prepare for Confession, and form the conscience over time.","Un examen de conciencia es una reflexión orante sobre nuestros pensamientos, palabras, obras y omisiones a la luz de los mandamientos de Dios y de la enseñanza de la Iglesia. Nos ayuda a reconocer los pecados con sinceridad, acoger la misericordia de Dios, prepararnos para la Confesión y formar la conciencia."))
        ExaminationIntro(examT("II. Why is it Important?","II. ¿Por qué es importante?"), examT("A good confession requires that we know and confess our sins honestly. Regular examination also fosters humility, self-awareness, growth in holiness, and a better alignment of conscience with God’s will.","Una buena confesión requiere conocer y confesar nuestros pecados con sinceridad. El examen frecuente también fomenta la humildad, el conocimiento propio, el crecimiento en santidad y una conciencia más conforme con la voluntad de Dios."))
        ExaminationIntro(examT("III. When and How Often?","III. ¿Cuándo y con qué frecuencia?"), examT("A thorough examination should be done before sacramental confession. A brief daily examen can be prayed at the end of the day. A deeper examination can also be helpful before retreats, spiritual direction, or major decisions.","Conviene hacer un examen detenido antes de la Confesión sacramental. Al final del día puede rezarse un examen breve. Un examen más profundo también ayuda antes de retiros, dirección espiritual o decisiones importantes."))
        ExaminationIntro(examT("IV. Dispositions for a Good Examination","IV. Disposiciones para un buen examen"), examT("Begin prayerfully. Ask the Holy Spirit for light and honesty. Avoid self-justification. Call sins what they are. Keep hope in God’s mercy, avoid despair, and renew your desire to amend your life.","Comienza en oración y pide al Espíritu Santo luz y sinceridad. Evita justificarte y llama a los pecados por su nombre. Mantén la esperanza en la misericordia de Dios y renueva tu propósito de enmienda."))
    } }
    item { Button(onClick = onBegin, modifier = Modifier.fillMaxWidth().height(54.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.PlayCircle, Color.White, Modifier.size(19.dp), IlluminedThemeTokens.Blue); Spacer(Modifier.width(8.dp)); Text(examT("Begin Examination","Comenzar el examen"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) } }
    item { Text(examT("Private: your checked items are only kept on this screen while you pray. They are not saved, uploaded, or shared with your instructor.","Privado: los elementos marcados solo permanecen en esta pantalla mientras oras. No se guardan, no se suben ni se comparten con tu instructor."), color = IlluminedThemeTokens.SecondaryText, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
}

@Composable
private fun ExaminationPrayerPage(onBack: () -> Unit, onBegin: () -> Unit) = FormationCards(null, null) {
    item { TextButton(onClick = onBack) { Text(examT("‹ Back", "‹ Atrás")) } }
    item { ExaminationCard { Text(examT("Prayer Before Examination","Oración antes del examen"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(ExaminationCatalog.localizedPreExamPrayer, fontSize = 18.sp, lineHeight = 28.sp, color = IlluminedThemeTokens.Ink) } }
    item { ExaminationCard { Text(examT("Examination of Conscience","Examen de conciencia"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(examT("Move prayerfully through the commandments, the deadly sins, sins of omission, and final questions about love. Check only what helps you prepare honestly before God.","Recorre en oración los mandamientos, los pecados capitales, los pecados de omisión y las preguntas finales sobre el amor. Marca solo lo que te ayude a prepararte con sinceridad ante Dios."), fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.SecondaryText) } }
    item { Button(onClick = onBegin, modifier = Modifier.fillMaxWidth().height(54.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.Checklist, Color.White, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text(examT("Begin Checklist","Comenzar la lista"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) } }
}

@Composable
private fun ExaminationChecklistPage(checked: Set<String>, onToggle: (String) -> Unit, onBack: () -> Unit, onComplete: () -> Unit) = FormationCards(null, null) {
    item { TextButton(onClick = onBack) { Text(examT("‹ Back", "‹ Atrás")) } }
    item { ExaminationCard { Text(examT("Examination tool","Herramienta de examen"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(examT("Check the items that you prayerfully recognize. This list is private and disappears when you leave the examination.","Marca los elementos que reconoces en oración. Esta lista es privada y desaparece cuando sales del examen."), fontSize = 15.sp, lineHeight = 23.sp, color = IlluminedThemeTokens.SecondaryText) } }
    itemsIndexed(ExaminationCatalog.sections, key = { index, _ -> index }) { sectionIndex, section ->
        ExaminationCard {
            Text(section.localizedTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
            section.localizedItems.forEachIndexed { itemIndex, label ->
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
    item { Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(54.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.CheckSeal, Color.White, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text(examT("Complete Examination","Completar el examen"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) } }
}

@Composable
private fun ExaminationSummaryPage(checkedItems: List<String>, onBack: () -> Unit, onFinish: () -> Unit) = FormationCards(null, null) {
    item { TextButton(onClick = onBack) { Text(examT("‹ Back", "‹ Atrás")) } }
    item { ExaminationCard { Text(examT("Private Examination Summary","Resumen privado del examen"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(examT("Examination tool","Herramienta de examen"), fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.SecondaryText); Text(examT("Use this only for your own prayer and preparation. Nothing on this page is saved or shared.","Usa esto únicamente para tu oración y preparación personal. Nada de esta página se guarda ni se comparte."), fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText) } }
    item { ExaminationCard { Text(examT("Items Checked","Elementos marcados"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold); if (checkedItems.isEmpty()) Text(examT("No items were checked.","No se marcó ningún elemento."), color = IlluminedThemeTokens.SecondaryText) else checkedItems.forEach { Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(7.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.CheckCircle, IlluminedThemeTokens.Blue, Modifier.size(15.dp)); Text(it, Modifier.weight(1f), fontSize = 14.sp, color = IlluminedThemeTokens.Blue) } } } }
    item { ExaminationCard { Text(examT("Act of Contrition","Acto de contrición"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(ExaminationCatalog.localizedActOfContrition, fontSize = 18.sp, lineHeight = 28.sp, color = IlluminedThemeTokens.Ink) } }
    item { OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(examT("Finish and Clear Private Checklist","Finalizar y borrar la lista privada")) } }
}

@Composable private fun ExaminationCard(content:@Composable ColumnScope.()->Unit){Surface(shape=RoundedCornerShape(16.dp),color=Color.White.copy(.94f),shadowElevation=6.dp,border=androidx.compose.foundation.BorderStroke(1.dp,IlluminedThemeTokens.Gold.copy(.22f))){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}}
@Composable private fun ExaminationIntro(title:String,text:String){Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Text(title,fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=IlluminedThemeTokens.Ink);Text(text,fontSize=16.sp,lineHeight=24.sp,color=IlluminedThemeTokens.SecondaryText)}}

@Composable
private fun FormationMenu(profile: UserProfile?, onPrayers: () -> Unit, onExamination: () -> Unit, onMass: () -> Unit, onPractices: () -> Unit) {
    val repository = remember { FormationRepository() }
    var entry by remember { mutableStateOf<DailyFormationEntry?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val rows = listOf(
        FormationMenuRow(examT("Prayers", "Oraciones"), examT("Common prayers, rosary, lectio divina, and the hours", "Oraciones comunes, rosario, Lectio Divina y Liturgia de las Horas"), SpiritualFormationSymbolKind.Prayers, onPrayers),
        FormationMenuRow(examT("Examination of Conscience","Examen de conciencia"), examT("Prepare for Reconciliation or pray a daily examen","Prepárate para la Reconciliación o reza un examen diario"), SpiritualFormationSymbolKind.Search, onExamination),
        FormationMenuRow(examT("Guide to the Mass", "Guía de la Misa"), examT("Walk through the order, prayers, readings, and Eucharistic Prayer", "Recorre el orden, las oraciones, las lecturas y la Plegaria eucarística"), SpiritualFormationSymbolKind.Church, onMass),
        FormationMenuRow(examT("Spiritual Practices", "Prácticas espirituales"), examT("Works of mercy, precepts, habits, and Catholic living", "Obras de misericordia, preceptos, hábitos y vida católica"), SpiritualFormationSymbolKind.Walking, onPractices),
    )
    val tour = LocalInstructorWalkthrough.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val anchors = listOf("formation", "formation-examination", "formation-mass", "formation-practices", "formation-daily")
    LaunchedEffect(tour?.target) {
        if(tour?.active == true && tour.screen == "formation") {
            val index = if(tour.target == "formation-daily-open") 4 else anchors.indexOf(tour.target)
            if(index >= 0) listState.animateScrollToItem(index)
        }
    }
    FormationCards(null, null, listState) {
        items(rows) { row ->
            Box(Modifier.walkthroughAnchor(anchors[rows.indexOf(row)])) {
                FormationMenuCard(row.title, row.subtitle, row.symbol, row.action)
            }
        }
        if (profile != null) item {
            Surface(modifier = Modifier.walkthroughAnchor("formation-daily"), shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HomeSymbol(HomeSymbolKind.CalendarBadgeClock, IlluminedThemeTokens.Blue, Modifier.size(20.dp))
                        Text(examT("Daily Formation", "Formación diaria"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
                    }
                    Text(examT("Open today’s liturgical fact, saint, or note from your class.", "Abre el dato, santo o nota litúrgica de hoy para tu clase."), fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.Ink)
                    Button(onClick = {
                        repository.loadDailyFormation(profile, force = true, onSuccess = {
                            entry = it
                            status = if (it == null) examT("No Daily Formation card is published for today.", "No hay una tarjeta de Formación diaria publicada para hoy.") else null
                        }, onError = { status = examT("Today’s Daily Formation card could not be loaded.", "No se pudo cargar la tarjeta de Formación diaria de hoy.") })
                    }, modifier = Modifier.walkthroughAnchor("formation-daily-open").fillMaxWidth().height(54.dp)) {
                        MassGuideSymbol(MassGuideSymbolKind.ExternalLink, Color.White, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(examT("Open Today’s Card", "Abrir la tarjeta de hoy"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    status?.let { Text(it, color = IlluminedThemeTokens.SecondaryText, fontSize = 14.sp) }
                }
            }
        }
    }
    entry?.let { current ->
        DailyFormationDialog(current) {
            entry = null
            profile?.let { repository.dismissDailyFormation(it, current) }
        }
    }
}

@Composable
private fun FormationList(title: String?, onBack: (() -> Unit)?, rows: List<FormationMenuRow>) {
    FormationCards(title, onBack) { items(rows) { row -> FormationMenuCard(row.title, row.subtitle, row.symbol, row.action) } }
}

@Composable
private fun FormationCards(title: String?, onBack: (() -> Unit)?, listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(), blueTheme: Boolean = false, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    val background = if (blueTheme) Brush.linearGradient(listOf(IlluminedThemeTokens.Blue, IlluminedThemeTokens.Blue)) else formationBrush()
    LazyColumn(Modifier.fillMaxSize().background(background), state = listState, contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (onBack != null || title != null) item {
            if (onBack != null) TextButton(onClick = onBack) { Text(examT("‹ Back", "‹ Volver"), color = if (blueTheme) Color(0xFFEFD08A) else IlluminedThemeTokens.Blue) }
            if (title != null) Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = if (blueTheme) Color.White else IlluminedThemeTokens.Ink)
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
private fun CommonPrayerCard(title: String, memorized: Boolean, selected: Boolean, onClick: () -> Unit) {
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
                if (memorized || selected) {
                    Text(
                        listOfNotNull(if (selected) examT("Selected", "Seleccionada") else null, if (memorized) examT("Memorized", "Memorizada") else null).joinToString(" • "),
                        fontSize = 13.sp,
                        color = IlluminedThemeTokens.SecondaryText,
                    )
                }
            }
            LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
        }
    }
}

@Composable
private fun SelectedPrayersEmptyCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(.94f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f)),
    ) {
        Text(
            examT("No prayers are selected. Return to Common Prayers to add one.", "No hay oraciones seleccionadas. Vuelve a Oraciones comunes para agregar una."),
            Modifier.fillMaxWidth().padding(20.dp),
            color = IlluminedThemeTokens.SecondaryText,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SelectedPrayerCard(
    prayer: FormationPrayer,
    onOpen: () -> Unit,
    onRemove: (String, Boolean, () -> Unit, () -> Unit) -> Unit,
) {
    var removing by remember(prayer.id) { mutableStateOf(false) }
    var failed by remember(prayer.id) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(.94f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f)),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clickable(enabled = !removing, onClick = onOpen).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).background(IlluminedThemeTokens.Gold.copy(.12f), CircleShape), contentAlignment = Alignment.Center) {
                    SpiritualFormationSymbol(SpiritualFormationSymbolKind.Bookmark, IlluminedThemeTokens.Gold, Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(prayer.localizedTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                    if (failed) Text(examT("Could not remove prayer", "No se pudo quitar la oración"), fontSize = 12.sp, color = Color.Red)
                }
                LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
            }
            VerticalDivider(Modifier.height(34.dp))
            IconButton(
                onClick = {
                    removing = true
                    failed = false
                    onRemove(prayer.id, false, { removing = false }, { removing = false; failed = true })
                },
                enabled = !removing,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .semantics { contentDescription = examT("Remove ${prayer.localizedTitle} from Selected Prayers", "Quitar ${prayer.localizedTitle} de las oraciones seleccionadas") },
            ) {
                SpiritualFormationSymbol(SpiritualFormationSymbolKind.Bookmark, Color.Red, Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun PrayerDetail(
    prayer: FormationPrayer,
    memorized: Boolean,
    selected: Boolean,
    onBack: () -> Unit,
    onSetPrayerMemorized: (String, Boolean, () -> Unit, () -> Unit) -> Unit,
    onSetPrayerSelected: (String, Boolean, () -> Unit, () -> Unit) -> Unit,
) {
    var saving by remember(prayer.id) { mutableStateOf(false) }
    var saveFailed by remember(prayer.id) { mutableStateOf(false) }
    val accent = Color(0xFFEFD08A)
    val entrance = remember(prayer.id) { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(prayer.id) { entrance.animateTo(0f, androidx.compose.animation.core.tween(300)) }
    Box(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue))
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onBack,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue), contentAlignment = Alignment.TopCenter) {
            val availableHeight = maxHeight
            Surface(
                modifier = Modifier.fillMaxSize().offset(y = availableHeight * entrance.value),
                shape = androidx.compose.ui.graphics.RectangleShape,
                color = IlluminedThemeTokens.Blue,
                contentColor = Color.White,
            ) {
                Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(6.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(examT("COMMON PRAYERS", "ORACIONES COMUNES"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(prayer.localizedTitle, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.width(90.dp).height(3.dp).background(accent))
                    }
                    Text(
                        prayer.localizedText,
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        fontSize = 18.5.sp, lineHeight = 27.5.sp, color = Color.White,
                    )
                    Column(Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (saveFailed) Text(examT("Could not save. Please try again.", "No se pudo guardar. Inténtalo de nuevo."), color = Color.White)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { saving = true; saveFailed = false; onSetPrayerMemorized(prayer.id, !memorized, { saving = false }, { saving = false; saveFailed = true }) },
                                enabled = !saving, modifier = Modifier.weight(1f).heightIn(min = 70.dp).semantics { this.selected = memorized },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    if (memorized) FormationGameSymbol(FormationGameSymbolKind.CheckCircleFilled, accent, Modifier.size(24.dp))
                                    else SpiritualFormationSymbol(SpiritualFormationSymbolKind.CheckCircle, accent, Modifier.size(24.dp))
                                    Text(if (memorized) examT("Memorized", "Memorizada") else examT("Mark Memorized", "Marcar memorizada"), fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            TextButton(
                                onClick = { saving = true; saveFailed = false; onSetPrayerSelected(prayer.id, !selected, { saving = false }, { saving = false; saveFailed = true }) },
                                enabled = !saving, modifier = Modifier.weight(1f).heightIn(min = 70.dp).semantics { this.selected = selected },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    if (selected) {
                                        androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                                            val bookmark = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(size.width * .26f, size.height * .1f)
                                                lineTo(size.width * .74f, size.height * .1f)
                                                lineTo(size.width * .74f, size.height * .9f)
                                                lineTo(size.width * .5f, size.height * .72f)
                                                lineTo(size.width * .26f, size.height * .9f)
                                                close()
                                            }
                                            drawPath(bookmark, accent)
                                        }
                                    } else SpiritualFormationSymbol(SpiritualFormationSymbolKind.Bookmark, accent, Modifier.size(24.dp))
                                    Text(if (selected) examT("Saved", "Guardada") else examT("Save Prayer", "Guardar oración"), fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                        Button(onClick = onBack,
                            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                            Text(examT("Close", "Cerrar"), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpiritualPracticeReader(section: FormationHtml, onBack: () -> Unit) {
    val accent = Color(0xFFEFD08A)
    val entrance = remember(section.title) { androidx.compose.animation.core.Animatable(1f) }
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    val html = remember(section.localizedHtml, fontScale) {
        """<html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
        body{font-family:Roboto,Arial,sans-serif;color:white;font-size:${18.5f * fontScale}px;line-height:1.55;background:transparent;margin:0;padding:0 24px 20px;overflow-wrap:break-word}
        body>h2:first-of-type{display:none}.content-wrapper{background:transparent!important;padding:0!important}
        h1,h2,h3,h4,a{color:#efd08a!important}h3,h4{margin-top:24px}p,li{color:white}li{margin-bottom:8px}
        blockquote{background:transparent!important;color:white!important;border-left:3px solid #efd08a;padding-left:14px;margin-left:0}
        img,video,iframe{max-width:100%}
        </style></head><body>${section.localizedHtml}</body></html>"""
    }
    LaunchedEffect(section.title) { entrance.animateTo(0f, androidx.compose.animation.core.tween(300)) }
    Box(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue))
    androidx.compose.ui.window.Dialog(onDismissRequest = onBack,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(IlluminedThemeTokens.Blue)) {
            Column(Modifier.fillMaxSize().offset(y = maxHeight * entrance.value).systemBarsPadding()) {
                Column(Modifier.fillMaxWidth().padding(30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(examT("SPIRITUAL PRACTICES", "PRÁCTICAS ESPIRITUALES"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(section.localizedTitle, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Box(Modifier.width(90.dp).height(3.dp).background(accent))
                }
                AndroidView(modifier = Modifier.fillMaxWidth().weight(1f),
                    factory = { context -> WebView(context).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        webViewClient = WebViewClient()
                    } },
                    update = { view ->
                        if (view.tag != html) {
                            view.tag = html
                            view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        }
                    },
                    onRelease = { it.destroy() })
                Button(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 22.dp).heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                    Text(examT("Close", "Cerrar"), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun HtmlFormationPage(section: FormationHtml, showsDailyGospel: Boolean = false, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(formationBrush()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text(if (java.util.Locale.getDefault().language == "es") "‹ Atrás" else "‹ Back") }
        Text(section.localizedTitle, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context -> WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT); webViewClient = WebViewClient()
        } }, update = { it.loadDataWithBaseURL(null, styledHtml(section.localizedHtml), "text/html", "UTF-8", null) })
        if (showsDailyGospel) Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { HomeSymbol(HomeSymbolKind.CalendarBadgeClock, IlluminedThemeTokens.Blue, Modifier.size(20.dp)); Text(if(java.util.Locale.getDefault().language=="es") "Evangelio del día" else "Daily Gospel", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
                Text(if(java.util.Locale.getDefault().language=="es") "Usa el Evangelio de hoy como pasaje para la Lectio Divina. La página oficial de lecturas diarias de la USCCB se actualiza cada día con las lecturas del leccionario de la Iglesia." else "Use today's Gospel as the scripture passage for Lectio Divina. The official USCCB daily readings page updates each day with the Church's lectionary readings.", fontSize = 16.sp, lineHeight = 24.sp, color = IlluminedThemeTokens.Ink)
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MassGuideCatalog.dailyReadingsUrl))) }, modifier = Modifier.fillMaxWidth().height(54.dp)) { MassGuideSymbol(MassGuideSymbolKind.ExternalLink, Color.White, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if(java.util.Locale.getDefault().language=="es") "Abrir el Evangelio de hoy" else "Open Today's Gospel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
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
        item { TextButton(onClick = onBack) { Text(examT("‹ Back", "‹ Atrás")) } }
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { SpiritualFormationSymbol(SpiritualFormationSymbolKind.Link, IlluminedThemeTokens.Blue, Modifier.size(20.dp)); Text(if(java.util.Locale.getDefault().language=="es") "Abrir el breviario de hoy" else "Open Today's Breviary", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue) }
                    Text(if(java.util.Locale.getDefault().language=="es") "Usa estos enlaces para rezar la Liturgia de las Horas del día fuera de la aplicación. Las páginas se actualizan diariamente." else "Use these links to pray the current Liturgy of the Hours outside the app. The pages update daily.", fontSize = 15.sp, lineHeight = 22.sp, color = IlluminedThemeTokens.SecondaryText)
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
                                    Text(link.localizedTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Ink)
                                    Text(link.localizedSubtitle, fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText)
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
    onRosaryCompleted: () -> Unit,
    onComplete: (String, () -> Unit, () -> Unit) -> Unit,
) {
    var started by rememberSaveable(mystery.id) { mutableStateOf(false) }
    val catalogContext = LocalContext.current
    val catalog = remember { runCatching { loadFormationCatalog(catalogContext.resources.openRawResource(R.raw.spiritual_formation).bufferedReader().use { it.readText() }) }.getOrNull() }
    val sequence = remember(mystery.id, catalog) { catalog?.let { buildRosarySequence(it.rosaryPrayers,mystery) }.orEmpty() }
    if(started && sequence.isNotEmpty()) {
        GuidedRosaryPage(
            mysteryId = mystery.id,
            sequence = sequence,
            onBack = { started = false },
            onRosaryCompleted = onRosaryCompleted,
            onComplete = onComplete,
        )
        return
    }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(formationBrush()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text(examT("‹ Back", "‹ Atrás")) }
        Text(mystery.localizedTitle, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context -> WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT); webViewClient = WebViewClient()
        } }, update = { it.loadDataWithBaseURL(null, styledHtml(mystery.localizedDescriptionHtml), "text/html", "UTF-8", null) })
        if (error) Text(
            if (java.util.Locale.getDefault().language == "es") "No se pudo guardar tu progreso del Rosario." else "Your Rosary progress could not be saved.",
            color = Color.Red,
        )
        Button(
            onClick = { started=true },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(if(java.util.Locale.getDefault().language=="es") (if(completed) "Rezar de nuevo" else "Comenzar el Rosario") else (if(completed) "Pray Again" else "Start Rosary")) }
    }
}

@Composable
private fun GuidedRosaryPage(
    mysteryId: String,
    sequence: List<RosaryStep>,
    onBack: () -> Unit,
    onRosaryCompleted: () -> Unit,
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
                { saving = false; index = 0; onRosaryCompleted() },
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
            Text(examT("‹ Back", "‹ Atrás"))
        }

        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / sequence.size.toFloat() },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = examT(
                    "Rosary progress, step ${index + 1} of ${sequence.size}",
                    "Progreso del Rosario, paso ${index + 1} de ${sequence.size}",
                )
            },
            color = IlluminedThemeTokens.Gold,
        )

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
                    }
                }
                }
            }
        }

        if (error) Text(
            if (java.util.Locale.getDefault().language == "es") "No se pudo guardar tu progreso del Rosario." else "Your Rosary progress could not be saved.",
            color = Color.Red,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { index = (index - 1).coerceAtLeast(0) }, enabled = index > 0 && !saving) {
                Text(if (java.util.Locale.getDefault().language == "es") "Atrás" else "Back")
            }
        }
    }
}

internal fun buildRosarySequence(prayers:RosaryPrayers,set:RosarySet):List<RosaryStep> = buildList {
    val es=java.util.Locale.getDefault().language=="es"
    fun t(en:String,spanish:String)=if(es) spanish else en
    add(RosaryStep(t("Sign of the Cross","La señal de la cruz"),prayers.localizedSignOfTheCross));add(RosaryStep(t("Apostles' Creed","Credo de los Apóstoles"),prayers.localizedApostlesCreed));add(RosaryStep(t("Our Father","Padre nuestro"),prayers.localizedOurFather));add(RosaryStep(t("Hail Mary (for Faith)","Ave María (por la fe)"),prayers.localizedHailMary));add(RosaryStep(t("Hail Mary (for Hope)","Ave María (por la esperanza)"),prayers.localizedHailMary));add(RosaryStep(t("Hail Mary (for Charity)","Ave María (por la caridad)"),prayers.localizedHailMary));add(RosaryStep(t("Glory Be","Gloria al Padre"),prayers.localizedGloryBe))
    set.mysteries.forEachIndexed{index,mystery->add(RosaryStep(if(es) "Misterio ${index+1}: ${mystery.localizedTitle}" else "Mystery ${index+1}: ${mystery.localizedTitle}",mystery.localizedScripture));add(RosaryStep(t("Our Father","Padre nuestro"),prayers.localizedOurFather));repeat(10){count->add(RosaryStep(t("Hail Mary","Ave María"),prayers.localizedHailMary,count+1))};add(RosaryStep(t("Glory Be","Gloria al Padre"),prayers.localizedGloryBe));add(RosaryStep(t("Fatima Prayer","Oración de Fátima"),prayers.localizedFatimaPrayer))}
    add(RosaryStep(t("Hail, Holy Queen","Salve, Reina y Madre"),prayers.localizedHailHolyQueen));add(RosaryStep(t("Concluding Prayer","Oración final"),prayers.localizedConcludingPrayer));add(RosaryStep(t("Final Sign of the Cross","Señal de la cruz final"),prayers.localizedSignOfTheCross));add(RosaryStep(t("Rosary Completed","Rosario completado"),t("You have completed the Holy Rosary. Peace be with you.","Has completado el santo Rosario. La paz esté contigo.")))
}

private fun loadFormationCatalog(text: String): FormationCatalog {
    val root = JSONObject(text)
    val prayers = root.getJSONArray("commonPrayers").let { array ->
        (0 until array.length()).map { i ->
            array.getJSONObject(i).let {
                FormationPrayer(
                    id = it.getString("id"),
                    title = it.getString("title"),
                    text = it.getString("text"),
                    titleEs = it.optString("titleEs"),
                    textEs = it.optString("textEs"),
                )
            }
        }
    }
    fun html(obj: JSONObject) = FormationHtml(obj.getString("title"), obj.optString("contentHTML", obj.optString("description")), obj.optString("titleEs"), obj.optString("contentHTMLEs", obj.optString("descriptionEs")))
    val rosaryRoot=root.getJSONObject("rosary");val prayerRoot=rosaryRoot.getJSONObject("prayers")
    val rosaryPrayers=RosaryPrayers(prayerRoot.getString("signOfTheCross"),prayerRoot.getString("apostlesCreed"),prayerRoot.getString("ourFather"),prayerRoot.getString("hailMary"),prayerRoot.getString("gloryBe"),prayerRoot.getString("fatimaPrayer"),prayerRoot.getString("hailHolyQueen"),prayerRoot.getString("concludingPrayer"),prayerRoot.optString("signOfTheCrossEs"),prayerRoot.optString("apostlesCreedEs"),prayerRoot.optString("ourFatherEs"),prayerRoot.optString("hailMaryEs"),prayerRoot.optString("gloryBeEs"),prayerRoot.optString("fatimaPrayerEs"),prayerRoot.optString("hailHolyQueenEs"),prayerRoot.optString("concludingPrayerEs"))
    val rosary = rosaryRoot.getJSONArray("mysteries").let { array -> (0 until array.length()).map { i -> array.getJSONObject(i).let {
        val mysteryItems = it.getJSONArray("mysteries")
        val items=(0 until mysteryItems.length()).map{index->mysteryItems.getJSONObject(index).let{item->RosaryMystery(item.getString("title"),item.getString("scripture"),item.optString("titleEs"),item.optString("scriptureEs"))}}
        RosarySet(it.getString("id"), it.getString("title"), it.getString("descriptionHTML"),items,it.optString("titleEs"),it.optString("descriptionHTMLEs"))
    } } }
    val practices = root.getJSONArray("spiritualPractices").let { array -> (0 until array.length()).map { i -> html(array.getJSONObject(i)) } }
    val hoursRoot = root.getJSONObject("liturgyOfTheHours")
    val hours = hoursRoot.getJSONArray("hours").let { array -> (0 until array.length()).map { i -> array.getJSONObject(i).let { FormationHtml(it.getString("title"), "<h2>${it.getString("title")}</h2><p>${it.getString("description")}</p>") } } }
    val hoursDescription = if(java.util.Locale.getDefault().language=="es") hoursRoot.optString("descriptionEs", hoursRoot.getString("description")) else hoursRoot.getString("description")
    return FormationCatalog(prayers, html(root.getJSONObject("lectioDivina")), html(root.getJSONObject("examinationOfConscience")), practices, rosary, rosaryPrayers, hours, hoursDescription)
}

private fun styledHtml(body: String) = """<html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{font-family:Georgia,serif;color:#1e1c1a;font-size:17px;line-height:1.55;background:#fdfdfc;padding:10px}h1,h2,h3{color:#3b6fa0}a{color:#3b6fa0}</style></head><body>$body</body></html>"""
private fun formationBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
