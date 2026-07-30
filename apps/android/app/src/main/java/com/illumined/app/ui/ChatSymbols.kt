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

internal enum class ChatSymbolKind { Message, MessageBadge, PaperPlane }

@Composable
internal fun ChatSymbol(kind: ChatSymbolKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension * .085f
        val stroke = Stroke(w, cap = StrokeCap.Round)
        when (kind) {
            ChatSymbolKind.Message, ChatSymbolKind.MessageBadge -> {
                val bubble = Path().apply {
                    moveTo(size.width*.12f,size.height*.16f);lineTo(size.width*.72f,size.height*.16f)
                    quadraticTo(size.width*.86f,size.height*.16f,size.width*.86f,size.height*.3f)
                    lineTo(size.width*.86f,size.height*.58f);quadraticTo(size.width*.86f,size.height*.72f,size.width*.72f,size.height*.72f)
                    lineTo(size.width*.4f,size.height*.72f);lineTo(size.width*.2f,size.height*.9f);lineTo(size.width*.24f,size.height*.72f)
                    lineTo(size.width*.12f,size.height*.72f);quadraticTo(size.width*.04f,size.height*.72f,size.width*.04f,size.height*.58f)
                    lineTo(size.width*.04f,size.height*.3f);quadraticTo(size.width*.04f,size.height*.16f,size.width*.12f,size.height*.16f)
                }
                drawPath(bubble,color,style=stroke)
                if (kind == ChatSymbolKind.MessageBadge) {
                    drawCircle(color,size.minDimension*.17f,Offset(size.width*.78f,size.height*.25f))
                    drawLine(Color.White,Offset(size.width*.71f,size.height*.25f),Offset(size.width*.85f,size.height*.25f),w)
                    drawLine(Color.White,Offset(size.width*.78f,size.height*.18f),Offset(size.width*.78f,size.height*.32f),w)
                }
            }
            ChatSymbolKind.PaperPlane -> {
                val plane=Path().apply{moveTo(size.width*.08f,size.height*.16f);lineTo(size.width*.93f,size.height*.5f);lineTo(size.width*.08f,size.height*.84f);lineTo(size.width*.25f,size.height*.55f);lineTo(size.width*.64f,size.height*.5f);lineTo(size.width*.25f,size.height*.45f);close()}
                drawPath(plane,color)
            }
        }
    }
}
