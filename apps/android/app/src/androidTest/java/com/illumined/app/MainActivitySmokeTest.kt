package com.illumined.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun authenticatedShellDisplaysSharedBrandAndNavigation() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Illumined. Being, Truth, Goodness.")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Illumined. Being, Truth, Goodness.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Home", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Lessons", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Discussion", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Formation", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("More", useUnmergedTree = true).assertIsDisplayed()
    }
}
