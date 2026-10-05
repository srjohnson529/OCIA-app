package com.illumined.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.FirebaseAuth

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Timestamp?,
    val replyTo: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val editedAt: Timestamp? = null,
)

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun listen(classId: String, onUpdate: (List<ChatMessage>) -> Unit, onError: (Throwable) -> Unit): ListenerRegistration =
        firestore.collection("chatMessages")
            .whereEqualTo("classId", classId)
            .orderBy("timestamp")
            .limitToLast(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener onError(error)
                onUpdate(snapshot?.documents.orEmpty().map { document ->
                    ChatMessage(
                        id = document.id,
                        senderId = document.getString("senderId").orEmpty(),
                        senderName = document.getString("senderName").orEmpty(),
                        message = document.getString("message").orEmpty(),
                        timestamp = document.getTimestamp("timestamp"),
                        replyTo = document.getString("replyTo"),
                        reactions = (document.get("reactions") as? Map<*, *>)?.entries?.mapNotNull { (key, value) -> if (key is String && value is String) key to value else null }?.toMap().orEmpty(),
                        editedAt = document.getTimestamp("editedAt"),
                    )
                })
            }

    fun send(classId: String, senderName: String, text: String,
        onSuccess: () -> Unit, onError: (Throwable) -> Unit, replyTo: String? = null) {
        val user = auth.currentUser ?: return onError(IllegalStateException("Please sign in before sending messages."))
        if (classId.isBlank()) return onError(IllegalStateException("Please join a class before sending messages."))
        if (text.trim().isEmpty() || text.trim().length > 4000) return onError(IllegalArgumentException("Message must contain 1–4,000 characters."))
        val data = mutableMapOf<String, Any>(
            "senderId" to user.uid,
            "senderName" to senderName,
            "senderEmail" to user.email.orEmpty(),
            "message" to text.trim(),
            "classId" to classId,
            "timestamp" to FieldValue.serverTimestamp(),
        )
        if (replyTo != null) data["replyTo"] = replyTo
        firestore.collection("chatMessages").add(data).addOnSuccessListener { onSuccess() }.addOnFailureListener(onError)
    }

    fun react(message: ChatMessage, emoji: String, onError: (Throwable) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val value: Any = if (message.reactions[uid] == emoji) FieldValue.delete() else emoji
        firestore.collection("chatMessages").document(message.id).update(FieldPath.of("reactions", uid), value).addOnFailureListener(onError)
    }
    fun edit(message: ChatMessage, text: String, onError: (Throwable) -> Unit) {
        if (text.trim().isEmpty() || text.trim().length > 4000) return onError(IllegalArgumentException("Message must contain 1–4,000 characters."))
        firestore.collection("chatMessages").document(message.id).update(mapOf("message" to text.trim(), "editedAt" to FieldValue.serverTimestamp())).addOnFailureListener(onError)
    }
    fun delete(message: ChatMessage, onError: (Throwable) -> Unit) {
        firestore.collection("chatMessages").document(message.id).delete().addOnFailureListener(onError)
    }
}
