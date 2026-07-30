package com.illumined.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.illumined.app.ui.theme.IlluminedThemeTokens

private data class GameTerm(val id: String, val term: String, val definition: String, val category: String)
private enum class GamePage { MENU, MATCH, NAME }

@Composable
fun FormationGamesExperience(onBack: () -> Unit) {
    var page by remember { mutableStateOf(GamePage.MENU) }
    BackHandler {
        if (page == GamePage.MENU) onBack() else page = GamePage.MENU
    }
    when (page) {
        GamePage.MENU -> GameMenu(onBack) { page = it }
        GamePage.MATCH -> GameRound(definitionFirst = false, onBack = { page = GamePage.MENU })
        GamePage.NAME -> GameRound(definitionFirst = true, onBack = { page = GamePage.MENU })
    }
}

@Composable
private fun GameMenu(onBack: () -> Unit, select: (GamePage) -> Unit) {
    Column(Modifier.fillMaxSize().background(gameBrush()).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        GameCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormationGameSymbol(FormationGameSymbolKind.Puzzle, IlluminedThemeTokens.Blue, Modifier.size(24.dp))
                Text("Formation Games", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
            }
            Text("Practice Catholic moral theology terms with quick, repeatable games.", fontSize = 16.sp, color = IlluminedThemeTokens.SecondaryText, lineHeight = 20.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GameMenuCard("Match Terms", "Choose the correct definition for each virtue or vice.") { select(GamePage.MATCH) }
            GameMenuCard("Name That Term", "Read the definition and select the matching term.") { select(GamePage.NAME) }
        }
    }
}

@Composable
private fun GameMenuCard(title: String, subtitle: String, click: () -> Unit) {
    Surface(onClick = click, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(IlluminedThemeTokens.Gold.copy(.12f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                FormationGameSymbol(formationGameMenuSymbol(title), IlluminedThemeTokens.Gold, Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 14.sp, color = IlluminedThemeTokens.SecondaryText)
            }
            LessonSymbol(LessonSymbolKind.ChevronRight, IlluminedThemeTokens.SecondaryText, Modifier.size(12.dp))
        }
    }
}

