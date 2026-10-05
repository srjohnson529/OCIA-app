package com.illumined.app.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class MessageOpenRequest(val classId: String, val recipientId: String, val privateMessage: Boolean, val refreshments: Boolean = false)
fun canOpenMessageRequest(request: MessageOpenRequest, userId: String, activeClasses: List<String>, removed: List<String>, inactive: List<String>): Boolean =
    request.recipientId == userId && request.classId in activeClasses && request.classId !in removed && request.classId !in inactive
object MessageNotificationNavigation {
    var request by mutableStateOf<MessageOpenRequest?>(null)
    fun accept(data: Map<String, String>) {
        if (data["type"] !in setOf("classroom_message", "chat_reply", "chat_reaction", "private_message", "refreshment_reminder")) return
        val room = data["classId"]?.takeIf { it.isNotBlank() } ?: return
        val recipient = data["recipientId"]?.takeIf { it.isNotBlank() } ?: return
        request = MessageOpenRequest(room, recipient, data["type"] == "private_message", data["type"] == "refreshment_reminder")
    }
}
