package com.illumined.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.FirebaseAuth

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Timestamp?,
)

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun listen(classId: String, onUpdate: (List<ChatMessage>) -> Unit, onError: (Throwable) -> Unit): ListenerRegistration =
        firestore.collection("chatMessages")
            .whereEqualTo("classId", classId)
            .orderBy("timestamp")
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener onError(error)
                onUpdate(snapshot?.documents.orEmpty().map { document ->
                    ChatMessage(
                        id = document.id,
                        senderId = document.getString("senderId").orEmpty(),
                        senderName = document.getString("senderName").orEmpty(),
                        message = document.getString("message").orEmpty(),
                        timestamp = document.getTimestamp("timestamp"),
                    )
                })
            }

    fun send(classId: String, senderName: String, text: String,
        onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val user = auth.currentUser ?: return onError(IllegalStateException("Please sign in before sending messages."))
        if (classId.isBlank()) return onError(IllegalStateException("Please join a class before sending messages."))
        firestore.collection("chatMessages").add(mapOf(
            "senderId" to user.uid,
            "senderName" to senderName,
            "senderEmail" to user.email.orEmpty(),
            "message" to text.trim(),
            "classId" to classId,
            "timestamp" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { onSuccess() }.addOnFailureListener(onError)
    }
}
