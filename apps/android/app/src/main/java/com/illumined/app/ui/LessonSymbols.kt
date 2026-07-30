package com.illumined.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class LessonSymbolKind {
    BookClosed,
    DocumentText,
    Clock,
    CheckCircle,
    ChevronRight,
    PlayCircle,
    RadioOff,
    RadioOn,
    WarningCircle,
}

internal object LessonSymbolPresentation {
    const val StrokeFraction = 0.085f
}

@Composable
internal fun LessonSymbol(kind: LessonSymbolKind, color: Color, modifier: Modifier = Modifier, innerColor: Color = Color.White) {
    Canvas(modifier) {
        val width = size.minDimension * LessonSymbolPresentation.StrokeFraction
        val stroke = Stroke(width = width, cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * .39f
        when (kind) {
            LessonSymbolKind.BookClosed -> {
                val book = Path().apply {
                    moveTo(size.width * .13f, size.height * .16f)
                    lineTo(size.width * .76f, size.height * .16f)
                    quadraticTo(size.width * .88f, size.height * .16f, size.width * .88f, size.height * .29f)
                    lineTo(size.width * .88f, size.height * .84f)
                    lineTo(size.width * .23f, size.height * .84f)
                    quadraticTo(size.width * .1f, size.height * .84f, size.width * .1f, size.height * .71f)
                    quadraticTo(size.width * .1f, size.height * .6f, size.width * .23f, size.height * .6f)
                    lineTo(size.width * .88f, size.height * .6f)
                }
                drawPath(book, color, style = stroke)
                drawLine(color, Offset(size.width * .22f, size.height * .16f), Offset(size.width * .22f, size.height * .6f), width)
            }
            LessonSymbolKind.DocumentText -> {
                val document = Path().apply {
                    moveTo(size.width * .2f, size.height * .1f)
                    lineTo(size.width * .61f, size.height * .1f)
                    lineTo(size.width * .82f, size.height * .31f)
                    lineTo(size.width * .82f, size.height * .9f)
                    lineTo(size.width * .2f, size.height * .9f)
                    close()
                    moveTo(size.width * .61f, size.height * .1f)
                    lineTo(size.width * .61f, size.height * .31f)
                    lineTo(size.width * .82f, size.height * .31f)
                }
                drawPath(document, color, style = stroke)
                drawLine(color, Offset(size.width * .33f, size.height * .5f), Offset(size.width * .69f, size.height * .5f), width)
                drawLine(color, Offset(size.width * .33f, size.height * .66f), Offset(size.width * .69f, size.height * .66f), width)
            }
            LessonSymbolKind.Clock -> {
                drawCircle(color, radius, center, style = stroke)
                drawLine(color, center, Offset(center.x, size.height * .25f), width)
                drawLine(color, center, Offset(size.width * .7f, size.height * .59f), width)
            }
            LessonSymbolKind.CheckCircle, LessonSymbolKind.RadioOn -> {
                if (kind == LessonSymbolKind.RadioOn) drawCircle(color, radius, center)
                else drawCircle(color, radius, center, style = stroke)
                val checkColor = if (kind == LessonSymbolKind.RadioOn) Color.White else color
                val check = Path().apply {
                    moveTo(size.width * .28f, size.height * .51f)
                    lineTo(size.width * .44f, size.height * .67f)
                    lineTo(size.width * .74f, size.height * .34f)
                }
                drawPath(check, checkColor, style = stroke)
            }
            LessonSymbolKind.RadioOff -> drawCircle(color, radius, center, style = stroke)
            LessonSymbolKind.ChevronRight -> {
                val path = Path().apply {
                    moveTo(size.width * .3f, size.height * .1f)
                    lineTo(size.width * .7f, size.height * .5f)
                    lineTo(size.width * .3f, size.height * .9f)
                }
                drawPath(path, color, style = stroke)
            }
            LessonSymbolKind.PlayCircle -> {
                drawCircle(color, radius, center)
                val play = Path().apply {
                    moveTo(size.width * .43f, size.height * .31f)
                    lineTo(size.width * .72f, size.height * .5f)
                    lineTo(size.width * .43f, size.height * .69f)
                    close()
                }
                drawPath(play, innerColor)
            }
            LessonSymbolKind.WarningCircle -> {
                drawCircle(color, radius, center)
                drawLine(Color.White, Offset(center.x, size.height * .27f), Offset(center.x, size.height * .57f), width)
                drawCircle(Color.White, width * .55f, Offset(center.x, size.height * .72f))
            }
        }
    }
}
