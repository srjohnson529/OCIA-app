package com.illumined.app.ui

internal object AuthPresentation {
    const val IntroDescription = "Continue your OCIA formation with lessons, prayer, and classroom conversation."
    const val ResetTitle = "Reset Password"
    const val ResetDescription = "Enter the email connected to your Illumined account. Firebase will send a secure link for setting a new password."

    fun introTitle(creatingAccount: Boolean): String = if (creatingAccount) "Create your account" else "Welcome back"

    fun resetVisibleAfterSuccessfulSend(currentlyVisible: Boolean): Boolean = currentlyVisible
}
