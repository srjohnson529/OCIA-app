package com.illumined.app.ui

internal data class ChatLink(
    val start: Int,
    val endExclusive: Int,
    val url: String,
)

internal object ChatPresentation {
    private val webUrlPattern = Regex("""(?i)\b(?:https?://|www\.)[^\s<>\"']+""")
    private val trailingPunctuation = charArrayOf('.', ',', '!', '?', ';', ':', ')', ']', '}')

    fun canSend(draft: String, hasProfile: Boolean, isSending: Boolean) =
        draft.isNotBlank() && hasProfile && !isSending

    fun linksIn(message: String): List<ChatLink> = webUrlPattern.findAll(message).mapNotNull { match ->
        val displayedUrl = match.value.trimEnd(*trailingPunctuation)
        if (displayedUrl.isEmpty()) return@mapNotNull null

        ChatLink(
            start = match.range.first,
            endExclusive = match.range.first + displayedUrl.length,
            url = if (displayedUrl.startsWith("www.", ignoreCase = true)) {
                "https://$displayedUrl"
            } else {
                displayedUrl
            },
        )
    }.toList()
}
