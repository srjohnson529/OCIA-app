package com.illumined.app.notifications

internal data class NotificationContent(val title: String, val body: String)

internal fun resolveNotificationContent(title: String?, body: String?, data: Map<String, String>): NotificationContent =
    NotificationContent(
        title = title?.takeIf(String::isNotBlank) ?: data["title"]?.takeIf(String::isNotBlank) ?: "Illumined",
        body = body?.takeIf(String::isNotBlank) ?: data["body"]?.takeIf(String::isNotBlank) ?: data["message"]?.takeIf(String::isNotBlank) ?: "You have a new class update.",
    )
