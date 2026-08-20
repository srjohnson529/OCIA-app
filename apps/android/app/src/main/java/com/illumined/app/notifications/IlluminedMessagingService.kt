package com.illumined.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, NotificationChannelPolicy.ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(this).notify((message.messageId ?: "${content.title}:${content.body}").hashCode(), notification)
    }

}
