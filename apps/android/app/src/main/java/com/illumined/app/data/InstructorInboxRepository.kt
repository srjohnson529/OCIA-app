package com.illumined.app.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class InstructorConversation(val id: String, val studentName: String, val updatedAt: Timestamp?, val lastSenderId: String, val studentId: String)
data class InboxStudent(val id: String, val name: String)

class InstructorInboxRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun conversations(classId: String, userId: String, instructor: Boolean,
        update: (List<InstructorConversation>) -> Unit, error: (Exception) -> Unit): ListenerRegistration {
        var query = db.collection("instructorConversations").whereEqualTo("classId", classId)
        if (!instructor) query = query.whereEqualTo("studentId", userId)
        return query.addSnapshotListener { snapshot, failure ->
            if (failure != null) error(failure)
            else update(snapshot?.documents.orEmpty().map {
                InstructorConversation(it.id, it.getString("studentName").orEmpty(), it.getTimestamp("updatedAt"), it.getString("lastSenderId").orEmpty(), it.getString("studentId").orEmpty())
            }.sortedByDescending { it.updatedAt })
        }
    }

    fun students(classId: String, update: (List<InboxStudent>) -> Unit, error: (Exception) -> Unit): ListenerRegistration =
        db.collection("userProfiles").whereArrayContains("classIds", classId).addSnapshotListener { snapshot, failure ->
            if (failure != null) error(failure)
            else update(snapshot?.documents.orEmpty().filter { document ->
                document.getBoolean("isInstructor") != true && document.getBoolean("isAdmin") != true &&
                    listOf("removedClassIds", "inactiveClassIds", "archivedClassIds").none { (document.get(it) as? List<*>)?.contains(classId) == true }
            }.map { InboxStudent(it.id, it.getString("displayName") ?: it.getString("username").orEmpty()) }
                .filter { it.name.isNotBlank() }.sortedBy { it.name.lowercase() })
        }

    fun messages(id: String, limit: Long, update: (List<ChatMessage>) -> Unit, error: (Exception) -> Unit): ListenerRegistration =
        db.collection("instructorConversations").document(id).collection("messages")
            .orderBy("timestamp").limitToLast(limit).addSnapshotListener { snapshot, failure ->
                if (failure != null) error(failure)
                else update(snapshot?.documents.orEmpty().map {
                    ChatMessage(it.id, it.getString("senderId").orEmpty(), it.getString("senderName").orEmpty(), it.getString("message").orEmpty(), it.getTimestamp("timestamp"))
                })
            }

    fun send(classId: String, userId: String, name: String, conversationId: String?, text: String,
        success: (String) -> Unit, failure: (Exception) -> Unit, recipient: InboxStudent? = null) {
        val message = text.trim()
        if (FirebaseAuth.getInstance().currentUser?.uid != userId || classId.isBlank() || message.isBlank() || message.length > 4000) {
            failure(IllegalArgumentException("Message cannot be sent.")); return
        }
        val studentId = recipient?.id ?: userId
        val id = conversationId ?: "${classId}__${studentId}"
        val ref = db.collection("instructorConversations").document(id)
        val batch = db.batch()
        if (conversationId == null) batch.set(ref, mapOf("classId" to classId, "studentId" to studentId, "studentName" to (recipient?.name ?: name),
            "updatedAt" to FieldValue.serverTimestamp(), "lastSenderId" to userId))
        else batch.update(ref, mapOf("updatedAt" to FieldValue.serverTimestamp(), "lastSenderId" to userId))
        batch.set(ref.collection("messages").document(), mapOf("senderId" to userId, "senderName" to name,
            "message" to message, "timestamp" to FieldValue.serverTimestamp()))
        batch.commit().addOnSuccessListener { success(id) }.addOnFailureListener(failure)
    }
}
