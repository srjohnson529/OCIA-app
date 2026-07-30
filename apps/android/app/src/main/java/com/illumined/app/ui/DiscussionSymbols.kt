package com.illumined.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class DiscussionSymbolKind { Bubble, Warning, Chevron, CheckSeal, Check, Reply, Pencil, Trash, PaperPlane }

internal fun discussionSymbol(systemName: String) = when (systemName) {
    "text.bubble", "text.bubble.fill" -> DiscussionSymbolKind.Bubble
    "exclamationmark.triangle" -> DiscussionSymbolKind.Warning
    "chevron.right" -> DiscussionSymbolKind.Chevron
    "checkmark.seal", "checkmark.seal.fill" -> DiscussionSymbolKind.CheckSeal
    "checkmark" -> DiscussionSymbolKind.Check
    "arrowshape.turn.up.left" -> DiscussionSymbolKind.Reply
    "pencil" -> DiscussionSymbolKind.Pencil
    "trash" -> DiscussionSymbolKind.Trash
    "paperplane.fill" -> DiscussionSymbolKind.PaperPlane
    else -> DiscussionSymbolKind.Bubble
}

@Composable
internal fun DiscussionSymbol(kind: DiscussionSymbolKind, color: Color, modifier: Modifier = Modifier) {
    when (kind) {
        DiscussionSymbolKind.Bubble -> DiscussionBubbleGlyph(color, modifier)
        DiscussionSymbolKind.Chevron -> LessonSymbol(LessonSymbolKind.ChevronRight, color, modifier)
        DiscussionSymbolKind.CheckSeal -> MassGuideSymbol(MassGuideSymbolKind.CheckSeal, color, modifier)
        else -> Canvas(modifier) {
            val w = size.minDimension * .085f
            val stroke = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (kind) {
                DiscussionSymbolKind.Warning -> {
                    val p = Path().apply { moveTo(size.width*.5f,size.height*.08f); lineTo(size.width*.94f,size.height*.88f); lineTo(size.width*.06f,size.height*.88f); close() }
                    drawPath(p, color, style = stroke); drawLine(color, Offset(size.width*.5f,size.height*.34f), Offset(size.width*.5f,size.height*.61f), w); drawCircle(color,w*.55f,Offset(size.width*.5f,size.height*.75f))
                }
                DiscussionSymbolKind.Check -> { val p=Path().apply{moveTo(size.width*.12f,size.height*.52f);lineTo(size.width*.4f,size.height*.78f);lineTo(size.width*.9f,size.height*.22f)};drawPath(p,color,style=stroke) }
                DiscussionSymbolKind.Reply -> { val p=Path().apply{moveTo(size.width*.43f,size.height*.18f);lineTo(size.width*.08f,size.height*.48f);lineTo(size.width*.43f,size.height*.78f);moveTo(size.width*.1f,size.height*.48f);quadraticTo(size.width*.72f,size.height*.4f,size.width*.9f,size.height*.84f)};drawPath(p,color,style=stroke) }
                DiscussionSymbolKind.Pencil -> { drawLine(color,Offset(size.width*.18f,size.height*.78f),Offset(size.width*.72f,size.height*.24f),w*1.55f);drawLine(color,Offset(size.width*.72f,size.height*.24f),Offset(size.width*.84f,size.height*.36f),w*1.55f);val tip=Path().apply{moveTo(size.width*.12f,size.height*.88f);lineTo(size.width*.18f,size.height*.7f);lineTo(size.width*.3f,size.height*.82f);close()};drawPath(tip,color) }
                DiscussionSymbolKind.Trash -> { drawRoundRect(color,Offset(size.width*.25f,size.height*.3f),Size(size.width*.5f,size.height*.58f),style=stroke);drawLine(color,Offset(size.width*.16f,size.height*.24f),Offset(size.width*.84f,size.height*.24f),w);drawLine(color,Offset(size.width*.39f,size.height*.12f),Offset(size.width*.61f,size.height*.12f),w);drawLine(color,Offset(size.width*.42f,size.height*.42f),Offset(size.width*.42f,size.height*.73f),w*.65f);drawLine(color,Offset(size.width*.58f,size.height*.42f),Offset(size.width*.58f,size.height*.73f),w*.65f) }
                DiscussionSymbolKind.PaperPlane -> { val p=Path().apply{moveTo(size.width*.06f,size.height*.47f);lineTo(size.width*.92f,size.height*.08f);lineTo(size.width*.63f,size.height*.92f);lineTo(size.width*.45f,size.height*.59f);close()};drawPath(p,color,style=stroke);drawLine(color,Offset(size.width*.45f,size.height*.59f),Offset(size.width*.92f,size.height*.08f),w) }
                else -> Unit
            }
        }
    }
}

@Composable
private fun DiscussionBubbleGlyph(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w=size.minDimension*.085f; val stroke=Stroke(w,cap=StrokeCap.Round,join=StrokeJoin.Round)
        val p=Path().apply{moveTo(size.width*.18f,size.height*.18f);quadraticTo(size.width*.5f,size.height*.04f,size.width*.82f,size.height*.18f);quadraticTo(size.width*.94f,size.height*.47f,size.width*.78f,size.height*.68f);quadraticTo(size.width*.61f,size.height*.86f,size.width*.34f,size.height*.75f);lineTo(size.width*.14f,size.height*.91f);lineTo(size.width*.2f,size.height*.68f);quadraticTo(size.width*.06f,size.height*.45f,size.width*.18f,size.height*.18f);close()}
        drawPath(p,color,style=stroke); listOf(.36f,.51f,.66f).forEach{drawCircle(color,size.minDimension*.055f,Offset(size.width*it,size.height*.43f))}
    }
}
