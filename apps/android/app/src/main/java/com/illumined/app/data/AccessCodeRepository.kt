package com.illumined.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.FirebaseAuth

data class AccessCode(val code: String, val isActive: Boolean, val classId: String, val parishName: String,
    val usedByName: String, val usedByEmail: String, val createdAt: Timestamp?)

class AccessCodeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun listenInstructorCodes(classId: String, update: (List<AccessCode>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration =
        listen(db.collection("instructorInviteCodes").whereEqualTo("classId", classId), update, error)

    fun listenParishCodes(update: (List<AccessCode>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration =
        listen(db.collection("parishSetupCodes"), update, error)

    private fun listen(query: com.google.firebase.firestore.Query, update: (List<AccessCode>) -> Unit, error: (Throwable) -> Unit) =
        query.addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            update(snapshot?.documents.orEmpty().map { d -> AccessCode(d.id, d.getBoolean("isActive") == true,
                d.getString("classId").orEmpty(), d.getString("parishName").orEmpty(), d.getString("usedByName").orEmpty(),
                d.getString("usedByEmail").orEmpty(), d.getTimestamp("createdAt")) }.sortedByDescending { it.createdAt?.seconds ?: 0 })
        }

    fun createInstructorCode(profile: UserProfile, success: () -> Unit, error: (Throwable) -> Unit) {
        RoleWritePolicy.instructorError(profile, auth.currentUser != null, "Please sign in before creating invite codes.", "Only instructors can create invite codes.", "Assign your instructor profile to a class before creating invite codes.")?.let { return error(IllegalStateException(it)) }
        val classId = profile.classIds.firstOrNull().orEmpty()
        val userId = auth.currentUser!!.uid
        val code = generateCode(false)
        db.collection("instructorInviteCodes").document(code).set(mapOf("classId" to classId, "isActive" to true,
            "usedBy" to "", "usedByEmail" to "", "usedByName" to "", "createdBy" to userId, "createdByName" to profile.displayName,
            "createdAt" to FieldValue.serverTimestamp())).addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun createParishCode(profile: UserProfile, success: () -> Unit, error: (Throwable) -> Unit) {
        RoleWritePolicy.adminSetupCodeError(profile, auth.currentUser != null)?.let { return error(IllegalStateException(it)) }
        val userId = auth.currentUser!!.uid
        val code = generateCode(true)
        db.collection("parishSetupCodes").document(code).set(mapOf("isActive" to true, "usedBy" to "", "usedByEmail" to "",
            "usedByName" to "", "classId" to "", "parishName" to "", "createdBy" to userId, "createdByName" to profile.displayName,
            "createdAt" to FieldValue.serverTimestamp())).addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun deactivate(collection: String, code: String, success: () -> Unit, error: (Throwable) -> Unit) =
        db.collection(collection).document(code).update("isActive", false).addOnSuccessListener { success() }.addOnFailureListener(error)

    private fun generateCode(parish: Boolean): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val raw = (1..8).map { alphabet.random() }.joinToString("")
        val formatted = "${raw.take(4)}-${raw.drop(4)}"
        return if (parish) "START-$formatted" else formatted
    }
}
