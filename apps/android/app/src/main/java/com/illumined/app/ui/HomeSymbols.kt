package com.illumined.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class HomeSymbolKind { ClassMembers, CalendarBadgeClock, Megaphone, Checklist, ChevronRight }

internal object HomeSymbolPresentation {
    const val StrokeFraction = 0.085f
}

@Composable
internal fun HomeSymbol(kind: HomeSymbolKind, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension * HomeSymbolPresentation.StrokeFraction)
        when (kind) {
            HomeSymbolKind.ClassMembers -> {
                val radius = size.minDimension * .115f
                drawCircle(color, radius, Offset(size.width * .5f, size.height * .28f), style = stroke)
                drawCircle(color, radius * .82f, Offset(size.width * .23f, size.height * .37f), style = stroke)
                drawCircle(color, radius * .82f, Offset(size.width * .77f, size.height * .37f), style = stroke)
                drawArc(color, 205f, 130f, false, Offset(size.width * .25f, size.height * .42f), Size(size.width * .5f, size.height * .42f), style = stroke)
                drawArc(color, 205f, 110f, false, Offset(size.width * .02f, size.height * .5f), Size(size.width * .4f, size.height * .34f), style = stroke)
                drawArc(color, 225f, 110f, false, Offset(size.width * .58f, size.height * .5f), Size(size.width * .4f, size.height * .34f), style = stroke)
            }
            HomeSymbolKind.CalendarBadgeClock -> {
                val calendarTop = size.height * .22f
                drawRoundRect(color, Offset(size.width * .1f, calendarTop), Size(size.width * .7f, size.height * .62f), CornerRadius(size.minDimension * .09f), style = stroke)
                drawLine(color, Offset(size.width * .1f, size.height * .41f), Offset(size.width * .8f, size.height * .41f), stroke.width)
                drawLine(color, Offset(size.width * .28f, size.height * .12f), Offset(size.width * .28f, size.height * .31f), stroke.width)
                drawLine(color, Offset(size.width * .62f, size.height * .12f), Offset(size.width * .62f, size.height * .31f), stroke.width)
                val clockCenter = Offset(size.width * .76f, size.height * .72f)
                val clockRadius = size.minDimension * .2f
                drawCircle(Color.White, clockRadius * 1.18f, clockCenter)
                drawCircle(color, clockRadius, clockCenter, style = stroke)
                drawLine(color, clockCenter, Offset(clockCenter.x, clockCenter.y - clockRadius * .52f), stroke.width)
                drawLine(color, clockCenter, Offset(clockCenter.x + clockRadius * .42f, clockCenter.y), stroke.width)
            }
            HomeSymbolKind.Megaphone -> {
                val horn = Path().apply {
                    moveTo(size.width * .18f, size.height * .42f)
                    lineTo(size.width * .72f, size.height * .18f)
                    lineTo(size.width * .72f, size.height * .78f)
                    lineTo(size.width * .18f, size.height * .58f)
                    close()
                }
                drawPath(horn, color, style = stroke)
                drawRoundRect(color, Offset(size.width * .08f, size.height * .39f), Size(size.width * .16f, size.height * .23f), CornerRadius(size.minDimension * .04f), style = stroke)
                val handle = Path().apply {
                    moveTo(size.width * .31f, size.height * .61f)
                    lineTo(size.width * .42f, size.height * .86f)
                    lineTo(size.width * .57f, size.height * .82f)
                    lineTo(size.width * .49f, size.height * .67f)
                }
                drawPath(handle, color, style = stroke)
                drawLine(color, Offset(size.width * .84f, size.height * .31f), Offset(size.width * .96f, size.height * .22f), stroke.width)
                drawLine(color, Offset(size.width * .85f, size.height * .48f), Offset(size.width, size.height * .48f), stroke.width)
                drawLine(color, Offset(size.width * .84f, size.height * .65f), Offset(size.width * .96f, size.height * .75f), stroke.width)
            }
            HomeSymbolKind.Checklist -> {
                listOf(.25f, .5f, .75f).forEach { y ->
                    val check = Path().apply {
                        moveTo(size.width * .06f, size.height * (y - .01f))
                        lineTo(size.width * .14f, size.height * (y + .07f))
                        lineTo(size.width * .28f, size.height * (y - .08f))
                    }
                    drawPath(check, color, style = stroke)
                    drawLine(color, Offset(size.width * .38f, size.height * y), Offset(size.width * .94f, size.height * y), stroke.width)
                }
            }
            HomeSymbolKind.ChevronRight -> {
                val chevron = Path().apply {
                    moveTo(size.width * .28f, size.height * .1f)
                    lineTo(size.width * .7f, size.height * .5f)
                    lineTo(size.width * .28f, size.height * .9f)
                }
                drawPath(chevron, color, style = stroke)
            }
        }
    }
}
