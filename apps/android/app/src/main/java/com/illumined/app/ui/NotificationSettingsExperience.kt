package com.illumined.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.illumined.app.data.UserProfile
import com.illumined.app.notifications.NotificationRegistrar
import com.illumined.app.ui.theme.IlluminedThemeTokens
import java.text.DateFormat
import java.util.Date

@Composable
fun NotificationSettingsExperience(profile: UserProfile?, onBack: () -> Unit) {
    val context = LocalContext.current; val registrar = remember { NotificationRegistrar() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember { context.getSharedPreferences("illumined_notification_settings", android.content.Context.MODE_PRIVATE) }
    val runtimePermissionRequired = Build.VERSION.SDK_INT >= 33
    fun notificationsEnabled() = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (!runtimePermissionRequired || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    var enabled by remember { mutableStateOf(notificationsEnabled()) }
    var permissionRequested by remember { mutableStateOf(preferences.getBoolean("permission_requested", false)) }
    var working by remember { mutableStateOf(false) }; var status by remember { mutableStateOf<String?>(null) }; var error by remember { mutableStateOf<String?>(null) }
    var savedAt by remember { mutableStateOf(preferences.getLong("last_registered_at", 0L).takeIf { it > 0L }?.let(::Date)) }
    val requiresSettings = NotificationPermissionPolicy.showSettings(enabled, permissionRequested, runtimePermissionRequired)
    fun openSettings() {
        error = null; status = null
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
    fun register() { working = true; registrar.register(profile?.selectedClassId.orEmpty(), {
        val registeredAt = Date()
        working = false; enabled = true; savedAt = registeredAt
        preferences.edit().putLong("last_registered_at", registeredAt.time).apply()
        status = "Notifications are ready for ${profile?.selectedClassId?.ifBlank { "your class" } ?: "your class"}."
    }, { working = false; error = "Notification registration could not be saved." }) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRequested = true
        preferences.edit().putBoolean("permission_requested", true).apply()
        enabled = granted && notificationsEnabled()
        if (granted) register() else status = "Notifications are off. You can turn them on later in Android Settings."
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) enabled = notificationsEnabled() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(enabled, profile) {
        val current = profile ?: return@LaunchedEffect
        val allMatch = listOf(current.notificationsEnabled, current.notificationNewPrayerRequests, current.notificationNewAssignments, current.notificationAssignmentReminders, current.notificationDiscussionReplies).all { it == enabled }
        if (!allMatch) registrar.updateAllPreferences(enabled, error = { error = "Notification status could not be synchronized." })
    }
    Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(IlluminedThemeTokens.Parchment, IlluminedThemeTokens.Cream), radius = 1600f))) {
        NotificationPageHeading(onBack)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(.94f), shadowElevation = 6.dp, border = BorderStroke(1.dp, IlluminedThemeTokens.Gold.copy(.22f))) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoreMenuSymbol(MoreMenuSymbolKind.Notifications, IlluminedThemeTokens.Blue, Modifier.size(24.dp))
                Text("Notifications", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = IlluminedThemeTokens.Blue)
            }
            Text("Receive alerts for class announcements, assignments, prayer requests, and discussion activity. All alert types follow the notification status shown below.", color = IlluminedThemeTokens.SecondaryText)
            Row { Text("Status", fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text(NotificationPermissionPolicy.statusText(enabled, permissionRequested), color = if (enabled) IlluminedThemeTokens.Blue else IlluminedThemeTokens.SecondaryText, fontWeight = FontWeight.SemiBold) }
            savedAt?.let { Text("Last registered ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)}.", fontSize = 13.sp, color = IlluminedThemeTokens.SecondaryText) }
        } }
        status?.let { Text(it, Modifier.padding(horizontal = 4.dp), color = IlluminedThemeTokens.Blue, fontSize = 15.sp) }; error?.let { Text(it, Modifier.padding(horizontal = 4.dp), color = Color.Red, fontSize = 15.sp) }
        Button(onClick = {
            error = null; status = null
            when {
                requiresSettings -> openSettings()
                runtimePermissionRequired && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> register()
            }
        }, enabled = profile != null && !working, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text(if (working) "Setting Up…" else if (requiresSettings) "Open Android Settings" else if (enabled) "Refresh Notification Setup" else "Turn On Notifications", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        if (enabled) OutlinedButton(onClick = ::openSettings, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text("Manage in Android Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun NotificationPageHeading(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        Text("Notifications", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}
