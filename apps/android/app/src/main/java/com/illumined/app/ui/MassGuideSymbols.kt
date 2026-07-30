package com.illumined.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class MassGuideSymbolKind {
    Church, People, Book, Eucharist, Walking, HandsSparkles, QuoteBubble,
    ExternalLink, CheckSeal, PersonAlert, TextBubble, Drop, Scroll, Sun,
    Sparkles, Cross, Leaf, Gift, Tray, HeartUp, PeopleTwo, Grid, HeartText,
    ForwardCircle,
}

internal fun massPartSymbol(id: String) = when (id) {
    "introductory-rites" -> MassGuideSymbolKind.People
    "liturgy-word" -> MassGuideSymbolKind.Book
    "liturgy-eucharist" -> MassGuideSymbolKind.Eucharist
    "concluding-rites" -> MassGuideSymbolKind.Walking
    "communion-rite" -> MassGuideSymbolKind.HandsSparkles
    else -> MassGuideSymbolKind.Church
}

internal fun massPrayerSymbol(id: String) = when (id) {
    "confiteor" -> MassGuideSymbolKind.PersonAlert
    "dialogue" -> MassGuideSymbolKind.TextBubble
    "tropes" -> MassGuideSymbolKind.QuoteBubble
    "sprinkling" -> MassGuideSymbolKind.Drop
    "nicene", "apostles" -> MassGuideSymbolKind.Scroll
    "ep1", "ep2", "ep3", "ep4" -> MassGuideSymbolKind.Book
    "gloria" -> MassGuideSymbolKind.Sun
    "sanctus" -> MassGuideSymbolKind.Sparkles
    "memorial-acclamation", "final-blessing" -> MassGuideSymbolKind.Cross
    "lords-prayer", "collect" -> MassGuideSymbolKind.HandsSparkles
    "agnus-dei" -> MassGuideSymbolKind.Leaf
    "universal-prayer" -> MassGuideSymbolKind.PeopleTwo
    "presentation-gifts" -> MassGuideSymbolKind.Gift
    "prayer-over-offerings" -> MassGuideSymbolKind.Tray
    "preface-dialogue" -> MassGuideSymbolKind.HeartUp
    "great-amen" -> MassGuideSymbolKind.CheckSeal
    "communion-invitation" -> MassGuideSymbolKind.Grid
    "prayer-after-communion" -> MassGuideSymbolKind.HeartText
    "dismissal" -> MassGuideSymbolKind.ForwardCircle
    else -> MassGuideSymbolKind.Book
}

