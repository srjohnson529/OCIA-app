package com.illumined.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import java.util.Locale

object NotificationChannelPolicy {
    const val ID = "illumined_class_updates"
    const val NAME = "Class updates"
    const val DESCRIPTION = "Announcements, assignments, prayer requests, and discussion activity from Illumined"
}

object IlluminedNotificationChannel {
    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val isSpanish = Locale.getDefault().language == "es"
        val channel = NotificationChannel(
            NotificationChannelPolicy.ID,
            if (isSpanish) "Actualizaciones de la clase" else NotificationChannelPolicy.NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = if (isSpanish) {
                "Anuncios, tareas, peticiones de oración y actividad de debates de Illumined"
            } else {
                NotificationChannelPolicy.DESCRIPTION
            }
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
