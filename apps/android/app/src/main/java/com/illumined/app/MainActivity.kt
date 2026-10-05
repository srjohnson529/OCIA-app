package com.illumined.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.illumined.app.ui.IlluminedApp

data class DailyFormationOpenRequest(
    val id: Long,
    val classId: String?,
    val date: String?,
)

class MainActivity : ComponentActivity() {
    private var inviteUri by mutableStateOf<String?>(null)
    private var dailyFormationOpenRequest by mutableStateOf<DailyFormationOpenRequest?>(null)
    private var dailyFormationRequestSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inviteUri = intent?.dataString
        consumeMessageIntent(intent)
        consumeDailyFormationIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent { IlluminedApp(inviteUri = inviteUri, dailyFormationOpenRequest = dailyFormationOpenRequest) }
        applySystemBarAppearance()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        inviteUri = intent.dataString
        consumeMessageIntent(intent)
        consumeDailyFormationIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applySystemBarAppearance()
    }

    private fun applySystemBarAppearance() {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }

    private fun consumeDailyFormationIntent(intent: Intent?) {
        if (!intent.opensDailyFormation()) return
        dailyFormationRequestSequence += 1
        dailyFormationOpenRequest = DailyFormationOpenRequest(
            id = dailyFormationRequestSequence,
            classId = intent?.getStringExtra("classId")?.trim()?.takeIf(String::isNotEmpty),
            date = intent?.getStringExtra("date")?.trim()?.takeIf(String::isNotEmpty),
        )
        intent?.removeExtra("openDailyFormation")
        intent?.removeExtra("type")
        intent?.removeExtra("classId")
        intent?.removeExtra("date")
    }

    private fun consumeMessageIntent(intent: Intent?) {
        val data = listOf("type", "classId", "recipientId").mapNotNull { key -> intent?.getStringExtra(key)?.let { key to it } }.toMap()
        com.illumined.app.notifications.MessageNotificationNavigation.accept(data)
    }
}

private fun Intent?.opensDailyFormation(): Boolean =
    this?.getBooleanExtra("openDailyFormation", false) == true ||
        this?.getStringExtra("type") == "daily_formation"
