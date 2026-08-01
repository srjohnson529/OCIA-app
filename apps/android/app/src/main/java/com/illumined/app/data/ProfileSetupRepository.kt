package com.illumined.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth

class ProfileSetupRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private fun profileData(userId: String, email: String, name: String, classId: String, instructor: Boolean, extra: Map<String, Any> = emptyMap()) =
        mapOf<String, Any>("userId" to userId, "email" to email, "displayName" to name, "isInstructor" to instructor,
            "isAdmin" to false, "classIds" to listOf(classId), "completedLessons" to emptyList<String>(), "earnedBadges" to emptyList<String>(),
            "completedMysteries" to emptyList<String>(), "memorizedPrayerIds" to emptyList<String>(), "selectedPrayerIds" to emptyList<String>(), "currentLessonIndex" to 0,
            "createdAt" to FieldValue.serverTimestamp(), "username" to name, "classId" to classId) + extra

    fun joinStudent(name: String, classId: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val user = auth.currentUser ?: return error(IllegalStateException(ProfileSetupIdentityPolicy.missingUserMessage(startingClass = false)))
        val cleanedClass = classId.trim()
        db.collection("userProfiles").document(user.uid).set(profileData(user.uid, user.email.orEmpty(), name.trim(), cleanedClass, false), SetOptions.merge())
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun claimInstructor(name: String, classId: String, code: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val user = auth.currentUser ?: return error(IllegalStateException(ProfileSetupIdentityPolicy.missingUserMessage(startingClass = false)))
        val cleanedCode = code.trim().uppercase(); val enteredClass = classId.trim(); val invite = db.collection("instructorInviteCodes").document(cleanedCode)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(invite); if (!snapshot.exists()) throw IllegalStateException("That instructor invite code was not found.")
            if (snapshot.getBoolean("isActive") != true || !snapshot.getString("usedBy").isNullOrBlank()) throw IllegalStateException("That instructor invite code has already been used.")
            val inviteClass = snapshot.getString("classId")
            ProfileSetupPolicy.instructorInviteClassError(inviteClass, enteredClass)?.let { throw IllegalStateException(it) }
            val resolvedClass = requireNotNull(inviteClass)
            transaction.set(db.collection("userProfiles").document(user.uid), profileData(user.uid, user.email.orEmpty(), name.trim(), resolvedClass, true, mapOf("instructorInviteCode" to cleanedCode)), SetOptions.merge())
            transaction.update(invite, mapOf("isActive" to false, "usedBy" to user.uid, "usedByEmail" to user.email.orEmpty(), "usedByName" to name.trim(), "usedAt" to FieldValue.serverTimestamp()))
        }.addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun startClass(name: String, parish: String, classId: String, code: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val user = auth.currentUser ?: return error(IllegalStateException(ProfileSetupIdentityPolicy.missingUserMessage(startingClass = true)))
        val cleanedCode = code.trim().uppercase(); val cleanedClass = classId.trim().uppercase(); val setup = db.collection("parishSetupCodes").document(cleanedCode); val classroom = db.collection("classrooms").document(cleanedClass)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(setup); if (!snapshot.exists()) throw IllegalStateException("That parish setup code was not found.")
            if (snapshot.getBoolean("isActive") != true || !snapshot.getString("usedBy").isNullOrBlank()) throw IllegalStateException("That parish setup code has already been used.")
            transaction.set(db.collection("userProfiles").document(user.uid), profileData(user.uid, user.email.orEmpty(), name.trim(), cleanedClass, true, mapOf("parishSetupCode" to cleanedCode)), SetOptions.merge())
            transaction.set(classroom, mapOf("id" to cleanedClass, "classId" to cleanedClass, "name" to parish.trim(), "parishName" to parish.trim(), "instructorId" to user.uid, "instructorName" to name.trim(), "studentIds" to emptyList<String>(), "createdAt" to FieldValue.serverTimestamp(), "createdBy" to user.uid))
            transaction.update(setup, mapOf("isActive" to false, "usedBy" to user.uid, "usedByEmail" to user.email.orEmpty(), "usedByName" to name.trim(), "classId" to cleanedClass, "parishName" to parish.trim(), "usedAt" to FieldValue.serverTimestamp()))
        }.addOnSuccessListener { success() }.addOnFailureListener(error)
    }
}
