package com.illumined.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.auth.FirebaseAuth

data class UserProfile(
    val displayName: String,
    val classIds: List<String>,
    val completedLessons: Set<String>,
    val archivedClassIds: List<String> = emptyList(),
    val memorizedPrayerIds: Set<String> = emptySet(),
    val selectedPrayerIds: Set<String> = emptySet(),
    val earnedBadges: Set<String> = emptySet(),
    val completedMysteries: Set<String> = emptySet(),
    val isInstructor: Boolean = false,
    val isAdmin: Boolean = false,
    val isConfigured: Boolean = true,
    val userId: String = "",
    val email: String = "",
    val currentLessonIndex: Int = 0,
    val activeClassId: String = "",
    val notificationNewPrayerRequests: Boolean = true,
    val notificationNewAssignments: Boolean = true,
    val notificationAssignmentReminders: Boolean = true,
    val notificationDiscussionReplies: Boolean = true,
    val notificationsEnabled: Boolean = true,
) {
    val activeClassIds: List<String>
        get() = classIds.filterNot(archivedClassIds::contains)

    val selectedClassId: String
        get() = activeClassId.takeIf { it in activeClassIds } ?: activeClassIds.firstOrNull().orEmpty()
}

data class Assignment(
    val id: String,
    val classId: String,
    val title: String,
    val lessonId: String,
    val lessonTitle: String,
    val lessonLinks: List<AssignmentLessonLink>,
    val instructions: String,
    val readings: List<AssignmentReading>,
    val isActive: Boolean,
    val dueAt: Timestamp?,
)

data class AssignmentLessonLink(
    val lessonId: String,
    val lessonTitle: String,
)

data class AssignmentReading(
    val id: String,
    val title: String,
    val text: String,
)

data class ScheduleItem(
    val id: String,
    val classId: String,
    val topic: String,
    val details: String,
    val date: Timestamp?,
    val sortOrder: Long? = null,
)

internal val scheduleItemComparator = compareBy<ScheduleItem>(
    { it.date?.seconds ?: Long.MAX_VALUE },
    { it.sortOrder ?: Long.MAX_VALUE },
    { it.topic.lowercase() },
    { it.id },
)

data class DiscussionPrompt(
    val id: String,
    val lessonId: String,
    val title: String,
    val prompt: String,
    val lessonTitle: String,
    val requiredForAssignment: Boolean,
    val classId: String? = null,
    val isVisible: Boolean = true,
)

data class PrayerRequest(
    val id: String,
    val title: String,
    val details: String,
    val requesterName: String,
    val requesterId: String,
    val requesterEmail: String,
    val reactions: Map<String, String> = emptyMap(),
    val createdAt: Timestamp?,
    val expiresAt: Timestamp?,
)

data class FormationOverview(
    val profile: UserProfile,
    val assignments: List<Assignment>,
    val completedAssignmentIds: Set<String>,
    val assignmentCompletions: List<AssignmentCompletion>,
    val schedule: List<ScheduleItem>,
    val discussionPrompts: List<DiscussionPrompt> = emptyList(),
    val prayerRequests: List<PrayerRequest> = emptyList(),
)

internal data class OverviewListenerScope(
    val collection: String,
    val field: String,
    val value: String,
    val activeOnly: Boolean = false,
)

internal fun overviewListenerScopes(userId: String, classId: String?): List<OverviewListenerScope> = buildList {
    if (!classId.isNullOrBlank()) {
        add(OverviewListenerScope("assignmentCompletions", "userId", userId))
        add(OverviewListenerScope("assignments", "classId", classId))
        add(OverviewListenerScope("classSchedule", "classId", classId))
        add(OverviewListenerScope("discussionPrompts", "classId", classId, activeOnly = true))
        add(OverviewListenerScope("prayerRequests", "classId", classId))
    }
}

