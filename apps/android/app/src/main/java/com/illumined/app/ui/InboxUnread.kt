package com.illumined.app.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.illumined.app.data.InstructorConversation
import com.illumined.app.data.InstructorInboxRepository
import com.illumined.app.data.UserProfile

@Composable
internal fun inboxUnreadCount(profile: UserProfile?): Int {
    val preferences = LocalContext.current.getSharedPreferences("instructor-inbox", Context.MODE_PRIVATE)
    var revision by remember { mutableIntStateOf(0) }
    var rows by remember(profile) { mutableStateOf(emptyList<InstructorConversation>()) }
    DisposableEffect(profile) {
        var active = true
        val room = profile?.selectedClassId.orEmpty()
        val listener = if (profile != null && room.isNotBlank() && room !in profile.removedClassIds && room !in profile.inactiveClassIds)
            InstructorInboxRepository().conversations(room, profile.userId, profile.isInstructor,
                { if (active) rows = it }, { if (active) rows = emptyList() }) else null
        onDispose { active = false; listener?.remove() }
    }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return remember(rows, revision, profile) {
        rows.count { row ->
            val time = row.updatedAt?.let { it.seconds * 1_000_000_000L + it.nanoseconds } ?: 0L
            row.lastSenderId != profile?.userId && time > preferences.getLong("${profile?.userId}:${row.id}", 0)
        }
    }
}
