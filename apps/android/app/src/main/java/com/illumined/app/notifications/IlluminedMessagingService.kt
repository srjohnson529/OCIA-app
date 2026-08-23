package com.illumined.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.illumined.app.MainActivity
import com.illumined.app.R

@Suppress("OVERRIDE_DEPRECATION")
class IlluminedMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        IlluminedNotificationChannel.create(this)
    }

    override fun onNewToken(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("userProfiles").document(userId).get()
            .addOnSuccessListener { profile ->
                val classIds = (profile.get("classIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
                val classId = profile.getString("activeClassId")?.takeIf { it in classIds }
                    ?: classIds.firstOrNull()
                    ?: profile.getString("classId").orEmpty()
                val enabled = NotificationManagerCompat.from(this).areNotificationsEnabled() &&
                    (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                NotificationRegistrar().save(classId, token, notificationsEnabled = enabled)
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val content = resolveNotificationContent(message.notification?.title, message.notification?.body, message.data)
        IlluminedNotificationChannel.create(this)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val opensDailyFormation = message.data["type"] == "daily_formation"
        val openApp = PendingIntent.getActivity(this, if (opensDailyFormation) 41 else 0, Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("openDailyFormation", opensDailyFormation)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, NotificationChannelPolicy.ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setContentIntent(openApp)
        if (opensDailyFormation) {
            builder.setColor(liturgicalNotificationColor(message.data["colorCode"]))
                .setColorized(true)
        }
        val notification = builder.build()
        NotificationManagerCompat.from(this).notify((message.messageId ?: "${content.title}:${content.body}").hashCode(), notification)
    }

}

private fun liturgicalNotificationColor(code: String?): Int = when (code?.uppercase()) {
    "WHITE" -> Color.rgb(245, 240, 220)
    "GOLD" -> Color.rgb(201, 155, 71)
    "RED" -> Color.rgb(150, 25, 30)
    "PURPLE" -> Color.rgb(90, 45, 110)
    "ROSE" -> Color.rgb(220, 120, 145)
    else -> Color.rgb(30, 115, 65)
}
