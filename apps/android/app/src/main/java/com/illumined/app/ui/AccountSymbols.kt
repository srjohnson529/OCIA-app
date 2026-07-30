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

internal enum class AccountSymbolKind { Avatar, Person, Envelope, ClassMembers, Instructor, Student, Book, Rosette, ProfileAlert, SignOut }

@Composable
internal fun AccountSymbol(kind: AccountSymbolKind, color: Color, modifier: Modifier = Modifier) {
    when (kind) {
        AccountSymbolKind.ClassMembers -> HomeSymbol(HomeSymbolKind.ClassMembers, color, modifier)
        AccountSymbolKind.Student -> AwardSymbol(AwardSymbolKind.GraduationCap, color, modifier)
        AccountSymbolKind.Book -> LessonSymbol(LessonSymbolKind.BookClosed, color, modifier)
        else -> Canvas(modifier) {
            val w = size.minDimension * .085f
            val stroke = Stroke(w, cap = StrokeCap.Round)
            fun person() {
                drawCircle(color, size.minDimension * .16f, Offset(size.width * .5f, size.height * .3f), style = stroke)
                drawArc(color, 205f, 130f, false, Offset(size.width * .18f, size.height * .47f), Size(size.width * .64f, size.height * .43f), style = stroke)
            }
            when (kind) {
                AccountSymbolKind.Avatar -> { drawCircle(color, size.minDimension * .43f, Offset(size.width/2f,size.height/2f), style=stroke); person() }
                AccountSymbolKind.Person -> person()
                AccountSymbolKind.Envelope -> {
                    drawRoundRect(color, Offset(size.width*.08f,size.height*.2f), Size(size.width*.84f,size.height*.6f), style=stroke)
                    drawLine(color,Offset(size.width*.1f,size.height*.25f),Offset(size.width*.5f,size.height*.57f),w);drawLine(color,Offset(size.width*.9f,size.height*.25f),Offset(size.width*.5f,size.height*.57f),w)
                }
                AccountSymbolKind.Instructor -> {
                    drawRoundRect(color,Offset(size.width*.06f,size.height*.18f),Size(size.width*.88f,size.height*.64f),style=stroke)
                    drawCircle(color,size.minDimension*.1f,Offset(size.width*.28f,size.height*.4f),style=stroke)
                    drawArc(color,205f,130f,false,Offset(size.width*.14f,size.height*.51f),Size(size.width*.28f,size.height*.2f),style=stroke)
                    drawLine(color,Offset(size.width*.53f,size.height*.37f),Offset(size.width*.82f,size.height*.37f),w);drawLine(color,Offset(size.width*.53f,size.height*.57f),Offset(size.width*.76f,size.height*.57f),w)
                }
                AccountSymbolKind.Rosette -> {
                    val center=Offset(size.width*.5f,size.height*.4f);drawCircle(color,size.minDimension*.25f,center,style=stroke)
                    repeat(8){i->val a=Math.toRadians(i*45.0);val p=Offset(center.x+kotlin.math.cos(a).toFloat()*size.minDimension*.34f,center.y+kotlin.math.sin(a).toFloat()*size.minDimension*.34f);drawCircle(color,size.minDimension*.09f,p,style=stroke)}
                    val ribbons=Path().apply{moveTo(size.width*.36f,size.height*.62f);lineTo(size.width*.3f,size.height*.94f);lineTo(size.width*.5f,size.height*.82f);lineTo(size.width*.7f,size.height*.94f);lineTo(size.width*.64f,size.height*.62f)};drawPath(ribbons,color,style=stroke)
                }
                AccountSymbolKind.ProfileAlert -> {
                    person();drawCircle(color,size.minDimension*.21f,Offset(size.width*.78f,size.height*.75f));drawLine(Color.White,Offset(size.width*.78f,size.height*.64f),Offset(size.width*.78f,size.height*.75f),w);drawCircle(Color.White,w*.45f,Offset(size.width*.78f,size.height*.82f))
                }
                AccountSymbolKind.SignOut -> {
                    drawRoundRect(color,Offset(size.width*.08f,size.height*.1f),Size(size.width*.48f,size.height*.8f),style=stroke)
                    drawLine(color,Offset(size.width*.38f,size.height*.5f),Offset(size.width*.92f,size.height*.5f),w)
                    val arrow=Path().apply{moveTo(size.width*.72f,size.height*.3f);lineTo(size.width*.92f,size.height*.5f);lineTo(size.width*.72f,size.height*.7f)};drawPath(arrow,color,style=stroke)
                }
                else -> Unit
            }
        }
    }
}
