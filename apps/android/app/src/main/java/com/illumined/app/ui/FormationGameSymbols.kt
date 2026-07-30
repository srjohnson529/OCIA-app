package com.illumined.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class FormationGameSymbolKind {
    Puzzle,
    SearchDocument,
    Checklist,
    EmptyCircle,
    CheckCircleFilled,
    XCircleFilled,
    ArrowCircleFilled,
}

internal fun formationGameMenuSymbol(title: String) = when (title) {
    "Match Terms" -> FormationGameSymbolKind.SearchDocument
    "Name That Term" -> FormationGameSymbolKind.Checklist
    else -> FormationGameSymbolKind.Puzzle
}

@Composable
internal fun FormationGameSymbol(
    kind: FormationGameSymbolKind,
    color: Color,
    modifier: Modifier = Modifier,
    innerColor: Color = Color.White,
) {
    if (kind == FormationGameSymbolKind.Puzzle) {
        MoreMenuSymbol(MoreMenuSymbolKind.Games, color, modifier)
        return
    }
    Canvas(modifier) {
        val line = size.minDimension * .085f
        val stroke = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        when (kind) {
            FormationGameSymbolKind.SearchDocument -> {
                val page = Path().apply {
                    moveTo(size.width * .16f, size.height * .10f)
                    lineTo(size.width * .63f, size.height * .10f)
                    lineTo(size.width * .80f, size.height * .27f)
                    lineTo(size.width * .80f, size.height * .57f)
                    moveTo(size.width * .63f, size.height * .10f)
                    lineTo(size.width * .63f, size.height * .28f)
                    lineTo(size.width * .80f, size.height * .28f)
                    moveTo(size.width * .16f, size.height * .10f)
                    lineTo(size.width * .16f, size.height * .88f)
                    lineTo(size.width * .53f, size.height * .88f)
                }
                drawPath(page, color, style = stroke)
                drawLine(color, Offset(size.width * .28f, size.height * .39f), Offset(size.width * .64f, size.height * .39f), line)
                drawLine(color, Offset(size.width * .28f, size.height * .54f), Offset(size.width * .54f, size.height * .54f), line)
                drawCircle(color, size.minDimension * .17f, Offset(size.width * .68f, size.height * .70f), style = stroke)
                drawLine(color, Offset(size.width * .80f, size.height * .82f), Offset(size.width * .91f, size.height * .93f), line)
            }
            FormationGameSymbolKind.Checklist -> {
                listOf(.23f, .50f, .77f).forEach { y ->
                    drawLine(color, Offset(size.width * .10f, size.height * (y - .01f)), Offset(size.width * .16f, size.height * (y + .05f)), line)
                    drawLine(color, Offset(size.width * .16f, size.height * (y + .05f)), Offset(size.width * .27f, size.height * (y - .07f)), line)
                    drawLine(color, Offset(size.width * .37f, size.height * y), Offset(size.width * .90f, size.height * y), line)
                }
            }
            FormationGameSymbolKind.EmptyCircle -> drawCircle(color, size.minDimension * .40f, center, style = stroke)
            FormationGameSymbolKind.CheckCircleFilled -> {
                drawCircle(color, size.minDimension * .46f, center)
                val check = Path().apply {
                    moveTo(size.width * .25f, size.height * .51f)
                    lineTo(size.width * .43f, size.height * .68f)
                    lineTo(size.width * .76f, size.height * .32f)
                }
                drawPath(check, innerColor, style = Stroke(line * .92f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            FormationGameSymbolKind.XCircleFilled -> {
                drawCircle(color, size.minDimension * .46f, center)
                drawLine(innerColor, Offset(size.width * .32f, size.height * .32f), Offset(size.width * .68f, size.height * .68f), line * .92f, StrokeCap.Round)
                drawLine(innerColor, Offset(size.width * .68f, size.height * .32f), Offset(size.width * .32f, size.height * .68f), line * .92f, StrokeCap.Round)
            }
            FormationGameSymbolKind.ArrowCircleFilled -> {
                drawCircle(color, size.minDimension * .46f, center)
                drawLine(innerColor, Offset(size.width * .27f, size.height * .50f), Offset(size.width * .70f, size.height * .50f), line * .9f, StrokeCap.Round)
                drawLine(innerColor, Offset(size.width * .55f, size.height * .34f), Offset(size.width * .71f, size.height * .50f), line * .9f, StrokeCap.Round)
                drawLine(innerColor, Offset(size.width * .55f, size.height * .66f), Offset(size.width * .71f, size.height * .50f), line * .9f, StrokeCap.Round)
            }
            FormationGameSymbolKind.Puzzle -> Unit
        }
    }
}
