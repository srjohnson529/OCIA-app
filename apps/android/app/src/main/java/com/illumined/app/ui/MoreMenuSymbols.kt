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

internal enum class MoreMenuSymbolKind { Awards, Chat, Account, Notifications, Games, InstructorTools, AdminTools }

internal fun moreMenuSymbol(title: String) = when (title) {
    "Awards" -> MoreMenuSymbolKind.Awards
    "Chat" -> MoreMenuSymbolKind.Chat
    "Account" -> MoreMenuSymbolKind.Account
    "Notifications" -> MoreMenuSymbolKind.Notifications
    "Games" -> MoreMenuSymbolKind.Games
    "Instructor Tools" -> MoreMenuSymbolKind.InstructorTools
    "Admin Tools" -> MoreMenuSymbolKind.AdminTools
    else -> MoreMenuSymbolKind.Games
}

@Composable
internal fun MoreMenuSymbol(kind: MoreMenuSymbolKind, color: Color, modifier: Modifier = Modifier) {
    when (kind) {
        MoreMenuSymbolKind.Awards -> AccountSymbol(AccountSymbolKind.Rosette, color, modifier)
        MoreMenuSymbolKind.Chat -> ChatSymbol(ChatSymbolKind.Message, color, modifier)
        MoreMenuSymbolKind.Account -> AccountSymbol(AccountSymbolKind.Person, color, modifier)
        MoreMenuSymbolKind.InstructorTools -> AccountSymbol(AccountSymbolKind.Instructor, color, modifier)
        else -> Canvas(modifier) {
            val w = size.minDimension * .085f
            val stroke = Stroke(w, cap = StrokeCap.Round)
            when (kind) {
                MoreMenuSymbolKind.Notifications -> {
                    drawArc(color, 195f, 150f, false, Offset(size.width*.18f,size.height*.12f), Size(size.width*.64f,size.height*.64f), style=stroke)
                    val bell=Path().apply{moveTo(size.width*.23f,size.height*.56f);lineTo(size.width*.15f,size.height*.72f);lineTo(size.width*.85f,size.height*.72f);lineTo(size.width*.77f,size.height*.56f)};drawPath(bell,color,style=stroke)
                    drawArc(color,0f,180f,false,Offset(size.width*.4f,size.height*.7f),Size(size.width*.2f,size.height*.18f),style=stroke)
                    drawCircle(color,size.minDimension*.16f,Offset(size.width*.79f,size.height*.22f));drawCircle(Color.White,w*.55f,Offset(size.width*.79f,size.height*.22f))
                }
                MoreMenuSymbolKind.Games -> {
                    val puzzle=Path().apply{moveTo(size.width*.13f,size.height*.16f);lineTo(size.width*.42f,size.height*.16f);cubicTo(size.width*.39f,size.height*.02f,size.width*.62f,size.height*.02f,size.width*.59f,size.height*.16f);lineTo(size.width*.86f,size.height*.16f);lineTo(size.width*.86f,size.height*.43f);cubicTo(size.width*.99f,size.height*.39f,size.width*.99f,size.height*.62f,size.width*.86f,size.height*.58f);lineTo(size.width*.86f,size.height*.86f);lineTo(size.width*.58f,size.height*.86f);cubicTo(size.width*.62f,size.height*.72f,size.width*.39f,size.height*.72f,size.width*.42f,size.height*.86f);lineTo(size.width*.13f,size.height*.86f);lineTo(size.width*.13f,size.height*.58f);cubicTo(size.width*.01f,size.height*.62f,size.width*.01f,size.height*.39f,size.width*.13f,size.height*.43f);close()};drawPath(puzzle,color,style=stroke)
                }
                MoreMenuSymbolKind.AdminTools -> {
                    drawCircle(color,size.minDimension*.18f,Offset(size.width*.3f,size.height*.5f),style=stroke)
                    drawLine(color,Offset(size.width*.48f,size.height*.5f),Offset(size.width*.9f,size.height*.5f),w);drawLine(color,Offset(size.width*.72f,size.height*.5f),Offset(size.width*.72f,size.height*.68f),w);drawLine(color,Offset(size.width*.84f,size.height*.5f),Offset(size.width*.84f,size.height*.62f),w)
                    drawArc(color,205f,110f,false,Offset(size.width*.07f,size.height*.1f),Size(size.width*.46f,size.height*.8f),style=stroke)
                    drawArc(color,315f,110f,false,Offset(size.width*.02f,size.height*.02f),Size(size.width*.56f,size.height*.96f),style=stroke)
                }
                else -> Unit
            }
        }
    }
}
