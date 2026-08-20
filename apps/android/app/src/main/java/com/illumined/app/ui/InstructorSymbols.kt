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

internal enum class InstructorSymbolKind { Tools, Megaphone, CalendarClock, Calendar, Checklist, Bubble, Chart, Key, PlusCircle, Active, Paused, Chevron, Book, Document, Person, People, Rosette, Rosary, CheckCircle, EyeSlash, Expand, Collapse, Trash }

internal fun instructorSymbol(systemName: String) = when (systemName) {
    "person.text.rectangle" -> InstructorSymbolKind.Tools
    "megaphone" -> InstructorSymbolKind.Megaphone
    "calendar.badge.clock" -> InstructorSymbolKind.CalendarClock
    "calendar" -> InstructorSymbolKind.Calendar
    "checklist" -> InstructorSymbolKind.Checklist
    "text.bubble", "text.bubble.fill" -> InstructorSymbolKind.Bubble
    "chart.bar" -> InstructorSymbolKind.Chart
    "key" -> InstructorSymbolKind.Key
    "plus.circle.fill" -> InstructorSymbolKind.PlusCircle
    "checkmark.circle.fill" -> InstructorSymbolKind.Active
    "pause.circle.fill" -> InstructorSymbolKind.Paused
    "chevron.right" -> InstructorSymbolKind.Chevron
    "book", "book.closed", "text.book.closed" -> InstructorSymbolKind.Book
    "doc.text" -> InstructorSymbolKind.Document
    "person.crop.circle.fill" -> InstructorSymbolKind.Person
    "person.3" -> InstructorSymbolKind.People
    "rosette" -> InstructorSymbolKind.Rosette
    "circle.grid.cross" -> InstructorSymbolKind.Rosary
    "eye.slash.fill" -> InstructorSymbolKind.EyeSlash
    "chevron.right.circle" -> InstructorSymbolKind.Expand
    "chevron.down.circle.fill" -> InstructorSymbolKind.Collapse
    "trash" -> InstructorSymbolKind.Trash
    else -> InstructorSymbolKind.Tools
}

internal fun instructorToolSymbol(key: String) = instructorSymbol(when(key){
    "classes"->"person.3";"announcements"->"megaphone";"schedule"->"calendar.badge.clock";"assignments"->"checklist";"discussions"->"text.bubble";"progress"->"chart.bar";else->"key"
})

