package com.illumined.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelPolicy {
    const val ID = "illumined_class_updates"
    const val NAME = "Class updates"
    const val DESCRIPTION = "Announcements, assignments, prayer requests, and discussion activity from Illumined"
}

object IlluminedNotificationChannel {
    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NotificationChannelPolicy.ID,
            NotificationChannelPolicy.NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = NotificationChannelPolicy.DESCRIPTION
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