internal class FormationOverviewAccumulator {
    private var profile: UserProfile? = null
    private var assignments = emptyList<Assignment>()
    private var completions = emptyList<AssignmentCompletion>()
    private var schedule = emptyList<ScheduleItem>()
    private var prompts = emptyList<DiscussionPrompt>()
    private var prayers = emptyList<PrayerRequest>()

    fun updateProfile(value: UserProfile) { profile = value }
    fun updateAssignments(value: List<Assignment>) { assignments = value }
    fun updateCompletions(value: List<AssignmentCompletion>) { completions = value }
    fun updateSchedule(value: List<ScheduleItem>) { schedule = value }
    fun updatePrompts(value: List<DiscussionPrompt>) { prompts = value }
    fun updatePrayers(value: List<PrayerRequest>) { prayers = value }

    fun resetClassData() {
        assignments = emptyList()
        completions = emptyList()
        schedule = emptyList()
        prompts = emptyList()
        prayers = emptyList()
    }

    fun overview(): FormationOverview? = profile?.let { currentProfile ->
        val selectedCompletions = completions.filter {
            it.classId.isBlank() || it.classId == currentProfile.selectedClassId
        }
        FormationOverview(
            profile = currentProfile,
            assignments = assignments,
            completedAssignmentIds = selectedCompletions.filter { it.isCompleted }.map { it.assignmentId }.toSet(),
            assignmentCompletions = selectedCompletions,
            schedule = schedule,
            discussionPrompts = prompts,
            prayerRequests = prayers,
        )
    }
}

private fun DocumentSnapshot?.toUserProfile(): UserProfile {
    val document = this
    val classIds = document?.get("classIds").asStringList().ifEmpty {
        listOfNotNull(document?.getString("classId"))
    }.distinct()
    return UserProfile(
        displayName = document?.getString("displayName") ?: document?.getString("username") ?: "Friend",
        classIds = classIds,
        completedLessons = document?.get("completedLessons").asStringList().toSet(),
        archivedClassIds = document?.get("archivedClassIds").asStringList().distinct(),
        memorizedPrayerIds = document?.get("memorizedPrayerIds").asStringList().toSet(),
        selectedPrayerIds = document?.get("selectedPrayerIds").asStringList().toSet(),
        earnedBadges = document?.get("earnedBadges").asStringList().toSet(),
        completedMysteries = document?.get("completedMysteries").asStringList().toSet(),
        isInstructor = document?.getBoolean("isInstructor") == true,
        isAdmin = document?.getBoolean("isAdmin") == true,
        isConfigured = document?.exists() == true,
        userId = document?.getString("userId") ?: document?.id.orEmpty(),
        email = document?.getString("email").orEmpty(),
        currentLessonIndex = document?.getLong("currentLessonIndex")?.toInt() ?: 0,
        activeClassId = document?.getString("activeClassId").orEmpty(),
        notificationNewPrayerRequests = document?.getBoolean("notificationNewPrayerRequests") != false,
        notificationNewAssignments = document?.getBoolean("notificationNewAssignments") != false,
        notificationAssignmentReminders = document?.getBoolean("notificationAssignmentReminders") != false,
        notificationDiscussionReplies = document?.getBoolean("notificationDiscussionReplies") != false,
        notificationsEnabled = document?.getBoolean("notificationsEnabled") != false,
    )
}