@Composable
internal fun MassGuideSymbol(kind: MassGuideSymbolKind, color: Color, modifier: Modifier = Modifier) {
    when (kind) {
        MassGuideSymbolKind.Book -> LessonSymbol(LessonSymbolKind.BookClosed, color, modifier)
        MassGuideSymbolKind.PersonAlert -> AccountSymbol(AccountSymbolKind.ProfileAlert, color, modifier)
        MassGuideSymbolKind.People, MassGuideSymbolKind.PeopleTwo -> HomeSymbol(HomeSymbolKind.ClassMembers, color, modifier)
        MassGuideSymbolKind.Sparkles -> AwardSymbol(AwardSymbolKind.Sparkles, color, modifier)
        MassGuideSymbolKind.Cross -> AwardSymbol(AwardSymbolKind.Cross, color, modifier)
        else -> Canvas(modifier) {
            val w = size.minDimension * .082f
            val stroke = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val center = Offset(size.width / 2f, size.height / 2f)
            fun sparkle(at: Offset, radius: Float) {
                drawLine(color, Offset(at.x - radius, at.y), Offset(at.x + radius, at.y), w * .65f, StrokeCap.Round)
                drawLine(color, Offset(at.x, at.y - radius), Offset(at.x, at.y + radius), w * .65f, StrokeCap.Round)
            }
            fun bubble(tail: Boolean) {
                drawRoundRect(color, Offset(size.width*.10f,size.height*.16f), Size(size.width*.78f,size.height*.58f), CornerRadius(size.minDimension*.12f), style=stroke)
                if (tail) {
                    val p=Path().apply{moveTo(size.width*.28f,size.height*.74f);lineTo(size.width*.20f,size.height*.91f);lineTo(size.width*.44f,size.height*.74f)}
                    drawPath(p,color,style=stroke)
                }
            }
            fun heart() {
                val p=Path().apply{moveTo(size.width*.5f,size.height*.85f);cubicTo(size.width*.38f,size.height*.72f,size.width*.12f,size.height*.53f,size.width*.16f,size.height*.3f);cubicTo(size.width*.2f,size.height*.1f,size.width*.43f,size.height*.15f,size.width*.5f,size.height*.31f);cubicTo(size.width*.57f,size.height*.15f,size.width*.8f,size.height*.1f,size.width*.84f,size.height*.3f);cubicTo(size.width*.88f,size.height*.53f,size.width*.62f,size.height*.72f,size.width*.5f,size.height*.85f)}
                drawPath(p,color,style=stroke)
            }
            when (kind) {
                MassGuideSymbolKind.Church -> {
                    val p=Path().apply{moveTo(size.width*.14f,size.height*.9f);lineTo(size.width*.14f,size.height*.45f);lineTo(size.width*.5f,size.height*.22f);lineTo(size.width*.86f,size.height*.45f);lineTo(size.width*.86f,size.height*.9f);close()}
                    drawPath(p,color,style=stroke); drawLine(color,Offset(size.width*.5f,size.height*.05f),Offset(size.width*.5f,size.height*.29f),w);drawLine(color,Offset(size.width*.41f,size.height*.13f),Offset(size.width*.59f,size.height*.13f),w);drawRoundRect(color,Offset(size.width*.41f,size.height*.62f),Size(size.width*.18f,size.height*.28f),style=stroke)
                }
                MassGuideSymbolKind.Eucharist -> {
                    drawArc(color,0f,180f,false,Offset(size.width*.17f,size.height*.12f),Size(size.width*.66f,size.height*.48f),style=stroke);drawLine(color,Offset(size.width*.5f,size.height*.6f),Offset(size.width*.5f,size.height*.84f),w);drawLine(color,Offset(size.width*.28f,size.height*.86f),Offset(size.width*.72f,size.height*.86f),w)
                }
                MassGuideSymbolKind.Walking -> {
                    drawCircle(color,size.minDimension*.11f,Offset(size.width*.57f,size.height*.17f),style=stroke);drawLine(color,Offset(size.width*.52f,size.height*.3f),Offset(size.width*.43f,size.height*.57f),w);drawLine(color,Offset(size.width*.48f,size.height*.4f),Offset(size.width*.25f,size.height*.51f),w);drawLine(color,Offset(size.width*.48f,size.height*.39f),Offset(size.width*.72f,size.height*.5f),w);drawLine(color,Offset(size.width*.43f,size.height*.57f),Offset(size.width*.24f,size.height*.88f),w);drawLine(color,Offset(size.width*.43f,size.height*.57f),Offset(size.width*.69f,size.height*.84f),w)
                }
                MassGuideSymbolKind.HandsSparkles -> {
                    val left=Path().apply{moveTo(size.width*.1f,size.height*.74f);lineTo(size.width*.34f,size.height*.5f);lineTo(size.width*.46f,size.height*.72f);lineTo(size.width*.31f,size.height*.91f)};drawPath(left,color,style=stroke)
                    val right=Path().apply{moveTo(size.width*.9f,size.height*.74f);lineTo(size.width*.66f,size.height*.5f);lineTo(size.width*.54f,size.height*.72f);lineTo(size.width*.69f,size.height*.91f)};drawPath(right,color,style=stroke);sparkle(Offset(size.width*.5f,size.height*.25f),size.minDimension*.14f)
                }
                MassGuideSymbolKind.QuoteBubble -> { bubble(true); drawLine(color,Offset(size.width*.34f,size.height*.35f),Offset(size.width*.34f,size.height*.52f),w);drawLine(color,Offset(size.width*.58f,size.height*.35f),Offset(size.width*.58f,size.height*.52f),w) }
                MassGuideSymbolKind.TextBubble -> { bubble(true);drawLine(color,Offset(size.width*.28f,size.height*.35f),Offset(size.width*.7f,size.height*.35f),w);drawLine(color,Offset(size.width*.28f,size.height*.52f),Offset(size.width*.6f,size.height*.52f),w) }
                MassGuideSymbolKind.ExternalLink -> { drawRoundRect(color,Offset(size.width*.1f,size.height*.28f),Size(size.width*.62f,size.height*.62f),style=stroke);drawLine(color,Offset(size.width*.47f,size.height*.53f),Offset(size.width*.9f,size.height*.1f),w);drawLine(color,Offset(size.width*.62f,size.height*.1f),Offset(size.width*.9f,size.height*.1f),w);drawLine(color,Offset(size.width*.9f,size.height*.1f),Offset(size.width*.9f,size.height*.38f),w) }
                MassGuideSymbolKind.CheckSeal -> { drawCircle(color,size.minDimension*.4f,center,style=stroke);val p=Path().apply{moveTo(size.width*.28f,size.height*.51f);lineTo(size.width*.44f,size.height*.67f);lineTo(size.width*.73f,size.height*.34f)};drawPath(p,color,style=stroke) }
                MassGuideSymbolKind.Drop -> { val p=Path().apply{moveTo(size.width*.5f,size.height*.08f);cubicTo(size.width*.35f,size.height*.3f,size.width*.2f,size.height*.48f,size.width*.2f,size.height*.66f);cubicTo(size.width*.2f,size.height*.9f,size.width*.8f,size.height*.9f,size.width*.8f,size.height*.66f);cubicTo(size.width*.8f,size.height*.48f,size.width*.65f,size.height*.3f,size.width*.5f,size.height*.08f);close()};drawPath(p,color,style=stroke) }
                MassGuideSymbolKind.Scroll -> { drawRoundRect(color,Offset(size.width*.18f,size.height*.14f),Size(size.width*.64f,size.height*.72f),CornerRadius(size.minDimension*.12f),style=stroke);drawLine(color,Offset(size.width*.33f,size.height*.38f),Offset(size.width*.67f,size.height*.38f),w);drawLine(color,Offset(size.width*.33f,size.height*.56f),Offset(size.width*.67f,size.height*.56f),w) }
                MassGuideSymbolKind.Sun -> { drawCircle(color,size.minDimension*.22f,center,style=stroke);repeat(8){i->val a=Math.toRadians(i*45.0);val c=kotlin.math.cos(a).toFloat();val s=kotlin.math.sin(a).toFloat();drawLine(color,Offset(center.x+c*size.minDimension*.31f,center.y+s*size.minDimension*.31f),Offset(center.x+c*size.minDimension*.45f,center.y+s*size.minDimension*.45f),w*.7f)} }
                MassGuideSymbolKind.Leaf -> { val p=Path().apply{moveTo(size.width*.18f,size.height*.78f);cubicTo(size.width*.18f,size.height*.3f,size.width*.55f,size.height*.08f,size.width*.87f,size.height*.13f);cubicTo(size.width*.88f,size.height*.48f,size.width*.65f,size.height*.83f,size.width*.18f,size.height*.78f)};drawPath(p,color,style=stroke);drawLine(color,Offset(size.width*.2f,size.height*.76f),Offset(size.width*.7f,size.height*.31f),w) }
                MassGuideSymbolKind.Gift -> { drawRoundRect(color,Offset(size.width*.12f,size.height*.35f),Size(size.width*.76f,size.height*.54f),style=stroke);drawLine(color,Offset(size.width*.08f,size.height*.35f),Offset(size.width*.92f,size.height*.35f),w);drawLine(color,Offset(size.width*.5f,size.height*.35f),Offset(size.width*.5f,size.height*.89f),w);drawArc(color,180f,180f,false,Offset(size.width*.23f,size.height*.08f),Size(size.width*.27f,size.height*.27f),style=stroke);drawArc(color,180f,180f,false,Offset(size.width*.5f,size.height*.08f),Size(size.width*.27f,size.height*.27f),style=stroke) }
                MassGuideSymbolKind.Tray -> { drawLine(color,Offset(size.width*.12f,size.height*.72f),Offset(size.width*.88f,size.height*.72f),w);drawArc(color,180f,180f,false,Offset(size.width*.18f,size.height*.25f),Size(size.width*.64f,size.height*.6f),style=stroke) }
                MassGuideSymbolKind.HeartUp -> { heart();drawLine(color,Offset(size.width*.5f,size.height*.55f),Offset(size.width*.5f,size.height*.24f),w);drawLine(color,Offset(size.width*.38f,size.height*.36f),Offset(size.width*.5f,size.height*.24f),w);drawLine(color,Offset(size.width*.62f,size.height*.36f),Offset(size.width*.5f,size.height*.24f),w) }
                MassGuideSymbolKind.Grid -> { listOf(.3f,.7f).forEach{x->listOf(.3f,.7f).forEach{y->drawCircle(color,size.minDimension*.12f,Offset(size.width*x,size.height*y),style=stroke)}} }
                MassGuideSymbolKind.HeartText -> { heart();drawLine(color,Offset(size.width*.36f,size.height*.49f),Offset(size.width*.64f,size.height*.49f),w*.65f);drawLine(color,Offset(size.width*.4f,size.height*.61f),Offset(size.width*.6f,size.height*.61f),w*.65f) }
                MassGuideSymbolKind.ForwardCircle -> { drawCircle(color,size.minDimension*.42f,center,style=stroke);drawLine(color,Offset(size.width*.29f,size.height*.5f),Offset(size.width*.7f,size.height*.5f),w);drawLine(color,Offset(size.width*.56f,size.height*.35f),Offset(size.width*.71f,size.height*.5f),w);drawLine(color,Offset(size.width*.56f,size.height*.65f),Offset(size.width*.71f,size.height*.5f),w) }
                else -> Unit
            }
        }
    }
}
