package com.illumined.app.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.google.firebase.firestore.FirebaseFirestore
import com.illumined.app.data.UserProfile

@Composable
internal fun pendingClassroomRequests(profile: UserProfile?): Map<String, Int> {
    val ids = if(profile?.isInstructor == true) profile.activeClassIds.filter { it !in profile.inactiveClassIds && it !in profile.removedClassIds } else emptyList()
    var counts by remember(profile?.userId, ids) { mutableStateOf(emptyMap<String, Int>()) }
    DisposableEffect(profile?.userId, ids) {
        val listeners = ids.map { id -> FirebaseFirestore.getInstance().collection("classroomJoinRequests").whereEqualTo("classId", id).addSnapshotListener { snapshot, error ->
            counts = counts + (id to if(error != null) -1 else snapshot?.documents.orEmpty().count { it.getString("status") == "pending" })
        } }
        onDispose { listeners.forEach { it.remove() } }
    }
    return counts
}

@Composable
internal fun ClassroomRequestInbox(counts: Map<String, Int>, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    BackHandler { if(selected != null) selected=null else onBack() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        TextButton(onClick={if(selected != null)selected=null else onBack()}) { Text(classroomT("‹ Back", "‹ Atrás")) }
        Text(classroomT("Student Join Requests", "Solicitudes de ingreso"), style=MaterialTheme.typography.headlineSmall)
        if(selected != null && counts.containsKey(selected)) {
            Text(selected!!)
            ClassroomApprovalQueue(selected!!)
        } else {
            if(counts.values.all { it == 0 }) Text(classroomT("No pending requests.", "No hay solicitudes pendientes."))
            counts.filterValues { it != 0 }.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (id, count) ->
                OutlinedButton(onClick={selected=id}, modifier=Modifier.fillMaxWidth()) { Text(if(count < 0) "$id · " + classroomT("Unable to load requests", "No se pudieron cargar") else "$id · $count") }
            }
        }
    }
}
