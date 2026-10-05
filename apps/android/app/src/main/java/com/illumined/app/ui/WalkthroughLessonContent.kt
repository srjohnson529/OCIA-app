package com.illumined.app.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// Only the tour uses segmented content. The normal lesson reader is unchanged.
internal fun walkthroughLessonParts(html: String): List<Pair<String, String>> {
    val headings = Regex("<h[2-4]\\b[^>]*>([\\s\\S]*?)</h[2-4]>", RegexOption.IGNORE_CASE).findAll(html).toList()
    if (headings.isEmpty()) return listOf(html to "")
    return buildList {
        if(headings.first().range.first > 0) add(html.substring(0, headings.first().range.first) to "")
        headings.forEachIndexed { index, heading ->
            val end = headings.getOrNull(index + 1)?.range?.first ?: html.length
            add(html.substring(heading.range.first, end) to android.text.Html.fromHtml(heading.groupValues[1], android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim())
        }
    }
}

internal fun walkthroughLessonStep(index: Int, title: String): WalkthroughStep {
    val key = title.lowercase()
    val copy = when {
        key.contains("defin") -> "Start with the meaning of the topic and its key vocabulary before discussion." to "Empieza por el significado del tema y su vocabulario antes del debate."
        key.contains("scriptur") || key.contains("bíbli") || key.contains("biblic") -> "Read these Bible passages in context and consider what they reveal about the lesson." to "Lee estos pasajes bíblicos en su contexto y considera lo que revelan sobre la lección."
        key.contains("catechis") || key.contains("catecis") -> "Use these Catechism references for deeper study and class preparation. They complement reading the source." to "Usa estas referencias del Catecismo para profundizar y preparar la clase. Complementan la lectura de la fuente."
        key.contains("proclam") -> "This proclamation connects the teaching to the Gospel and its invitation to faith." to "Este anuncio conecta la enseñanza con el Evangelio y su invitación a la fe."
        else -> "Review the teaching here and consider how to discuss its key ideas with your class." to "Revisa la enseñanza y considera cómo tratar sus ideas principales con tu clase."
    }
    return WalkthroughStep("lesson-part-$index", "lessons", "detail", title, copy.first, title, copy.second)
}

@Composable internal fun WalkthroughLessonContent(html: String) {
    val parts = remember(html) { walkthroughLessonParts(html) }
    var headingIndex = 0
    parts.forEach { (content, title) ->
        val key = if(title.isNotBlank()) "lesson-part-${headingIndex++}" else "lesson-intro"
        var height by remember(content) { mutableStateOf(120.dp) }
        AndroidView(modifier = Modifier.fillMaxWidth().height(height).walkthroughAnchor(key), factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                isVerticalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        view.evaluateJavascript("document.body.scrollHeight") { result -> result?.trim('"')?.toFloatOrNull()?.let { height = (it + 16).dp } }
                    }
                }
            }
        }, update = { view ->
            if(view.tag != content) {
                view.tag = content
                view.loadDataWithBaseURL(null, LessonReaderPolicy.wrapHtml(content), "text/html", "UTF-8", null)
            }
        })
    }
}
