package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthPresentationTest {
    @Test
    fun `intro and reset copy match iOS without an eyebrow`() {
        assertEquals("Welcome back", AuthPresentation.introTitle(false))
        assertEquals("Create your account", AuthPresentation.introTitle(true))
        assertEquals("Reset Password", AuthPresentation.ResetTitle)
        assertEquals("Continue your OCIA formation with lessons, prayer, and classroom conversation.", AuthPresentation.IntroDescription)
    }

    @Test
    fun `successful reset keeps reset screen visible for confirmation`() {
        assertTrue(AuthPresentation.resetVisibleAfterSuccessfulSend(true))
    }
}
