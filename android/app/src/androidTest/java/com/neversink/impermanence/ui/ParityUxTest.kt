package com.neversink.impermanence.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neversink.impermanence.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParityUxTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun newDayStartsAtMidnight() {
        openNewDayScreen()
        assertTrue(
            "Expected midnight default start time to be shown",
            hasNodeWithText("12:00 AM") || hasNodeWithText("00:00")
        )
        composeRule.onNodeWithContentDescription("Cancel").performClick()
    }

    @Test
    fun dayEditExposesThemeAndBellControls() {
        openNewDayScreen()
        composeRule.onNodeWithText("Default bell").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Increase chimes").performClick()
        composeRule.onNodeWithText("Chimes: 2").assertIsDisplayed()
        composeRule.onAllNodesWithText("Theme").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Teal").onFirst().assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Cancel").performClick()
    }

    @Test
    fun quickSitDisablesCloseWhileRunning() {
        composeRule.onNodeWithContentDescription("Quick Sit").performClick()
        composeRule.waitUntil(5_000) { hasNodeWithText("Quick Sit") }

        composeRule.onNodeWithText("Start").performClick()
        composeRule.waitUntil(5_000) { hasNodeWithText("Cancel") }

        composeRule.onNodeWithContentDescription("Close").assertIsNotEnabled()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Quick Sit").assertIsDisplayed()

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithContentDescription("Close").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Close").performClick()
    }

    private fun openNewDayScreen() {
        composeRule.waitUntil(10_000) { hasNodeWithContentDescription("New Day") }
        composeRule.onNodeWithContentDescription("New Day").performClick()
        composeRule.waitUntil(5_000) { hasNodeWithText("Start of day") }
    }

    private fun hasNodeWithText(text: String): Boolean {
        return composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun hasNodeWithContentDescription(contentDescription: String): Boolean {
        return runCatching {
            composeRule.onNodeWithContentDescription(contentDescription).fetchSemanticsNode()
            true
        }.getOrDefault(false)
    }
}
