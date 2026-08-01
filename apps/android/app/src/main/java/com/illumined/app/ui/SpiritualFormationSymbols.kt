package com.illumined.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class SpiritualFormationSymbolKind {
    Prayers, Search, Church, Walking, Book, Bookmark, RosaryGrid, TextBook, Clock,
    PlayCircle, Checklist, SquareOff, SquareOn, CheckSeal, CheckCircle,
    Link, Sunrise, Sun, Sunset, MoonStars, Speaker, MusicTv,
}

internal fun breviarySymbol(symbolName: String) = when (symbolName) {
    "book.closed" -> SpiritualFormationSymbolKind.Book
    "text.book.closed" -> SpiritualFormationSymbolKind.TextBook
    "sunrise" -> SpiritualFormationSymbolKind.Sunrise
    "sun.max" -> SpiritualFormationSymbolKind.Sun
    "sunset" -> SpiritualFormationSymbolKind.Sunset
    "moon.stars" -> SpiritualFormationSymbolKind.MoonStars
    "speaker.wave.2" -> SpiritualFormationSymbolKind.Speaker
    "music.note.tv" -> SpiritualFormationSymbolKind.MusicTv
    else -> SpiritualFormationSymbolKind.Book
}

@Composable
internal fun SpiritualFormationSymbol(kind: SpiritualFormationSymbolKind, color: Color, modifier: Modifier = Modifier, innerColor: Color = Color.White) {
    when (kind) {
        SpiritualFormationSymbolKind.Prayers -> MassGuideSymbol(MassGuideSymbolKind.HandsSparkles, color, modifier)
        SpiritualFormationSymbolKind.Church -> MassGuideSymbol(MassGuideSymbolKind.Church, color, modifier)
        SpiritualFormationSymbolKind.Walking -> MassGuideSymbol(MassGuideSymbolKind.Walking, color, modifier)
        SpiritualFormationSymbolKind.Book -> LessonSymbol(LessonSymbolKind.BookClosed, color, modifier)
        SpiritualFormationSymbolKind.Clock -> LessonSymbol(LessonSymbolKind.Clock, color, modifier)
        SpiritualFormationSymbolKind.PlayCircle -> LessonSymbol(LessonSymbolKind.PlayCircle, color, modifier, innerColor)
        SpiritualFormationSymbolKind.CheckCircle -> LessonSymbol(LessonSymbolKind.CheckCircle, color, modifier)
        SpiritualFormationSymbolKind.CheckSeal -> MassGuideSymbol(MassGuideSymbolKind.CheckSeal, color, modifier)
        SpiritualFormationSymbolKind.Sun -> MassGuideSymbol(MassGuideSymbolKind.Sun, color, modifier)
        else -> Canvas(modifier) {
            val w = size.minDimension * .082f
            val stroke = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val center = Offset(size.width / 2f, size.height / 2f)
            fun book(withLines: Boolean) {
                val p=Path().apply{moveTo(size.width*.12f,size.height*.18f);lineTo(size.width*.43f,size.height*.18f);quadraticTo(size.width*.5f,size.height*.18f,size.width*.5f,size.height*.27f);quadraticTo(size.width*.5f,size.height*.18f,size.width*.57f,size.height*.18f);lineTo(size.width*.88f,size.height*.18f);lineTo(size.width*.88f,size.height*.84f);lineTo(size.width*.58f,size.height*.84f);quadraticTo(size.width*.5f,size.height*.84f,size.width*.5f,size.height*.91f);quadraticTo(size.width*.5f,size.height*.84f,size.width*.42f,size.height*.84f);lineTo(size.width*.12f,size.height*.84f);close()}
                drawPath(p,color,style=stroke);drawLine(color,Offset(size.width*.5f,size.height*.27f),Offset(size.width*.5f,size.height*.9f),w)
                if(withLines){drawLine(color,Offset(size.width*.2f,size.height*.42f),Offset(size.width*.39f,size.height*.42f),w*.6f);drawLine(color,Offset(size.width*.61f,size.height*.42f),Offset(size.width*.8f,size.height*.42f),w*.6f)}
            }
            fun sun(y: Float) {
                drawCircle(color,size.minDimension*.15f,Offset(center.x,size.height*y),style=stroke)
                repeat(6){i->val a=Math.toRadians(i*60.0);val c=kotlin.math.cos(a).toFloat();val s=kotlin.math.sin(a).toFloat();drawLine(color,Offset(center.x+c*size.minDimension*.23f,size.height*y+s*size.minDimension*.23f),Offset(center.x+c*size.minDimension*.34f,size.height*y+s*size.minDimension*.34f),w*.65f)}
            }
            when (kind) {
                SpiritualFormationSymbolKind.Search -> { drawCircle(color,size.minDimension*.27f,Offset(size.width*.42f,size.height*.42f),style=stroke);drawLine(color,Offset(size.width*.61f,size.height*.61f),Offset(size.width*.88f,size.height*.88f),w) }
                SpiritualFormationSymbolKind.RosaryGrid -> { listOf(.25f,.5f,.75f).forEach{x->listOf(.25f,.5f,.75f).forEach{y->drawCircle(color,size.minDimension*.055f,Offset(size.width*x,size.height*y),style=stroke)}};drawLine(color,Offset(size.width*.5f,size.height*.02f),Offset(size.width*.5f,size.height*.18f),w);drawLine(color,Offset(size.width*.43f,size.height*.09f),Offset(size.width*.57f,size.height*.09f),w) }
                SpiritualFormationSymbolKind.TextBook -> book(true)
                SpiritualFormationSymbolKind.Bookmark -> { val p=Path().apply{moveTo(size.width*.26f,size.height*.1f);lineTo(size.width*.74f,size.height*.1f);lineTo(size.width*.74f,size.height*.9f);lineTo(size.width*.5f,size.height*.72f);lineTo(size.width*.26f,size.height*.9f);close()};drawPath(p,color,style=stroke) }
                SpiritualFormationSymbolKind.Checklist -> { listOf(.25f,.5f,.75f).forEach{y->drawLine(color,Offset(size.width*.1f,size.height*y),Offset(size.width*.18f,size.height*(y+.07f)),w);drawLine(color,Offset(size.width*.18f,size.height*(y+.07f)),Offset(size.width*.29f,size.height*(y-.06f)),w);drawLine(color,Offset(size.width*.39f,size.height*y),Offset(size.width*.9f,size.height*y),w)} }
                SpiritualFormationSymbolKind.SquareOff, SpiritualFormationSymbolKind.SquareOn -> { drawRoundRect(color,Offset(size.width*.1f,size.height*.1f),Size(size.width*.8f,size.height*.8f),style=stroke);if(kind==SpiritualFormationSymbolKind.SquareOn){val p=Path().apply{moveTo(size.width*.25f,size.height*.5f);lineTo(size.width*.43f,size.height*.68f);lineTo(size.width*.76f,size.height*.32f)};drawPath(p,color,style=stroke)} }
                SpiritualFormationSymbolKind.Link -> { drawArc(color,135f,180f,false,Offset(size.width*.05f,size.height*.27f),Size(size.width*.52f,size.height*.52f),style=stroke);drawArc(color,-45f,180f,false,Offset(size.width*.43f,size.height*.21f),Size(size.width*.52f,size.height*.52f),style=stroke);drawLine(color,Offset(size.width*.34f,size.height*.62f),Offset(size.width*.66f,size.height*.38f),w) }
                SpiritualFormationSymbolKind.Sunrise -> { sun(.52f);drawLine(color,Offset(size.width*.08f,size.height*.72f),Offset(size.width*.92f,size.height*.72f),w);drawLine(color,Offset(size.width*.5f,size.height*.08f),Offset(size.width*.5f,size.height*.25f),w) }
                SpiritualFormationSymbolKind.Sunset -> { sun(.42f);drawLine(color,Offset(size.width*.08f,size.height*.72f),Offset(size.width*.92f,size.height*.72f),w);drawLine(color,Offset(size.width*.5f,size.height*.79f),Offset(size.width*.5f,size.height*.94f),w);drawLine(color,Offset(size.width*.42f,size.height*.87f),Offset(size.width*.5f,size.height*.95f),w);drawLine(color,Offset(size.width*.58f,size.height*.87f),Offset(size.width*.5f,size.height*.95f),w) }
                SpiritualFormationSymbolKind.MoonStars -> { drawArc(color,75f,235f,false,Offset(size.width*.1f,size.height*.08f),Size(size.width*.68f,size.height*.82f),style=stroke);drawCircle(color,w*.65f,Offset(size.width*.78f,size.height*.24f));drawCircle(color,w*.48f,Offset(size.width*.86f,size.height*.43f)) }
                SpiritualFormationSymbolKind.Speaker -> { val p=Path().apply{moveTo(size.width*.08f,size.height*.4f);lineTo(size.width*.27f,size.height*.4f);lineTo(size.width*.51f,size.height*.2f);lineTo(size.width*.51f,size.height*.8f);lineTo(size.width*.27f,size.height*.6f);lineTo(size.width*.08f,size.height*.6f);close()};drawPath(p,color,style=stroke);drawArc(color,-45f,90f,false,Offset(size.width*.48f,size.height*.28f),Size(size.width*.28f,size.height*.44f),style=stroke);drawArc(color,-45f,90f,false,Offset(size.width*.48f,size.height*.15f),Size(size.width*.48f,size.height*.7f),style=stroke) }
                SpiritualFormationSymbolKind.MusicTv -> { drawRoundRect(color,Offset(size.width*.08f,size.height*.18f),Size(size.width*.84f,size.height*.65f),style=stroke);drawLine(color,Offset(size.width*.38f,size.height*.93f),Offset(size.width*.62f,size.height*.93f),w);drawLine(color,Offset(size.width*.48f,size.height*.34f),Offset(size.width*.7f,size.height*.29f),w);drawLine(color,Offset(size.width*.48f,size.height*.34f),Offset(size.width*.48f,size.height*.65f),w);drawCircle(color,size.minDimension*.07f,Offset(size.width*.4f,size.height*.68f),style=stroke);drawLine(color,Offset(size.width*.7f,size.height*.29f),Offset(size.width*.7f,size.height*.57f),w);drawCircle(color,size.minDimension*.07f,Offset(size.width*.62f,size.height*.6f),style=stroke) }
                else -> Unit
            }
        }
    }
}
