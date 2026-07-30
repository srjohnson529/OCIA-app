package com.illumined.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountSymbolPresentationTest {
    @Test fun `account covers every current iOS symbol intent`() {
        assertEquals(listOf("Avatar", "Person", "Envelope", "ClassMembers", "Instructor", "Student", "Book", "Rosette", "ProfileAlert", "SignOut"), AccountSymbolKind.entries.map { it.name })
    }
}
