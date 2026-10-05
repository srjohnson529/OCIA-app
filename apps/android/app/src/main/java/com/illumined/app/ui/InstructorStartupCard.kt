package com.illumined.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.illumined.app.data.UserProfile
import com.illumined.app.ui.theme.IlluminedThemeTokens
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.tasks.Task

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

@Composable
fun InstructorStartupCard(profile: UserProfile?, canPresent: Boolean) {
    var update by remember(profile?.userId) { mutableStateOf<DocumentSnapshot?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val db = remember { FirebaseFirestore.getInstance() }
    fun t(en: String, es: String) = if (java.util.Locale.getDefault().language == "es") es else en
    LaunchedEffect(profile?.userId, profile?.isInstructor) {
        update = null
        if (profile?.isInstructor != true) return@LaunchedEffect
        try {
            val latest = db.collection("instructorUpdates").orderBy("createdAt", Query.Direction.DESCENDING).limit(1).get().await().documents.firstOrNull()
            if (latest?.getBoolean("showOnStartup") == true && latest.getBoolean("withdrawn") != true && (latest.getLong("expiresAtMs") ?: Long.MAX_VALUE) > System.currentTimeMillis()) {
                val receipt = db.collection("userProfiles").document(profile.userId).collection("instructorUpdateReceipts").document(latest.id).get().await()
                if (!receipt.exists()) update = latest
            }
        } catch (_: Exception) { /* Supplemental content must not block app access. */ }
    }
    val current = update
    if (current != null && canPresent && profile?.isInstructor == true && (current.getLong("expiresAtMs") ?: Long.MAX_VALUE) > System.currentTimeMillis()) AlertDialog(
        onDismissRequest = { update = null }, containerColor = IlluminedThemeTokens.Blue,
        titleContentColor = Color.White, textContentColor = Color.White,
        title = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t("ILLUMINED UPDATE", "NOVEDADES DE ILLUMINED"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(current.getString("title").orEmpty(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.width(86.dp).height(3.dp).background(IlluminedThemeTokens.Gold))
        } },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text(current.getString("message").orEmpty(), fontSize = 18.sp, lineHeight = 27.sp)
            if (error) Text(t("Could not save. Try again or close for now.", "No se pudo guardar. Inténtalo de nuevo o cierra por ahora."))
        } },
        confirmButton = { Button(enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = IlluminedThemeTokens.Gold, contentColor = Color.Black), onClick = {
            saving = true
            db.collection("userProfiles").document(profile.userId).collection("instructorUpdateReceipts").document(current.id).set(mapOf("dismissedAt" to FieldValue.serverTimestamp()))
                .addOnSuccessListener { saving = false; update = null }
                .addOnFailureListener { saving = false; error = true }
        }) { Text(t("Got it", "Entendido")) } },
        dismissButton = { TextButton(onClick = { update = null }) { Text(t("Close for now", "Cerrar por ahora"), color = Color.White) } }
    )
}