private fun QuerySnapshot.toAssignments(): List<Assignment> = documents
    .filter { it.getBoolean("isActive") != false }
    .map { document ->
        val parsedLessonLinks = (document.get("lessonLinks") as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .mapNotNull {
                val id = (it["lessonId"] as? String).orEmpty().trim()
                if (id.isBlank()) null else AssignmentLessonLink(id, (it["lessonTitle"] as? String).orEmpty().trim())
            }
        val legacyLessonId = document.getString("lessonId").orEmpty().trim()
        val linkedLessons = parsedLessonLinks.ifEmpty {
            if (legacyLessonId.isBlank()) emptyList() else listOf(
                AssignmentLessonLink(legacyLessonId, document.getString("lessonTitle").orEmpty().trim()),
            )
        }
        val firstLesson = linkedLessons.firstOrNull()
        val rawReadings = (document.get("readings") as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .map {
                AssignmentReading(
                    id = (it["id"] as? String).orEmpty().ifBlank { java.util.UUID.randomUUID().toString() },
                    title = (it["title"] as? String).orEmpty(),
                    text = (it["text"] as? String).orEmpty(),
                )
            }
        val readings = AssignmentContent(emptyList(), rawReadings).resolvedReadings(
            legacyTitle = document.getString("readingTitle").orEmpty(),
            legacyText = document.getString("readingText").orEmpty(),
        )
        Assignment(
            id = document.id,
            classId = document.getString("classId").orEmpty(),
            title = document.getString("title").orEmpty(),
            lessonId = firstLesson?.lessonId.orEmpty(),
            lessonTitle = firstLesson?.lessonTitle.orEmpty().ifBlank { document.getString("title").orEmpty() },
            lessonLinks = linkedLessons,
            instructions = document.getString("instructions").orEmpty(),
            readings = readings,
            isActive = document.getBoolean("isActive") != false,
            dueAt = document.getTimestamp("dueAt"),
        )
    }
    .sortedBy { it.dueAt?.seconds ?: Long.MAX_VALUE }

private fun QuerySnapshot.toAssignmentCompletions(): List<AssignmentCompletion> = documents.map { document ->
    AssignmentCompletion(
        assignmentId = document.getString("assignmentId").orEmpty(),
        userId = document.getString("userId").orEmpty(),
        studentName = document.getString("studentName").orEmpty(),
        isCompleted = document.getBoolean("isCompleted") == true,
        parentAssignmentId = document.getString("parentAssignmentId").orEmpty(),
        assignmentItemId = document.getString("assignmentItemId").orEmpty(),
        assignmentItemTitle = document.getString("assignmentItemTitle").orEmpty(),
        assignmentItemType = document.getString("assignmentItemType").orEmpty(),
        classId = document.getString("classId").orEmpty(),
    )
}

private fun QuerySnapshot.toSchedule(): List<ScheduleItem> = documents.map { document ->
    ScheduleItem(
        id = document.id,
        classId = document.getString("classId").orEmpty(),
        topic = document.getString("topic").orEmpty(),
        details = document.getString("details").orEmpty(),
        date = document.getTimestamp("date"),
        sortOrder = document.getLong("sortOrder"),
    )
}.sortedWith(scheduleItemComparator)

private fun QuerySnapshot.toDiscussionPrompts(): List<DiscussionPrompt> = documents
    .filter { it.getBoolean("isActive") != false }
    .map { document ->
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
    }
    .sortedBy { it.title.lowercase() }

private fun QuerySnapshot.toPrayerRequestsAndDeleteExpired(): List<PrayerRequest> {
    val now = Timestamp.now().seconds
    val loaded = documents.map { document ->
        PrayerRequest(
            id = document.id,
            title = document.getString("title").orEmpty(),
            details = document.getString("details").orEmpty(),
            requesterName = document.getString("requesterName").orEmpty(),
            requesterId = document.getString("requesterId").orEmpty(),
            requesterEmail = document.getString("requesterEmail").orEmpty(),
            reactions = (document.get("reactions") as? Map<*, *>).orEmpty()
                .mapNotNull { (key, value) -> if (key is String && value is String) key to value else null }.toMap(),
            createdAt = document.getTimestamp("createdAt"),
            expiresAt = document.getTimestamp("expiresAt"),
        )
    }
    documents.filter { (it.getTimestamp("expiresAt")?.seconds ?: Long.MIN_VALUE) <= now }.forEach {
        it.reference.delete()
    }
    return PrayerRequestPolicy.recentActive(loaded, now, { it.expiresAt?.seconds }, { it.createdAt?.seconds })
}

class FormationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun setActiveClass(profile: UserProfile, classId: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onError(IllegalStateException("Please sign in before switching classes."))
        val selected = classId.trim()
        if (!profile.isInstructor || selected !in profile.classIds) {
            return onError(IllegalArgumentException("You can only switch to a class assigned to your instructor profile."))
        }
        firestore.collection("userProfiles").document(userId).update(
            mapOf("activeClassId" to selected, "classId" to selected),
        ).addOnSuccessListener { onSuccess() }.addOnFailureListener(onError)
    }

    fun listenOverview(
        userId: String,
        onSuccess: (FormationOverview) -> Unit,
        onError: (Throwable) -> Unit,
    ): ListenerRegistration {
        val dynamicListeners = mutableListOf<ListenerRegistration>()
        val accumulator = FormationOverviewAccumulator()
        var activeClassId: String? = null
        var hasBoundScopes = false
        var stopped = false

        fun emit() {
            if (!stopped) accumulator.overview()?.let(onSuccess)
        }

        fun listen(scope: OverviewListenerScope): ListenerRegistration {
            var query: Query = firestore.collection(scope.collection).whereEqualTo(scope.field, scope.value)
            if (scope.activeOnly) query = query.whereEqualTo("isActive", true)
            return query.addSnapshotListener { snapshot, problem ->
                if (problem != null) {
                    onError(problem)
                    return@addSnapshotListener
                }
                val result = snapshot ?: return@addSnapshotListener
                when (scope.collection) {
                    "assignmentCompletions" -> accumulator.updateCompletions(result.toAssignmentCompletions())
                    "assignments" -> accumulator.updateAssignments(result.toAssignments())
                    "classSchedule" -> accumulator.updateSchedule(result.toSchedule())
                    "discussionPrompts" -> accumulator.updatePrompts(result.toDiscussionPrompts())
                    "prayerRequests" -> accumulator.updatePrayers(result.toPrayerRequestsAndDeleteExpired())
                }
                emit()
            }
        }

        val profileListener = firestore.collection("userProfiles").document(userId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { profile, problem ->
                if (problem != null) {
                    onError(problem)
                    return@addSnapshotListener
                }

                // Firestore reports a newly created profile from its local cache
                // before the server has accepted the write. Class-scoped listeners
                // must wait for the server acknowledgement or their rules checks
                // can fail with "Missing or insufficient permissions."
                if (profile?.metadata?.hasPendingWrites() == true) {
                    return@addSnapshotListener
                }

                val currentProfile = profile.toUserProfile()
                accumulator.updateProfile(currentProfile)
                val classId = currentProfile.selectedClassId.takeIf(String::isNotBlank)
                if (!hasBoundScopes || classId != activeClassId) {
                    dynamicListeners.forEach { it.remove() }
                    dynamicListeners.clear()
                    accumulator.resetClassData()
                    activeClassId = classId
                    hasBoundScopes = true
                    overviewListenerScopes(userId, classId).forEach { scope ->
                        dynamicListeners += listen(scope)
                    }
                }
                emit()
            }

        return ListenerRegistration {
            stopped = true
            profileListener.remove()
            dynamicListeners.forEach { it.remove() }
            dynamicListeners.clear()
        }
    }

    fun loadOverview(
        userId: String,
        onSuccess: (FormationOverview) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        firestore.collection("userProfiles").document(userId).get()
            .addOnSuccessListener { profileDocument ->
                val classIds = profileDocument.get("classIds").asStringList().ifEmpty {
                    listOfNotNull(profileDocument.getString("classId"))
                }.distinct()

                val profile = UserProfile(
                    displayName = profileDocument.getString("displayName")
                        ?: profileDocument.getString("username")
                        ?: "Friend",
                    classIds = classIds,
                    completedLessons = profileDocument.get("completedLessons").asStringList().toSet(),
                    archivedClassIds = profileDocument.get("archivedClassIds").asStringList().distinct(),
                    memorizedPrayerIds = profileDocument.get("memorizedPrayerIds").asStringList().toSet(),
                    selectedPrayerIds = profileDocument.get("selectedPrayerIds").asStringList().toSet(),
                    earnedBadges = profileDocument.get("earnedBadges").asStringList().toSet(),
                    completedMysteries = profileDocument.get("completedMysteries").asStringList().toSet(),
                    isInstructor = profileDocument.getBoolean("isInstructor") == true,
                    isAdmin = profileDocument.getBoolean("isAdmin") == true,
                    isConfigured = profileDocument.exists(),
                    userId = profileDocument.getString("userId") ?: profileDocument.id,
                    email = profileDocument.getString("email").orEmpty(),
                    currentLessonIndex = profileDocument.getLong("currentLessonIndex")?.toInt() ?: 0,
                    activeClassId = profileDocument.getString("activeClassId").orEmpty(),
                    notificationNewPrayerRequests = profileDocument.getBoolean("notificationNewPrayerRequests") != false,
                    notificationNewAssignments = profileDocument.getBoolean("notificationNewAssignments") != false,
                    notificationAssignmentReminders = profileDocument.getBoolean("notificationAssignmentReminders") != false,
                    notificationDiscussionReplies = profileDocument.getBoolean("notificationDiscussionReplies") != false,
                    notificationsEnabled = profileDocument.getBoolean("notificationsEnabled") != false,
                )

                if (classIds.isEmpty()) {
                    onSuccess(FormationOverview(profile, emptyList(), emptySet(), emptyList(), emptyList()))
                    return@addOnSuccessListener
                }

                val primaryClassId = profile.selectedClassId
                val assignmentsTask = firestore.collection("assignments")
                    .whereEqualTo("classId", primaryClassId)
                    .get()
                val completionsTask = firestore.collection("assignmentCompletions")
                    .whereEqualTo("userId", userId)
                    .get()
                val scheduleTask = firestore.collection("classSchedule")
                    .whereEqualTo("classId", primaryClassId)
                    .get()
                val promptsTask = firestore.collection("discussionPrompts")
                    .whereEqualTo("classId", primaryClassId)
                    .whereEqualTo("isActive", true)
                    .get()
                val prayerTask = firestore.collection("prayerRequests")
                    .whereEqualTo("classId", primaryClassId)
                    .get()

                assignmentsTask.addOnSuccessListener { assignmentSnapshot ->
                    completionsTask.addOnSuccessListener { completionSnapshot ->
                        scheduleTask.addOnSuccessListener { scheduleSnapshot ->
                            promptsTask.addOnSuccessListener { promptSnapshot ->
                                prayerTask.addOnSuccessListener { prayerSnapshot ->
                            val assignments = assignmentSnapshot.documents
                                .filter { it.getBoolean("isActive") != false }
                                .map { document ->
                                    val lessonLinks = document.get("lessonLinks") as? List<*>
                                    val parsedLessonLinks = lessonLinks.orEmpty()
                                        .mapNotNull { it as? Map<*, *> }
                                        .mapNotNull {
                                            val id = (it["lessonId"] as? String).orEmpty().trim()
                                            if (id.isBlank()) null else AssignmentLessonLink(id, (it["lessonTitle"] as? String).orEmpty().trim())
                                        }
                                    val legacyLessonId = document.getString("lessonId").orEmpty().trim()
                                    val legacyLessonTitle = document.getString("lessonTitle").orEmpty().trim()
                                    val linkedLessons = parsedLessonLinks.ifEmpty {
                                        if (legacyLessonId.isBlank()) emptyList() else listOf(AssignmentLessonLink(legacyLessonId, legacyLessonTitle))
                                    }
                                    val firstLesson = linkedLessons.firstOrNull()
                                    val readings = (document.get("readings") as? List<*>)
                                        .orEmpty()
                                        .mapNotNull { it as? Map<*, *> }
                                        .map {
                                            AssignmentReading(
                                                id = (it["id"] as? String).orEmpty().ifBlank { java.util.UUID.randomUUID().toString() },
                                                title = (it["title"] ?: it["readingTitle"]) as? String ?: "Reading",
                                                text = (it["text"] ?: it["readingText"]) as? String ?: "",
                                            )
                                        }
                                        .filter { it.title.isNotBlank() || it.text.isNotBlank() }
                                        .ifEmpty {
                                            val title = document.getString("readingTitle").orEmpty()
                                            val text = document.getString("readingText").orEmpty()
                                            if (title.isNotBlank() || text.isNotBlank()) {
                                                listOf(AssignmentReading("legacy-reading", title.ifBlank { "Reading" }, text))
                                            } else {
                                                emptyList()
                                            }
                                        }
                                    Assignment(
                                        id = document.id,
                                        classId = document.getString("classId").orEmpty(),
                                        title = document.getString("title").orEmpty(),
                                        lessonId = firstLesson?.lessonId.orEmpty(),
                                        lessonTitle = firstLesson?.lessonTitle.orEmpty().ifBlank { document.getString("title").orEmpty() },
                                        lessonLinks = linkedLessons,
                                        instructions = document.getString("instructions").orEmpty(),
                                        readings = readings,
                                        isActive = document.getBoolean("isActive") != false,
                                        dueAt = document.getTimestamp("dueAt"),
                                    )
                                }
                                .sortedBy { it.dueAt?.seconds ?: Long.MAX_VALUE }

                            val classCompletionDocuments = completionSnapshot.documents.filter {
                                it.getString("classId").isNullOrBlank() || it.getString("classId") == primaryClassId
                            }
                            val completedIds = classCompletionDocuments
                                .filter { it.getBoolean("isCompleted") == true }
                                .mapNotNull { it.getString("assignmentId") }
                                .toSet()
                            val completionRecords = classCompletionDocuments.map { document ->
                                AssignmentCompletion(
                                    assignmentId = document.getString("assignmentId").orEmpty(),
                                    userId = document.getString("userId").orEmpty(),
                                    studentName = document.getString("studentName").orEmpty(),
                                    isCompleted = document.getBoolean("isCompleted") == true,
                                    parentAssignmentId = document.getString("parentAssignmentId").orEmpty(),
                                    assignmentItemId = document.getString("assignmentItemId").orEmpty(),
                                    assignmentItemTitle = document.getString("assignmentItemTitle").orEmpty(),
                                    assignmentItemType = document.getString("assignmentItemType").orEmpty(),
                                    classId = document.getString("classId").orEmpty(),
                                )
                            }

                            val schedule = scheduleSnapshot.documents.map { document ->
                                ScheduleItem(
                                    id = document.id,
                                    classId = document.getString("classId").orEmpty(),
                                    topic = document.getString("topic").orEmpty(),
                                    details = document.getString("details").orEmpty(),
                                    date = document.getTimestamp("date"),
                                    sortOrder = document.getLong("sortOrder"),
                                )
                            }.sortedWith(scheduleItemComparator)

                            val prompts = promptSnapshot.documents
                                .filter { it.getBoolean("isActive") != false }
                                .map { document ->
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
                                }
                                .sortedBy { it.title.lowercase() }

                            val now = Timestamp.now().seconds
                            val prayerDocuments = prayerSnapshot.documents
                            val loadedPrayers = prayerDocuments.map { document ->
                                PrayerRequest(
                                    id = document.id,
                                    title = document.getString("title").orEmpty(),
                                    details = document.getString("details").orEmpty(),
                                    requesterName = document.getString("requesterName").orEmpty(),
                                    requesterId = document.getString("requesterId").orEmpty(),
                                    requesterEmail = document.getString("requesterEmail").orEmpty(),
                                    reactions = (document.get("reactions") as? Map<*, *>).orEmpty()
                                        .mapNotNull { (key, value) -> if (key is String && value is String) key to value else null }.toMap(),
                                    createdAt = document.getTimestamp("createdAt"),
                                    expiresAt = document.getTimestamp("expiresAt"),
                                )
                            }
                            val prayers = PrayerRequestPolicy.recentActive(loadedPrayers, now, { it.expiresAt?.seconds }, { it.createdAt?.seconds })
                            prayerDocuments.filter { (it.getTimestamp("expiresAt")?.seconds ?: Long.MIN_VALUE) <= now }.forEach { expired ->
                                expired.reference.delete()
                            }

                            onSuccess(
                                FormationOverview(
                                    profile = profile,
                                    assignments = assignments,
                                    completedAssignmentIds = completedIds,
                                    assignmentCompletions = completionRecords,
                                    schedule = schedule,
                                    discussionPrompts = prompts,
                                    prayerRequests = prayers,
                                ),
                            )
                                }.addOnFailureListener(onError)
                            }.addOnFailureListener(onError)
                        }.addOnFailureListener(onError)
                    }.addOnFailureListener(onError)
                }.addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    fun setAssignmentCompleted(
        profile: UserProfile,
        assignment: Assignment,
        completed: Boolean,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val user = auth.currentUser ?: return onError(IllegalStateException("Please sign in before updating an assignment."))
        val classId = AuthenticatedWritePolicy.primaryClassId(profile)
        if (classId.isBlank()) return onError(IllegalStateException("Please join a class before updating assignments."))
        val userId = user.uid
        val completionId = "${assignment.id}_$userId"
        val completion = mutableMapOf<String, Any>(
            "assignmentId" to assignment.id,
            "classId" to classId,
            "isCompleted" to completed,
            "studentName" to profile.displayName,
            "updatedAt" to FieldValue.serverTimestamp(),
            "userId" to userId,
        )
        if (completed) completion["completedAt"] = FieldValue.serverTimestamp()

        firestore.collection("assignmentCompletions")
            .document(completionId)
            .set(completion, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun setReadingCompleted(
        profile: UserProfile,
        assignment: Assignment,
        reading: AssignmentReading,
        completed: Boolean,
        completedReadingIds: Set<String>,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val user = auth.currentUser ?: return onError(IllegalStateException("Please sign in before updating an assignment."))
        val classId = AuthenticatedWritePolicy.primaryClassId(profile)
        if (classId.isBlank()) return onError(IllegalStateException("Please join a class before updating assignments."))
        val userId = user.uid
        val readingKey = "${assignment.id}__reading__${reading.id}"
        val readingDocument = firestore.collection("assignmentCompletions").document("${readingKey}_$userId")
        val parentDocument = firestore.collection("assignmentCompletions").document("${assignment.id}_$userId")
        val parentCompleted = InstructorReadinessCalculator.parentCompletedAfterReadingChange(assignment.readings.map { it.id }, reading.id, completed, completedReadingIds)
        val common = mapOf(
            "userId" to userId,
            "studentName" to profile.displayName,
            "classId" to classId,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        val readingData = common + mapOf(
            "assignmentId" to readingKey,
            "parentAssignmentId" to assignment.id,
            "assignmentItemId" to reading.id,
            "assignmentItemTitle" to reading.title.trim(),
            "assignmentItemType" to "reading",
            "isCompleted" to completed,
        ) + if (completed) mapOf("completedAt" to FieldValue.serverTimestamp()) else emptyMap()
        val parentData = common + mapOf("assignmentId" to assignment.id, "isCompleted" to parentCompleted) +
            if (parentCompleted) mapOf("completedAt" to FieldValue.serverTimestamp()) else emptyMap()
        firestore.runBatch { batch ->
            batch.set(readingDocument, readingData, SetOptions.merge())
            batch.set(parentDocument, parentData, SetOptions.merge())
        }.addOnSuccessListener { onSuccess() }.addOnFailureListener(onError)
    }

    fun markLessonComplete(
        lessonId: String,
        badgeIds: List<String>,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val userId = auth.currentUser?.uid ?: return onError(IllegalStateException("Please sign in before updating lesson progress."))
        val updates = mutableMapOf<String, Any>(
            "completedLessons" to FieldValue.arrayUnion(lessonId),
        )
        val cleanedBadgeIds = FormationProgressWritePolicy.normalizedBadgeIds(badgeIds)
        if (cleanedBadgeIds.isNotEmpty()) {
            updates["earnedBadges"] = FieldValue.arrayUnion(*cleanedBadgeIds.toTypedArray())
        }

        firestore.collection("userProfiles").document(userId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun setPrayerMemorized(
        prayerId: String,
        memorized: Boolean,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val userId = auth.currentUser?.uid ?: return onError(IllegalStateException("Please sign in before updating prayer progress."))
        val cleanedPrayerId = FormationProgressWritePolicy.normalizedPrayerId(prayerId)
            ?: return onSuccess()
        val value = if (memorized) FieldValue.arrayUnion(cleanedPrayerId) else FieldValue.arrayRemove(cleanedPrayerId)
        firestore.collection("userProfiles").document(userId)
            .update("memorizedPrayerIds", value)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun setPrayerSelected(
        prayerId: String,
        selected: Boolean,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val userId = auth.currentUser?.uid ?: return onError(IllegalStateException("Please sign in before updating Selected Prayers."))
        val cleanedPrayerId = FormationProgressWritePolicy.normalizedPrayerId(prayerId)
            ?: return onSuccess()
        val value = if (selected) FieldValue.arrayUnion(cleanedPrayerId) else FieldValue.arrayRemove(cleanedPrayerId)
        firestore.collection("userProfiles").document(userId)
            .update("selectedPrayerIds", value)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun createPrayerRequest(
        profile: UserProfile,
        title: String,
        details: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val user = auth.currentUser
        val classId = AuthenticatedWritePolicy.primaryClassId(profile)
        PrayerRequestPolicy.creationError(user?.uid, title, classId)?.let { return onError(IllegalStateException(it)) }
        val expires = PrayerRequestPolicy.expirationDate()
        firestore.collection("prayerRequests").add(
            mapOf(
                "title" to title.trim(),
                "details" to details.trim(),
                "classId" to classId,
                "requesterId" to user!!.uid,
                "requesterName" to profile.displayName,
                "requesterEmail" to user.email.orEmpty(),
                "reactions" to emptyMap<String, String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to Timestamp(expires),
            ),
        ).addOnSuccessListener { onSuccess() }.addOnFailureListener(onError)
    }

    fun setPrayerReaction(
        request: PrayerRequest,
        reaction: String?,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val userId = auth.currentUser?.uid
            ?: return onError(IllegalStateException("Please sign in before acknowledging a prayer request."))
        if (userId == request.requesterId) return onError(IllegalStateException("Your classmates can acknowledge your prayer request."))
        if (reaction != null && reaction !in setOf("praying", "with_you", "amen")) {
            return onError(IllegalArgumentException("Please choose a valid prayer response."))
        }
        firestore.collection("prayerRequests").document(request.id).update(
            FieldPath.of("reactions", userId), reaction ?: FieldValue.delete(),
            "reactionUpdatedAt", FieldValue.serverTimestamp(),
        ).addOnSuccessListener { onSuccess() }.addOnFailureListener(onError)
    }

    fun markRosaryMysteryComplete(
        mysteryId: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val userId = auth.currentUser?.uid ?: return onError(IllegalStateException("Please sign in before updating Rosary progress."))
        firestore.collection("userProfiles").document(userId).update(
            mapOf(
                "completedMysteries" to FieldValue.arrayUnion(mysteryId),
                "earnedBadges" to FieldValue.arrayUnion("rosary-$mysteryId"),
            ),
        ).addOnSuccessListener { onSuccess() }.addOnFailureListener(onError)
    }
}

private fun Any?.asStringList(): List<String> =
    (this as? List<*>)?.filterIsInstance<String>().orEmpty()

object FormationProgressWritePolicy {
    fun normalizedPrayerId(prayerId: String): String? = prayerId.trim().takeIf(String::isNotEmpty)

    fun normalizedBadgeIds(badgeIds: List<String>): List<String> =
        badgeIds.filter(String::isNotEmpty).distinct()
}
