package com.illumined.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class AwardSymbolKind { Cross, Sparkles, Heart, PrayingHands, GraduationCap, Sun, Lightbulb, Drop, Crown, Lock }

internal fun awardSymbolKind(symbolName: String?): AwardSymbolKind = when (symbolName) {
    "cross.fill" -> AwardSymbolKind.Cross
    "sparkles" -> AwardSymbolKind.Sparkles
    "heart.fill" -> AwardSymbolKind.Heart
    "hands.sparkles.fill" -> AwardSymbolKind.PrayingHands
    "graduationcap.fill" -> AwardSymbolKind.GraduationCap
    "sun.max.fill" -> AwardSymbolKind.Sun
    "lightbulb.fill" -> AwardSymbolKind.Lightbulb
    "drop.fill" -> AwardSymbolKind.Drop
    "crown.fill" -> AwardSymbolKind.Crown
    else -> AwardSymbolKind.Sparkles
}

@Composable
internal fun AwardSymbol(kind: AwardSymbolKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension * .08f
        val stroke = Stroke(w, cap = StrokeCap.Round)
        fun star(cx: Float, cy: Float, outer: Float, inner: Float = outer * .38f) = Path().apply {
            repeat(8) { index ->
                val angle = Math.toRadians((-90.0 + index * 45.0)).toFloat()
                val radius = if (index % 2 == 0) outer else inner
                val point = Offset(cx + kotlin.math.cos(angle) * radius, cy + kotlin.math.sin(angle) * radius)
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        when (kind) {
            AwardSymbolKind.Cross -> {
                drawRect(color, Offset(size.width * .41f, size.height * .08f), Size(size.width * .18f, size.height * .84f))
                drawRect(color, Offset(size.width * .18f, size.height * .32f), Size(size.width * .64f, size.height * .18f))
            }
            AwardSymbolKind.Sparkles -> {
                drawPath(star(size.width * .42f, size.height * .48f, size.minDimension * .32f), color)
                drawPath(star(size.width * .78f, size.height * .22f, size.minDimension * .13f), color)
                drawPath(star(size.width * .76f, size.height * .76f, size.minDimension * .1f), color)
            }
            AwardSymbolKind.Heart -> {
                val heart = Path().apply {
                    moveTo(size.width * .5f, size.height * .88f)
                    cubicTo(size.width * .38f, size.height * .75f, size.width * .12f, size.height * .58f, size.width * .12f, size.height * .32f)
                    cubicTo(size.width * .12f, size.height * .08f, size.width * .4f, size.height * .05f, size.width * .5f, size.height * .25f)
                    cubicTo(size.width * .6f, size.height * .05f, size.width * .88f, size.height * .08f, size.width * .88f, size.height * .32f)
                    cubicTo(size.width * .88f, size.height * .58f, size.width * .62f, size.height * .75f, size.width * .5f, size.height * .88f)
                    close()
                }
                drawPath(heart, color)
            }
            AwardSymbolKind.PrayingHands -> {
                val left = Path().apply { moveTo(size.width*.46f,size.height*.12f); cubicTo(size.width*.35f,size.height*.35f,size.width*.18f,size.height*.58f,size.width*.26f,size.height*.86f); lineTo(size.width*.48f,size.height*.68f) }
                val right = Path().apply { moveTo(size.width*.54f,size.height*.12f); cubicTo(size.width*.65f,size.height*.35f,size.width*.82f,size.height*.58f,size.width*.74f,size.height*.86f); lineTo(size.width*.52f,size.height*.68f) }
                drawPath(left,color,style=stroke); drawPath(right,color,style=stroke)
                drawPath(star(size.width*.82f,size.height*.18f,size.minDimension*.1f),color)
            }
            AwardSymbolKind.GraduationCap -> {
                val cap = Path().apply { moveTo(size.width*.08f,size.height*.4f);lineTo(size.width*.5f,size.height*.17f);lineTo(size.width*.92f,size.height*.4f);lineTo(size.width*.5f,size.height*.63f);close() }
                drawPath(cap,color); drawLine(color,Offset(size.width*.23f,size.height*.52f),Offset(size.width*.23f,size.height*.75f),w); drawArc(color,0f,180f,false,Offset(size.width*.28f,size.height*.48f),Size(size.width*.44f,size.height*.34f),style=stroke)
            }
            AwardSymbolKind.Sun -> {
                drawCircle(color,size.minDimension*.22f,Offset(size.width*.5f,size.height*.5f))
                repeat(8){i->val a=Math.toRadians(i*45.0);val inner=size.minDimension*.32f;val outer=size.minDimension*.45f;drawLine(color,Offset(size.width*.5f+kotlin.math.cos(a).toFloat()*inner,size.height*.5f+kotlin.math.sin(a).toFloat()*inner),Offset(size.width*.5f+kotlin.math.cos(a).toFloat()*outer,size.height*.5f+kotlin.math.sin(a).toFloat()*outer),w)}
            }
            AwardSymbolKind.Lightbulb -> {
                drawCircle(color,size.minDimension*.29f,Offset(size.width*.5f,size.height*.38f),style=stroke)
                drawLine(color,Offset(size.width*.36f,size.height*.65f),Offset(size.width*.64f,size.height*.65f),w);drawLine(color,Offset(size.width*.39f,size.height*.77f),Offset(size.width*.61f,size.height*.77f),w);drawLine(color,Offset(size.width*.44f,size.height*.88f),Offset(size.width*.56f,size.height*.88f),w)
            }
            AwardSymbolKind.Drop -> {
                val drop=Path().apply{moveTo(size.width*.5f,size.height*.07f);cubicTo(size.width*.43f,size.height*.25f,size.width*.18f,size.height*.53f,size.width*.18f,size.height*.7f);cubicTo(size.width*.18f,size.height*.96f,size.width*.82f,size.height*.96f,size.width*.82f,size.height*.7f);cubicTo(size.width*.82f,size.height*.53f,size.width*.57f,size.height*.25f,size.width*.5f,size.height*.07f);close()};drawPath(drop,color)
            }
            AwardSymbolKind.Crown -> {
                val crown=Path().apply{moveTo(size.width*.12f,size.height*.28f);lineTo(size.width*.32f,size.height*.55f);lineTo(size.width*.5f,size.height*.2f);lineTo(size.width*.68f,size.height*.55f);lineTo(size.width*.88f,size.height*.28f);lineTo(size.width*.78f,size.height*.78f);lineTo(size.width*.22f,size.height*.78f);close()};drawPath(crown,color,style=stroke);drawLine(color,Offset(size.width*.24f,size.height*.9f),Offset(size.width*.76f,size.height*.9f),w)
            }
            AwardSymbolKind.Lock -> {
                drawRoundRect(color,Offset(size.width*.2f,size.height*.43f),Size(size.width*.6f,size.height*.46f),style=stroke)
                drawArc(color,180f,180f,false,Offset(size.width*.31f,size.height*.1f),Size(size.width*.38f,size.height*.52f),style=stroke)
                drawCircle(color,w*.55f,Offset(size.width*.5f,size.height*.64f));drawLine(color,Offset(size.width*.5f,size.height*.66f),Offset(size.width*.5f,size.height*.77f),w)
            }
        }
    }
}
