package com.illumined.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.auth.FirebaseAuth

data class DiscussionPost(val id: String, val promptId: String, val lessonId: String, val classId: String,
    val authorId: String, val authorName: String, val message: String, val createdAt: Timestamp?)
data class DiscussionReply(val id: String, val postId: String, val authorId: String, val authorName: String,
    val message: String, val createdAt: Timestamp?)

class DiscussionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun listenParticipation(classId: String, userId: String, update: (Set<String>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration {
        val authenticatedId = auth.currentUser?.uid
        if (authenticatedId.isNullOrBlank()) {
            update(emptySet())
            return ListenerRegistration { }
        }
        return db.collection("discussionParticipation").whereEqualTo("userId", authenticatedId).addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            update(snapshot?.documents.orEmpty().mapNotNull { d ->
                if (d.getString("classId") == classId && d.getString("userId") == authenticatedId) d.getString("promptId") else null
            }.toSet())
        }
    }

    fun listenPosts(promptId: String, classId: String, update: (List<DiscussionPost>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration =
        db.collection("discussionPosts").whereEqualTo("classId", classId).addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            update(snapshot?.documents.orEmpty().mapNotNull { d -> if (d.getString("promptId") != promptId) null else DiscussionPost(d.id,
                promptId, d.getString("lessonId").orEmpty(), classId, d.getString("authorId").orEmpty(), d.getString("authorName").orEmpty(),
                d.getString("message").orEmpty(), d.getTimestamp("createdAt")) }.sortedBy { it.createdAt?.seconds ?: 0 })
        }

    fun listenReplies(promptId: String, classId: String, update: (List<DiscussionReply>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration =
        db.collection("discussionReplies").whereEqualTo("classId", classId).addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            update(snapshot?.documents.orEmpty().mapNotNull { d -> if (d.getString("promptId") != promptId) null else DiscussionReply(d.id,
                d.getString("postId").orEmpty(), d.getString("authorId").orEmpty(), d.getString("authorName").orEmpty(), d.getString("message").orEmpty(), d.getTimestamp("createdAt")) }.sortedBy { it.createdAt?.seconds ?: 0 })
        }

    fun post(prompt: DiscussionPrompt, profile: UserProfile, existingPosts: List<DiscussionPost>, message: String,
        success: () -> Unit, error: (Throwable) -> Unit) {
        val authenticatedId = auth.currentUser?.uid
        val primaryClassId = profile.selectedClassId
        DiscussionWritePolicy.postError(authenticatedId, primaryClassId, message, existingPosts.any { it.promptId == prompt.id && it.authorId == authenticatedId })
            ?.let { return error(IllegalStateException(it)) }
        val userId = authenticatedId!!
        val classId = primaryClassId
        val name = profile.displayName
        val post = db.collection("discussionPosts").document()
        val participation = db.collection("discussionParticipation").document("${prompt.id}_$userId")
        db.runBatch { batch ->
            batch.set(post, mapOf("promptId" to prompt.id, "lessonId" to prompt.lessonId, "classId" to classId,
                "authorId" to userId, "authorName" to name, "message" to message.trim(), "createdAt" to FieldValue.serverTimestamp()))
            batch.set(participation, mapOf("promptId" to prompt.id, "lessonId" to prompt.lessonId, "classId" to classId,
                "userId" to userId, "studentName" to name, "postedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
        }.addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun reply(post: DiscussionPost, prompt: DiscussionPrompt, profile: UserProfile, message: String,
        success: () -> Unit, error: (Throwable) -> Unit) {
        val authenticatedId = auth.currentUser?.uid
        val primaryClassId = profile.selectedClassId
        DiscussionWritePolicy.replyError(authenticatedId, primaryClassId, message)
            ?.let { return error(IllegalStateException(it)) }
        db.collection("discussionReplies").add(mapOf("postId" to post.id, "promptId" to prompt.id, "lessonId" to prompt.lessonId,
            "classId" to primaryClassId, "authorId" to authenticatedId!!, "authorName" to profile.displayName, "message" to message.trim(), "createdAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun updatePost(post: DiscussionPost, profile: UserProfile, message: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val authenticatedId = auth.currentUser?.uid ?: return error(IllegalStateException("Please sign in before editing."))
        if (post.authorId != authenticatedId) return error(IllegalStateException("You can only edit your own response."))
        if (message.isBlank()) return error(IllegalStateException("Please write a response before saving."))
        db.collection("discussionPosts").document(post.id).update(mapOf("message" to message.trim(), "authorName" to profile.displayName))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun deletePost(post: DiscussionPost, promptId: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val authenticatedId = auth.currentUser?.uid ?: return error(IllegalStateException("Please sign in before deleting."))
        if (post.authorId != authenticatedId) return error(IllegalStateException("You can only delete your own response."))
        db.runBatch { batch: WriteBatch ->
            batch.delete(db.collection("discussionPosts").document(post.id))
            batch.delete(db.collection("discussionParticipation").document("${promptId}_$authenticatedId"))
        }.addOnSuccessListener { success() }.addOnFailureListener(error)
    }
}

object DiscussionWritePolicy {
    fun postError(userId: String?, classId: String, message: String, alreadyPosted: Boolean): String? = when {
        userId.isNullOrBlank() -> "Please sign in before posting."
        classId.isBlank() -> "Please join a class before posting."
        message.isBlank() -> "Please write a response before posting."
        alreadyPosted -> "You already posted a response for this discussion. Edit or delete your original response to post a new one."
        else -> null
    }

    fun replyError(userId: String?, classId: String, message: String): String? = when {
        userId.isNullOrBlank() -> "Please sign in before replying."
        classId.isBlank() -> "Please join a class before replying."
        message.isBlank() -> "Please write a reply before posting."
        else -> null
    }
}
