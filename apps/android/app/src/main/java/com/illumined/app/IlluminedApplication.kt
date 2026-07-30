package com.illumined.app

import android.app.Application
import com.illumined.app.notifications.IlluminedNotificationChannel

class IlluminedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        IlluminedNotificationChannel.create(this)
    }
}
