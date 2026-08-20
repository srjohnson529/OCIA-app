package com.illumined.app.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class NotificationRegistrar(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun updateAllPreferences(enabled: Boolean, success: () -> Unit = {}, error: (Throwable) -> Unit = {}) {
        val userId = auth.currentUser?.uid
            ?: return error(IllegalStateException(NotificationRegistrationPolicy.SIGN_IN_REQUIRED))
        firestore.collection("userProfiles").document(userId).set(
            mapOf(
                "notificationNewPrayerRequests" to enabled,
                "notificationNewAssignments" to enabled,
                "notificationAssignmentReminders" to enabled,
                "notificationDiscussionReplies" to enabled,
                "notificationsEnabled" to enabled,
                "notificationPreferencesUpdatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).addOnSuccessListener { success() }.addOnFailureListener(error)
    }

    fun register(classId: String, success: () -> Unit, error: (Throwable) -> Unit) {
        messaging.token.addOnSuccessListener { token ->
            save(classId, token, success, error)
        }.addOnFailureListener(error)
    }

    fun save(classId: String, token: String, success: () -> Unit = {}, error: (Throwable) -> Unit = {}, notificationsEnabled: Boolean = true) {
        val userId = auth.currentUser?.uid
            ?: return error(IllegalStateException(NotificationRegistrationPolicy.SIGN_IN_REQUIRED))
        if (token.isBlank()) return error(IllegalArgumentException(NotificationRegistrationPolicy.TOKEN_REQUIRED))
        firestore.collection("userProfiles").document(userId).set(
            mapOf(
                "fcmTokens" to FieldValue.arrayUnion(token),
                "lastFcmToken" to token,
                "notificationPlatform" to "android",
                "notificationClassId" to classId,
                "notificationUpdatedAt" to FieldValue.serverTimestamp(),
                "notificationNewPrayerRequests" to notificationsEnabled,
                "notificationNewAssignments" to notificationsEnabled,
                "notificationAssignmentReminders" to notificationsEnabled,
                "notificationDiscussionReplies" to notificationsEnabled,
                "notificationsEnabled" to notificationsEnabled,
                "notificationPreferencesUpdatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).addOnSuccessListener { success() }.addOnFailureListener(error)
    }
}

object NotificationRegistrationPolicy {
    const val SIGN_IN_REQUIRED = "Please sign in before registering notifications."
    const val TOKEN_REQUIRED = "A valid notification token is required."
}
