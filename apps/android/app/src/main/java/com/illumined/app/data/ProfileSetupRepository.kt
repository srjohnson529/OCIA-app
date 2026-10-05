package com.illumined.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
            "createdAt" to FieldValue.serverTimestamp(), "username" to name, "classId" to classId, "activeClassId" to classId) + extra

    fun joinStudent(name: String, classId: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val user = auth.currentUser ?: return error(IllegalStateException(ProfileSetupIdentityPolicy.missingUserMessage(startingClass = false)))
        val cleanedClass = classId.trim()
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1").getHttpsCallable("joinStudentClass")
            .call(mapOf("displayName" to name.trim(), "code" to cleanedClass))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun claimInstructor(name: String, classId: String, code: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val user = auth.currentUser ?: return error(IllegalStateException(ProfileSetupIdentityPolicy.missingUserMessage(startingClass = false)))
        val cleanedCode = code.trim().uppercase(); val enteredClass = classId.trim(); val invite = db.collection("instructorInviteCodes").document(cleanedCode)
        db.runTransaction { transaction ->
            val profile = transaction.get(db.collection("userProfiles").document(user.uid))
            val snapshot = transaction.get(invite); if (!snapshot.exists()) throw IllegalStateException("That instructor invite code was not found.")
            if (snapshot.getBoolean("isActive") != true || !snapshot.getString("usedBy").isNullOrBlank()) throw IllegalStateException("That instructor invite code has already been used.")
            val inviteClass = snapshot.getString("classId")
            ProfileSetupPolicy.instructorInviteClassError(inviteClass, enteredClass)?.let { throw IllegalStateException(it) }
            val resolvedClass = requireNotNull(inviteClass)
            val existingClasses = (profile.get("classIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val classes = (existingClasses + resolvedClass).distinct()
            transaction.set(db.collection("userProfiles").document(user.uid), profileData(user.uid, user.email.orEmpty(), name.trim(), resolvedClass, true, mapOf("instructorInviteCode" to cleanedCode, "classIds" to classes)), SetOptions.merge())
            transaction.update(invite, mapOf("isActive" to false, "usedBy" to user.uid, "usedByEmail" to user.email.orEmpty(), "usedByName" to name.trim(), "usedAt" to FieldValue.serverTimestamp()))
        }.addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun createAdditionalInstructorClass(profile: UserProfile, classId: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val user = auth.currentUser ?: return error(IllegalStateException("Please sign in before creating another class."))
        val cleanedClass = classId.trim()
        if (cleanedClass.isBlank()) return error(IllegalArgumentException("Enter a class ID."))
        if (cleanedClass.length > 100 || "/" in cleanedClass) return error(IllegalArgumentException("Class IDs must be 100 characters or fewer and cannot contain a slash."))
        if (!profile.isInstructor) return error(IllegalStateException("Instructor access is required."))
        if (profile.classIds.any { it.equals(cleanedClass, ignoreCase = true) }) return error(IllegalStateException("That class ID is already attached to your account."))
        val profileRef = db.collection("userProfiles").document(user.uid)
        val classroomRef = db.collection("classrooms").document(cleanedClass)
        val batch = db.batch()
        batch.update(profileRef, mapOf("classIds" to profile.classIds + cleanedClass, "activeClassId" to cleanedClass, "classId" to cleanedClass))
        batch.set(classroomRef, mapOf(
                "id" to cleanedClass,
                "classId" to cleanedClass,
                "name" to cleanedClass,
                "parishName" to "",
                "instructorId" to user.uid,
                "instructorName" to profile.displayName,
                "studentIds" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "createdBy" to user.uid,
                "isArchived" to false,
            ))
        batch.commit().addOnSuccessListener { success() }.addOnFailureListener { problem ->
            if (problem is FirebaseFirestoreException && problem.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                error(IllegalStateException("That class ID could not be created. It may already be in use."))
            } else error(problem)
        }
    }

    fun checkParishAccess(success: (Boolean) -> Unit, error: (Throwable) -> Unit) {
        val uid = auth.currentUser?.uid ?: return error(IllegalStateException("Please sign in."))
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1").getHttpsCallable("getParishAccess")
            .call().addOnSuccessListener { result ->
                if (auth.currentUser?.uid == uid) {
                    when ((result.data as? Map<*, *>)?.get("status")) {
                        "ready", "not_activated", "completed" -> success((result.data as? Map<*, *>)?.get("status") == "ready")
                        else -> error(IllegalStateException("Could not confirm parish access. Please retry."))
                    }
                }
            }.addOnFailureListener { problem -> if (auth.currentUser?.uid == uid) error(problem) }
    }

    fun startClass(name: String, parish: String, code: String, success: () -> Unit, error: (Throwable) -> Unit, city: String) {
        if (auth.currentUser == null) return error(IllegalStateException(ProfileSetupIdentityPolicy.missingUserMessage(startingClass = true)))
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1").getHttpsCallable("startParishClass")
            .call(mapOf("displayName" to name.trim(), "parishName" to parish.trim(), "city" to city.trim(), "setupCode" to code.trim().uppercase()))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }
}
