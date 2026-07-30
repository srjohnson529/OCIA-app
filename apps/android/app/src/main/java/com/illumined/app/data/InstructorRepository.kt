package com.illumined.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.ZoneId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

data class Announcement(
    val id: String,
    val title: String,
    val message: String,
    val isActive: Boolean,
    val updatedAt: Timestamp?,
    val createdAt: Timestamp? = null,
) {
    val displayTimestamp: Timestamp?
        get() = updatedAt ?: createdAt
}

class InstructorRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
) {
    private data class Creator(val userId: String, val classId: String, val name: String)

    private fun creator(profile: UserProfile, signInMessage: String, roleMessage: String, error: (Throwable) -> Unit): Creator? {
        RoleWritePolicy.instructorError(profile, auth.currentUser != null, signInMessage, roleMessage)?.let {
            error(IllegalStateException(it)); return null
        }
        val userId = auth.currentUser!!.uid
        val classId = profile.classIds.firstOrNull().orEmpty()
        return Creator(userId, classId, profile.displayName)
    }
    fun listenAssignments(classId: String, update: (List<Assignment>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration =
        db.collection("assignments").whereEqualTo("classId", classId).addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            val assignments = snapshot?.documents.orEmpty().map { document ->
                val arrayLinks = (document.get("lessonLinks") as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }.mapNotNull {
                    val lessonId = (it["lessonId"] as? String).orEmpty().trim()
                    if (lessonId.isBlank()) null else AssignmentLessonLink(lessonId, (it["lessonTitle"] as? String).orEmpty().trim())
                }
                val legacyId = document.getString("lessonId").orEmpty().trim()
                val links = arrayLinks.ifEmpty { if (legacyId.isBlank()) emptyList() else listOf(AssignmentLessonLink(legacyId, document.getString("lessonTitle").orEmpty())) }
                val arrayReadings = (document.get("readings") as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }.map {
                    AssignmentReading(
                        (it["id"] as? String).orEmpty().ifBlank { java.util.UUID.randomUUID().toString() },
                        (it["title"] as? String).orEmpty(),
                        (it["text"] as? String).orEmpty(),
                    )
                }
                val readings = AssignmentContent(emptyList(), arrayReadings).resolvedReadings(
                    document.getString("readingTitle").orEmpty(),
                    document.getString("readingText").orEmpty(),
                )
                Assignment(document.id, document.getString("classId").orEmpty(), document.getString("title").orEmpty(), links.firstOrNull()?.lessonId.orEmpty(), links.firstOrNull()?.lessonTitle.orEmpty(), links, document.getString("instructions").orEmpty(), readings, document.getBoolean("isActive") != false, document.getTimestamp("dueAt"))
            }.sortedBy { it.dueAt?.seconds ?: Long.MAX_VALUE }
            update(assignments)
        }

    fun listenAssignmentCompletions(classId: String, update: (List<AssignmentCompletion>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration =
        db.collection("assignmentCompletions").whereEqualTo("classId", classId).addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            update(snapshot?.documents.orEmpty().map { document ->
                AssignmentCompletion(
                    assignmentId = document.getString("assignmentId").orEmpty(),
                    userId = document.getString("userId").orEmpty(),
                    studentName = document.getString("studentName").orEmpty(),
                    isCompleted = document.getBoolean("isCompleted") == true,
                    parentAssignmentId = document.getString("parentAssignmentId").orEmpty(),
                    assignmentItemId = document.getString("assignmentItemId").orEmpty(),
                    assignmentItemTitle = document.getString("assignmentItemTitle").orEmpty(),
                    assignmentItemType = document.getString("assignmentItemType").orEmpty(),
                )
            })
        }

    fun listenAnnouncements(classId: String, update: (List<Announcement>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration =
        db.collection("announcements").whereEqualTo("classId", classId).addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            update(snapshot?.documents.orEmpty().map { d ->
                Announcement(
                    d.id,
                    d.getString("title").orEmpty(),
                    d.getString("message").orEmpty(),
                    d.getBoolean("isActive") != false,
                    d.getTimestamp("updatedAt"),
                    d.getTimestamp("createdAt"),
                )
            }.sortedByDescending { it.displayTimestamp?.seconds ?: Long.MIN_VALUE })
        }

    fun createAnnouncement(profile: UserProfile, title: String, message: String, success: () -> Unit, error: (Throwable) -> Unit) {
        val creator = creator(profile, "Please sign in before creating an announcement.", "Only instructors can create announcements.", error) ?: return
        InstructorWriteValidation.announcementError(title, message)?.let { return error(IllegalArgumentException(it)) }
        db.collection("announcements").add(mapOf("title" to title.trim(), "message" to message.trim(), "classId" to creator.classId,
            "createdBy" to creator.userId, "createdByName" to creator.name, "isActive" to true, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    /** Creates the announcement and asks the trusted Firebase backend to alert this class. */
    fun createAnnouncementWithPush(
        profile: UserProfile,
        title: String,
        message: String,
        active: Boolean,
        success: (Int) -> Unit,
        error: (Throwable) -> Unit,
    ) {
        val creator = creator(profile, "Please sign in before creating an announcement.", "Only instructors can create announcements.", error) ?: return
        InstructorWriteValidation.announcementError(title, message)?.let { return error(IllegalArgumentException(it)) }
        functions
            .getHttpsCallable("createClassAnnouncement")
            .call(mapOf("classId" to creator.classId, "title" to title.trim(), "message" to message.trim(), "isActive" to active))
            .addOnSuccessListener { result ->
                val recipients = (result.data as? Map<*, *>)?.get("recipientCount") as? Number
                success(recipients?.toInt() ?: 0)
            }
            .addOnFailureListener(error)
    }

    fun updateAnnouncement(profile: UserProfile, id: String, title: String, message: String, active: Boolean, success: () -> Unit, error: (Throwable) -> Unit) {
        creator(profile, "Please sign in before editing an announcement.", "Only instructors can edit announcements.", error) ?: return
        InstructorWriteValidation.announcementError(title, message)?.let { return error(IllegalArgumentException(it)) }
        db.collection("announcements").document(id).update(mapOf("title" to title.trim(), "message" to message.trim(), "isActive" to active, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun deleteAnnouncement(profile: UserProfile, id: String, success: () -> Unit, error: (Throwable) -> Unit) {
        creator(profile, "Please sign in before deleting an announcement.", "Only instructors can delete announcements.", error) ?: return
        db.collection("announcements").document(id).delete().addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun createSchedule(profile: UserProfile, topic: String, details: String, dateMillis: Long, success: () -> Unit, error: (Throwable) -> Unit) {
        val creator = creator(profile, "Please sign in before creating a class schedule item.", "Only instructors can edit the class schedule.", error) ?: return
        InstructorWriteValidation.scheduleError(topic)?.let { return error(IllegalArgumentException(it)) }
        db.collection("classSchedule").add(mapOf("classId" to creator.classId, "topic" to topic.trim(), "details" to details.trim(),
            "date" to Timestamp(java.util.Date(FirestoreDatePolicy.localStartOfDayMillis(dateMillis))), "createdBy" to creator.userId, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun deleteSchedule(profile: UserProfile, id: String, success: () -> Unit, error: (Throwable) -> Unit) {
        creator(profile, "Please sign in before deleting a class schedule item.", "Only instructors can edit the class schedule.", error) ?: return
        db.collection("classSchedule").document(id).delete().addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun updateSchedule(profile: UserProfile, id: String, topic: String, details: String, dateMillis: Long, success: () -> Unit, error: (Throwable) -> Unit) {
        creator(profile, "Please sign in before editing a class schedule item.", "Only instructors can edit the class schedule.", error) ?: return
        InstructorWriteValidation.scheduleError(topic)?.let { return error(IllegalArgumentException(it)) }
        db.collection("classSchedule").document(id).update(mapOf(
            "topic" to topic.trim(),
            "details" to details.trim(),
            "date" to Timestamp(java.util.Date(FirestoreDatePolicy.localStartOfDayMillis(dateMillis))),
            "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun importSchedule(
        profile: UserProfile,
        rows: List<ImportedScheduleRow>,
        replacingExisting: Boolean,
        existing: List<ScheduleItem>,
        success: () -> Unit,
        error: (Throwable) -> Unit,
    ) {
        val creator = creator(profile, "Please sign in before importing the class schedule.", "Only instructors can import the class schedule.", error) ?: return
        val classId = creator.classId
        if (rows.isEmpty()) return error(IllegalArgumentException("There are no class dates to import."))
        val batch = db.batch()
        if (replacingExisting) existing.filter { it.classId == classId }.forEach {
            batch.delete(db.collection("classSchedule").document(it.id))
        }
        rows.forEach { row ->
            val date = java.util.Date.from(row.date.atStartOfDay(ZoneId.systemDefault()).toInstant())
            batch.set(db.collection("classSchedule").document(), mapOf(
                "classId" to classId,
                "topic" to row.topic.trim(),
                "details" to row.details.trim(),
                "date" to Timestamp(date),
                "createdBy" to creator.userId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
        }
        batch.commit().addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun createAssignment(profile: UserProfile, title: String, instructions: String,
        dueMillis: Long, lessonLinks: List<AssignmentLessonLink>, readings: List<AssignmentReading>,
        success: () -> Unit, error: (Throwable) -> Unit) {
        val creator = creator(profile, "Please sign in before creating an assignment.", "Only instructors can create assignments.", error) ?: return
        InstructorWriteValidation.assignmentError(title)?.let { return error(IllegalArgumentException(it)) }
        val content = AssignmentContent(lessonLinks, readings)
        if (content.hasPartialReading) return error(IllegalArgumentException("Please add both a title and full text for each reading."))
        val cleanedReadings = content.cleanedReadings.map { mapOf("id" to it.id, "title" to it.title, "text" to it.text) }
        val links = content.cleanedLessonLinks.map { mapOf("lessonId" to it.lessonId, "lessonTitle" to it.lessonTitle) }
        val firstLesson = links.firstOrNull(); val firstReading = cleanedReadings.firstOrNull()
        db.collection("assignments").add(mapOf("title" to title.trim(), "instructions" to instructions.trim(), "classId" to creator.classId,
            "lessonId" to (firstLesson?.get("lessonId") ?: ""), "lessonTitle" to (firstLesson?.get("lessonTitle") ?: ""), "lessonLinks" to links,
            "readingTitle" to (firstReading?.get("title") ?: ""), "readingText" to (firstReading?.get("text") ?: ""), "readings" to cleanedReadings, "createdBy" to creator.userId, "createdByName" to creator.name,
            "isActive" to true, "dueAt" to Timestamp(java.util.Date(FirestoreDatePolicy.localStartOfDayMillis(dueMillis))), "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun updateAssignment(profile: UserProfile, id: String, title: String, instructions: String, dueMillis: Long, lessonLinks: List<AssignmentLessonLink>,
        readings: List<AssignmentReading>, active: Boolean, success: () -> Unit, error: (Throwable) -> Unit) {
        creator(profile, "Please sign in before editing an assignment.", "Only instructors can edit assignments.", error) ?: return
        InstructorWriteValidation.assignmentError(title)?.let { return error(IllegalArgumentException(it)) }
        val content = AssignmentContent(lessonLinks, readings)
        if (content.hasPartialReading) return error(IllegalArgumentException("Please add both a title and full text for each reading."))
        val cleanedReadings = content.cleanedReadings.map { mapOf("id" to it.id, "title" to it.title, "text" to it.text) }
        val links = content.cleanedLessonLinks.map { mapOf("lessonId" to it.lessonId, "lessonTitle" to it.lessonTitle) }
        val firstLesson = links.firstOrNull(); val firstReading = cleanedReadings.firstOrNull()
        db.collection("assignments").document(id).update(mapOf("title" to title.trim(), "instructions" to instructions.trim(), "lessonId" to (firstLesson?.get("lessonId") ?: ""),
            "lessonTitle" to (firstLesson?.get("lessonTitle") ?: ""), "lessonLinks" to links, "readingTitle" to (firstReading?.get("title") ?: ""), "readingText" to (firstReading?.get("text") ?: ""), "readings" to cleanedReadings,
            "isActive" to active, "dueAt" to Timestamp(java.util.Date(FirestoreDatePolicy.localStartOfDayMillis(dueMillis))), "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun deleteAssignment(profile: UserProfile, id: String, success: () -> Unit, error: (Throwable) -> Unit) {
        creator(profile, "Please sign in before deleting an assignment.", "Only instructors can delete assignments.", error) ?: return
        db.collection("assignments").document(id).delete().addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun createDiscussion(profile: UserProfile, title: String, prompt: String, lessonId: String,
        lessonTitle: String, required: Boolean, active: Boolean, success: () -> Unit, error: (Throwable) -> Unit) {
        val creator = creator(profile, "Please sign in before creating a discussion.", "Only instructors can create discussion prompts.", error) ?: return
        InstructorWriteValidation.discussionError(title, prompt)?.let { return error(IllegalArgumentException(it)) }
        val id = "discussion-${java.util.UUID.randomUUID()}"
        db.collection("discussionPrompts").document(id).set(mapOf("id" to id, "lessonId" to lessonId, "lessonTitle" to lessonTitle,
            "title" to title.trim(), "prompt" to prompt.trim(), "requiredForAssignment" to required, "classId" to creator.classId,
            "createdBy" to creator.userId, "createdByName" to creator.name, "isActive" to active, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun updateDiscussion(profile: UserProfile, id: String, title: String, prompt: String, lessonId: String, lessonTitle: String, required: Boolean,
        active: Boolean, success: () -> Unit, error: (Throwable) -> Unit) {
        creator(profile, "Please sign in before editing a discussion prompt.", "Only instructors can edit discussion prompts.", error) ?: return
        InstructorWriteValidation.discussionError(title, prompt)?.let { return error(IllegalArgumentException(it)) }
        db.collection("discussionPrompts").document(id)
        .update(mapOf("title" to title.trim(), "prompt" to prompt.trim(), "lessonId" to lessonId, "lessonTitle" to lessonTitle,
            "requiredForAssignment" to required, "isActive" to active, "updatedAt" to FieldValue.serverTimestamp()))
        .addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun deleteDiscussion(profile: UserProfile, id: String, success: () -> Unit, error: (Throwable) -> Unit) {
        creator(profile, "Please sign in before deleting a discussion prompt.", "Only instructors can delete discussion prompts.", error) ?: return
        db.collection("discussionPrompts").document(id).delete().addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun listenDiscussionPrompts(
        classId: String,
        update: (List<DiscussionPrompt>) -> Unit,
        error: (Throwable) -> Unit,
    ): ListenerRegistration = db.collection("discussionPrompts")
        .whereEqualTo("classId", classId)
        .addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            update(snapshot?.documents.orEmpty().map { document ->
                DiscussionPrompt(
                    id = document.id,
                    lessonId = document.getString("lessonId").orEmpty(),
                    title = document.getString("title").orEmpty(),
                    prompt = document.getString("prompt").orEmpty(),
                    lessonTitle = document.getString("lessonTitle").orEmpty(),
                    requiredForAssignment = document.getBoolean("requiredForAssignment") == true,
                    classId = document.getString("classId"),
                    isVisible = document.getBoolean("isActive") != false,
                )
            }.sortedBy { it.title.lowercase() })
        }

    fun listenStudents(classId: String, update: (List<UserProfile>) -> Unit, error: (Throwable) -> Unit): ListenerRegistration =
        db.collection("userProfiles").whereArrayContains("classIds", classId).addSnapshotListener { snapshot, problem ->
            if (problem != null) return@addSnapshotListener error(problem)
            update(snapshot?.documents.orEmpty().map { d -> UserProfile(
                displayName = d.getString("displayName") ?: d.getString("username") ?: "Student",
                classIds = (d.get("classIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                completedLessons = (d.get("completedLessons") as? List<*>)?.filterIsInstance<String>().orEmpty().toSet(),
                memorizedPrayerIds = (d.get("memorizedPrayerIds") as? List<*>)?.filterIsInstance<String>().orEmpty().toSet(),
                earnedBadges = (d.get("earnedBadges") as? List<*>)?.filterIsInstance<String>().orEmpty().toSet(),
                completedMysteries = (d.get("completedMysteries") as? List<*>)?.filterIsInstance<String>().orEmpty().toSet(),
                isInstructor = d.getBoolean("isInstructor") == true,
                isAdmin = d.getBoolean("isAdmin") == true,
                userId = d.getString("userId") ?: d.id,
                email = d.getString("email").orEmpty(),
                currentLessonIndex = d.getLong("currentLessonIndex")?.toInt() ?: 0,
            ) }.filterNot { it.isInstructor })
        }
}