@Composable
private fun GameRound(definitionFirst: Boolean, onBack: () -> Unit) {
    var deck by remember { mutableStateOf(gameTerms.shuffled()) }; var index by remember { mutableIntStateOf(0) }; var score by remember { mutableIntStateOf(0) }; var attempts by remember { mutableIntStateOf(0) }; var selected by remember { mutableStateOf<String?>(null) }
    val current = deck[index]; val options = remember(deck, index) { (listOf(current) + deck.filter { it.id != current.id }.shuffled().take(3)).shuffled() }; val answered = selected != null
    fun reset() { deck = gameTerms.shuffled(); index = 0; score = 0; attempts = 0; selected = null }
    Column(Modifier.fillMaxSize().background(gameBrush()).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        GameCard { Row { Column(Modifier.weight(1f)) { Text(if (definitionFirst) "Name That Term" else "Match Terms", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(if (definitionFirst) "Choose the term that matches the definition." else "Choose the definition that matches the term.", color = IlluminedThemeTokens.SecondaryText) }; TextButton(onClick = { reset() }) { Text("Reset") } }; Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ScorePill("Score", score); ScorePill("Attempts", attempts) } }
        GameCard { Text(if (definitionFirst) "Definition" else current.category, color = IlluminedThemeTokens.Gold, fontWeight = FontWeight.SemiBold); Text(if (definitionFirst) current.definition else current.term, fontSize = if (definitionFirst) 20.sp else 30.sp, fontWeight = FontWeight.SemiBold, color = if (definitionFirst) IlluminedThemeTokens.Ink else IlluminedThemeTokens.Blue) }
        options.forEach { option ->
            val correct = answered && option.id == current.id
            val wrong = answered && selected == option.id && option.id != current.id
            val optionSelected = selected == option.id
            val answerColor = if (correct) Color(0xFF2E7D32) else if (wrong) Color.Red else IlluminedThemeTokens.Gold
            val symbol = if (correct) FormationGameSymbolKind.CheckCircleFilled else if (wrong) FormationGameSymbolKind.XCircleFilled else FormationGameSymbolKind.EmptyCircle
            Surface(
                onClick = { if (!answered) { selected = option.id; attempts++; if (option.id == current.id) score++ } },
                enabled = !answered,
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(.94f),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
                    .border(if (correct || wrong) 2.dp else 1.dp, if (correct || wrong) answerColor else IlluminedThemeTokens.Gold.copy(.22f), RoundedCornerShape(16.dp))
                    .semantics { role = Role.RadioButton; this.selected = optionSelected },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    FormationGameSymbol(symbol, answerColor, Modifier.padding(top = 2.dp).size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(if (definitionFirst) option.term else option.definition, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                }
            }
        }
        if (answered) {
            if (definitionFirst) GameCard { Text(current.category, fontSize = 14.sp, color = IlluminedThemeTokens.Gold, fontWeight = FontWeight.SemiBold); Text(current.term, fontSize = 22.sp, color = IlluminedThemeTokens.Blue, fontWeight = FontWeight.SemiBold) }
            Button(onClick = { index = (index + 1) % deck.size; selected = null }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) {
                FormationGameSymbol(FormationGameSymbolKind.ArrowCircleFilled, Color.White, Modifier.size(19.dp), IlluminedThemeTokens.Blue)
                Spacer(Modifier.width(8.dp))
                Text(if (definitionFirst) "Next Definition" else "Next Term", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable private fun RowScope.ScorePill(title: String, value: Int) { Column(Modifier.weight(1f).background(IlluminedThemeTokens.Blue.copy(.07f), RoundedCornerShape(12.dp)).padding(12.dp)) { Text("$value", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue); Text(title, fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText) } }
@Composable private fun GameCard(content: @Composable ColumnScope.() -> Unit) { Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) } }
private fun gameBrush() = Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f)
private val gameTerms = listOf(
        GameTerm("prudence", "Prudence", "Knowing the means to attain the end and how to apply a general principle in concrete circumstances.", "Cardinal Virtues"),
        GameTerm("memory", "Memory", "Remembering the right things pertaining to an action and its circumstances.", "Parts of Prudence"),
        GameTerm("understanding", "Understanding", "The ability to grasp practical principles and the nature of various situations.", "Parts of Prudence"),
        GameTerm("docility", "Docility", "The ability to be led and to take counsel from others.", "Parts of Prudence"),
        GameTerm("shrewdness", "Shrewdness", "Quickness in arriving at the means to the end.", "Parts of Prudence"),
        GameTerm("reason", "Reason", "The ability to reason about practical matters and apply universal principles to particular situations.", "Parts of Prudence"),
        GameTerm("foresight", "Foresight", "The ability to see future outcomes of actions based on past experience.", "Parts of Prudence"),
        GameTerm("circumspection", "Circumspection", "The virtue by which one keeps track of one's circumstances.", "Parts of Prudence"),
        GameTerm("caution", "Caution", "Applying knowledge of the past to action in order to avoid impediments and evils.", "Parts of Prudence"),
        GameTerm("good-counsel", "Good Counsel", "The habit of taking good counsel.", "Potential Parts of Prudence"),
        GameTerm("synesis", "Synesis", "The ability to know what to do when the common law applies.", "Potential Parts of Prudence"),
        GameTerm("gnome", "Gnome", "The ability to know what to do when the common law does not apply.", "Potential Parts of Prudence"),
        GameTerm("justice", "Justice", "To render another his due.", "Cardinal Virtues"),
        GameTerm("commutative-justice", "Commutative Justice", "Justice between individuals.", "Parts of Justice"),
        GameTerm("legal-justice", "Legal Justice", "Justice of the individual toward the common good.", "Parts of Justice"),
        GameTerm("distributive-justice", "Distributive Justice", "Justice of those in charge of the common good toward the individual.", "Parts of Justice"),
        GameTerm("restitution", "Restitution", "The habit by which one pays back what one owes.", "Parts of Justice"),
        GameTerm("religion", "Religion", "The virtue by which we render to God what is due to Him.", "Parts of Justice"),
        GameTerm("devotion", "Devotion", "A prompt will to do those things pertaining to the service of God.", "Parts of Justice"),
        GameTerm("prayer", "Prayer", "The act, and also a virtue, of lifting one's mind and heart to God.", "Parts of Justice"),
        GameTerm("adoration", "Adoration", "The act by which one exhibits due reverence to God.", "Parts of Justice"),
        GameTerm("sacrifice", "Sacrifice", "Offering some good to God in the form of oblation.", "Parts of Justice"),
        GameTerm("vow", "Vow", "Binding oneself by promise to do something, usually in relation to the service of God.", "Parts of Justice"),
        GameTerm("piety", "Piety", "The virtue by which one renders due honor and reverence to one's parents.", "Parts of Justice"),
        GameTerm("dulia", "Dulia", "Giving due honor to one's superiors.", "Parts of Justice"),
        GameTerm("obedience", "Obedience", "Promptness of the will to do the will of one's superior.", "Parts of Justice"),
        GameTerm("gratitude", "Gratitude", "Appreciation, normally expressed, to a benefactor for some gift given.", "Parts of Justice"),
        GameTerm("truthfulness", "Truthfulness", "The habit of telling the truth.", "Parts of Justice"),
        GameTerm("friendship", "Friendship", "The virtue by which one is able to be befriended.", "Parts of Justice"),
        GameTerm("liberality", "Liberality", "The use of one's surplus means to aid the poor.", "Parts of Justice"),
        GameTerm("epikeia", "Epikeia", "The virtue by which one knows the mind of the legislator.", "Parts of Justice"),
        GameTerm("fortitude", "Fortitude", "Willingness to engage the arduous and to endure suffering over time.", "Cardinal Virtues"),
        GameTerm("magnanimity", "Magnanimity", "Seeking excellence in all things, especially great things.", "Parts of Fortitude"),
        GameTerm("magnificence", "Magnificence", "Using one's wealth to do great things.", "Parts of Fortitude"),
        GameTerm("patience", "Patience", "The ability to suffer evils well.", "Parts of Fortitude"),
        GameTerm("perseverance", "Perseverance", "Persisting in the arduous until the end is achieved.", "Parts of Fortitude"),
        GameTerm("longanimity", "Longanimity", "Longness of soul; the ability to await the good.", "Parts of Fortitude"),
        GameTerm("mortification", "Mortification", "The willingness to suffer pain and discomfort well for love of God.", "Parts of Fortitude"),
        GameTerm("courage", "Courage", "Choosing to pursue the good in spite of mortal danger.", "Parts of Fortitude"),
        GameTerm("custody-mind", "Custody of the Mind", "Not allowing improper thoughts to be entertained in the mind.", "Parts of Fortitude"),
        GameTerm("custody-eyes", "Custody of the Eyes", "Maintaining control of sight so as not to be drawn into sin.", "Parts of Fortitude"),
        GameTerm("temperance", "Temperance", "The virtue which moderates the pleasures of touch and taste.", "Cardinal Virtues"),
        GameTerm("shame", "Shame", "Fear of being perceived as lowly.", "Parts of Temperance"),
        GameTerm("honestia", "Honestia", "The habit of always seeking to do what is virtuous in each situation.", "Parts of Temperance"),
        GameTerm("abstinence", "Abstinence", "Refraining from eating certain kinds of food.", "Parts of Temperance"),
        GameTerm("fasting", "Fasting", "Refraining from food in general.", "Parts of Temperance"),
        GameTerm("sobriety", "Sobriety", "The virtue by which one has moderated use of alcohol.", "Parts of Temperance"),
        GameTerm("chastity", "Chastity", "Moderating the pleasures of touch in matters pertaining to the Sixth Commandment.", "Parts of Temperance"),
        GameTerm("continence", "Continence", "A virtue of the will by which one remains steadfast despite the tumult of the appetites.", "Parts of Temperance"),
        GameTerm("clemency", "Clemency or Meekness", "Moderation of the delight of vindication or anger.", "Parts of Temperance"),
        GameTerm("humility", "Humility", "Willingness to live according to the truth and not judge oneself greater than one is.", "Parts of Temperance"),
        GameTerm("eutrapelia", "Eutrapelia", "The virtue of right recreation.", "Parts of Temperance"),
        GameTerm("silence", "Silence", "Not speaking unless necessary and seeking interior quiet of the appetites.", "Parts of Temperance"),
        GameTerm("studiosity", "Studiosity", "Pursuing knowledge according to one's state in life.", "Parts of Temperance"),
        GameTerm("simplicity", "Simplicity", "Moderating one's externals as to quantity, having neither too much nor too little.", "Parts of Temperance"),
        GameTerm("veracity", "Veracity", "Regulating speech and orienting it toward truth.", "Parts of Temperance"),
        GameTerm("faith", "Faith", "The virtue that inclines us to believe precisely what God tells us.", "Theological Virtues"),
        GameTerm("hope", "Hope", "The virtue concerned with future arduous good: eternal beatitude and divine aid.", "Theological Virtues"),
        GameTerm("charity", "Charity", "Friendship between God and man; supernatural love ordered to eternal beatitude.", "Theological Virtues"),
        GameTerm("precipitation", "Precipitation", "Acting too quickly because one does not take counsel.", "Vices Contrary to Prudence"),
        GameTerm("inconsideration", "Inconsideration", "Failing to judge which means is best among those considered.", "Vices Contrary to Prudence"),
        GameTerm("inconsistency", "Inconsistency", "Failing to command or do the action judged to be best.", "Vices Contrary to Prudence"),
        GameTerm("negligence", "Negligence", "Failing to take counsel or failing to do what one should when one ought.", "Vices Contrary to Prudence"),
        GameTerm("guile", "Guile", "The habit of deceit, usually in words.", "Vices Contrary to Prudence"),
        GameTerm("fraud", "Fraud", "The habit of deceit, usually in deeds.", "Vices Contrary to Prudence"),
        GameTerm("murder", "Murder", "Unjust killing of the innocent.", "Vices Contrary to Justice"),
        GameTerm("theft", "Theft", "Hidden taking of what belongs to another.", "Vices Contrary to Justice"),
        GameTerm("robbery", "Robbery", "Open or violent taking of what belongs to another.", "Vices Contrary to Justice"),
        GameTerm("perjury", "Perjury", "Lying under oath.", "Vices Contrary to Justice"),
        GameTerm("detraction", "Detraction", "Saying something true in order to destroy someone's reputation.", "Vices Contrary to Justice"),
        GameTerm("murmuring", "Murmuring", "Hidden detraction meant to separate one person's affection from another.", "Vices Contrary to Justice"),
        GameTerm("superstition", "Superstition", "Rendering honor or practice to a creature that is due only to God.", "Vices Contrary to Justice"),
)
internal val formationGameTermCount: Int get() = gameTerms.size
