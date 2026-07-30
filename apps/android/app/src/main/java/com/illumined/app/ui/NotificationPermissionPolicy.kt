package com.illumined.app.ui

internal object NotificationPermissionPolicy {
    fun statusText(enabled: Boolean, permissionRequested: Boolean) = when {
        enabled -> "Enabled"
        permissionRequested -> "Off"
        else -> "Not set up"
    }

    fun showSettings(enabled: Boolean, permissionRequested: Boolean, runtimePermissionRequired: Boolean) =
        !enabled && (permissionRequested || !runtimePermissionRequired)
}
