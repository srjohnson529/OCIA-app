package com.illumined.app.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.illumined.app.data.ChatMessage
import com.illumined.app.data.UserProfile

private fun markerKey(user: String, room: String) = org.json.JSONArray(listOf(user, room)).toString()
private fun nanos(time: Timestamp) = time.seconds * 1_000_000_000L + time.nanoseconds

@Composable
internal fun classroomUnreadCount(profile: UserProfile?): Int {
    val prefs = LocalContext.current.getSharedPreferences("classroom-chat-read", Context.MODE_PRIVATE)
    var count by remember(profile?.userId, profile?.selectedClassId) { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val changed = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        prefs.registerOnSharedPreferenceChangeListener(changed)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(changed) }
    }
    DisposableEffect(profile, revision) {
        var active = true
        count = 0
        val room = profile?.selectedClassId.orEmpty()
        val key = markerKey(profile?.userId.orEmpty(), room)
        if (!prefs.contains(key)) prefs.edit().putLong(key, nanos(Timestamp.now())).apply()
        val since = prefs.getLong(key, 0)
        val listener = if (profile != null && room.isNotBlank() && room !in profile.inactiveClassIds && room !in profile.removedClassIds)
            FirebaseFirestore.getInstance().collection("chatMessages").whereEqualTo("classId", room)
                .whereGreaterThan("timestamp", Timestamp(since / 1_000_000_000L, (since % 1_000_000_000L).toInt()))
                .addSnapshotListener { snapshot, error ->
                    if (active) count = if (error != null) 0 else snapshot?.documents?.count { it.getString("senderId") != profile.userId } ?: 0
                } else null
        onDispose { active = false; listener?.remove() }
    }
    return count
}

@Composable
internal fun markClassroomMessagesRead(userId: String, room: String, messages: List<ChatMessage>, visible: Boolean) {
    val prefs = LocalContext.current.getSharedPreferences("classroom-chat-read", Context.MODE_PRIVATE)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(messages, visible, resumed, userId, room) {
        if (visible && resumed && room.isNotBlank()) {
            val latest = messages.mapNotNull { it.timestamp }.maxOfOrNull(::nanos) ?: return@LaunchedEffect
            val key = markerKey(userId, room)
            if (latest > prefs.getLong(key, 0)) prefs.edit().putLong(key, latest).apply()
        }
    }
}
