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

class MainActivity : ComponentActivity() {
    private var inviteUri by mutableStateOf<String?>(null)
    private var dailyFormationOpenRequest by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inviteUri = intent?.dataString
        if (intent.opensDailyFormation()) dailyFormationOpenRequest = 1
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
        if (intent.opensDailyFormation()) {
            dailyFormationOpenRequest += 1
            intent.removeExtra("openDailyFormation")
            intent.removeExtra("type")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applySystemBarAppearance()
    }

    private fun applySystemBarAppearance() {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }
}

private fun Intent?.opensDailyFormation(): Boolean =
    this?.getBooleanExtra("openDailyFormation", false) == true ||
        this?.getStringExtra("type") == "daily_formation"