@Composable internal fun InstructorSymbol(kind: InstructorSymbolKind, color: Color, modifier: Modifier = Modifier) {
    when(kind) {
        InstructorSymbolKind.Megaphone -> HomeSymbol(HomeSymbolKind.Megaphone,color,modifier)
        InstructorSymbolKind.CalendarClock -> HomeSymbol(HomeSymbolKind.CalendarBadgeClock,color,modifier)
        InstructorSymbolKind.Checklist -> SpiritualFormationSymbol(SpiritualFormationSymbolKind.Checklist,color,modifier)
        InstructorSymbolKind.Bubble -> DiscussionSymbol(DiscussionSymbolKind.Bubble,color,modifier)
        InstructorSymbolKind.Chevron -> LessonSymbol(LessonSymbolKind.ChevronRight,color,modifier)
        InstructorSymbolKind.Book -> LessonSymbol(LessonSymbolKind.BookClosed,color,modifier)
        InstructorSymbolKind.CheckCircle, InstructorSymbolKind.Active -> LessonSymbol(LessonSymbolKind.CheckCircle,color,modifier)
        InstructorSymbolKind.Rosette -> MoreMenuSymbol(MoreMenuSymbolKind.Awards,color,modifier)
        InstructorSymbolKind.People -> HomeSymbol(HomeSymbolKind.ClassMembers,color,modifier)
        InstructorSymbolKind.Trash -> DiscussionSymbol(DiscussionSymbolKind.Trash,color,modifier)
        else -> Canvas(modifier) {
            val w=size.minDimension*.08f;val stroke=Stroke(w,cap=StrokeCap.Round,join=StrokeJoin.Round)
            when(kind){
                InstructorSymbolKind.Tools->{drawRoundRect(color,Offset(size.width*.08f,size.height*.15f),Size(size.width*.84f,size.height*.7f),style=stroke);drawCircle(color,size.minDimension*.11f,Offset(size.width*.31f,size.height*.42f),style=stroke);drawArc(color,200f,140f,false,Offset(size.width*.17f,size.height*.48f),Size(size.width*.28f,size.height*.25f),style=stroke);drawLine(color,Offset(size.width*.58f,size.height*.38f),Offset(size.width*.82f,size.height*.38f),w);drawLine(color,Offset(size.width*.58f,size.height*.57f),Offset(size.width*.82f,size.height*.57f),w)}
                InstructorSymbolKind.Calendar->{drawRoundRect(color,Offset(size.width*.1f,size.height*.2f),Size(size.width*.8f,size.height*.68f),style=stroke);drawLine(color,Offset(size.width*.1f,size.height*.39f),Offset(size.width*.9f,size.height*.39f),w);drawLine(color,Offset(size.width*.3f,size.height*.08f),Offset(size.width*.3f,size.height*.28f),w);drawLine(color,Offset(size.width*.7f,size.height*.08f),Offset(size.width*.7f,size.height*.28f),w)}
                InstructorSymbolKind.Chart->{listOf(.22f to .58f,.45f to .38f,.68f to .2f).forEach{(x,y)->drawRoundRect(color,Offset(size.width*x,size.height*y),Size(size.width*.14f,size.height*(.86f-y)),style=stroke)}}
                InstructorSymbolKind.Key->{drawCircle(color,size.minDimension*.19f,Offset(size.width*.3f,size.height*.42f),style=stroke);drawLine(color,Offset(size.width*.47f,size.height*.5f),Offset(size.width*.9f,size.height*.8f),w);drawLine(color,Offset(size.width*.68f,size.height*.65f),Offset(size.width*.76f,size.height*.55f),w)}
                InstructorSymbolKind.PlusCircle->{drawCircle(color,size.minDimension*.42f,style=stroke);drawLine(color,Offset(size.width*.5f,size.height*.28f),Offset(size.width*.5f,size.height*.72f),w);drawLine(color,Offset(size.width*.28f,size.height*.5f),Offset(size.width*.72f,size.height*.5f),w)}
                InstructorSymbolKind.Paused->{drawCircle(color,size.minDimension*.42f,style=stroke);drawLine(color,Offset(size.width*.4f,size.height*.34f),Offset(size.width*.4f,size.height*.66f),w);drawLine(color,Offset(size.width*.6f,size.height*.34f),Offset(size.width*.6f,size.height*.66f),w)}
                InstructorSymbolKind.Document->{drawRoundRect(color,Offset(size.width*.16f,size.height*.08f),Size(size.width*.68f,size.height*.84f),style=stroke);listOf(.38f,.55f,.72f).forEach{y->drawLine(color,Offset(size.width*.3f,size.height*y),Offset(size.width*.7f,size.height*y),w*.65f)}}
                InstructorSymbolKind.Person->{drawCircle(color,size.minDimension*.16f,Offset(size.width*.5f,size.height*.34f),style=stroke);drawArc(color,200f,140f,false,Offset(size.width*.2f,size.height*.48f),Size(size.width*.6f,size.height*.42f),style=stroke)}
                InstructorSymbolKind.EyeSlash->{drawArc(color,200f,140f,false,Offset(size.width*.08f,size.height*.22f),Size(size.width*.84f,size.height*.56f),style=stroke);drawCircle(color,size.minDimension*.1f,style=stroke);drawLine(color,Offset(size.width*.12f,size.height*.12f),Offset(size.width*.88f,size.height*.88f),w)}
                InstructorSymbolKind.Expand,InstructorSymbolKind.Collapse->{drawCircle(color,size.minDimension*.43f,style=stroke);val p=Path().apply{if(kind==InstructorSymbolKind.Expand){moveTo(size.width*.4f,size.height*.28f);lineTo(size.width*.62f,size.height*.5f);lineTo(size.width*.4f,size.height*.72f)}else{moveTo(size.width*.28f,size.height*.4f);lineTo(size.width*.5f,size.height*.62f);lineTo(size.width*.72f,size.height*.4f)}};drawPath(p,color,style=stroke)}
                InstructorSymbolKind.Rosary->{listOf(.25f,.5f,.75f).forEach{x->listOf(.25f,.5f,.75f).forEach{y->drawCircle(color,size.minDimension*.045f,Offset(size.width*x,size.height*y),style=stroke)}}}
                else->Unit
            }
        }
    }
}
