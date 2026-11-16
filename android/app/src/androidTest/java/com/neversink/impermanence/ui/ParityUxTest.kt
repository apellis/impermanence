package com.neversink.impermanence.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neversink.impermanence.MainActivity
import org.junit.Assert.assertTrue
import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.neversink.impermanence.service.SessionEngine
import org.junit.After
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParityUxTest {

    // Granted up front so the notification permission prompt can't cover the screen under test.
    @get:Rule(order = 0)
    val notifications: TestRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TestRule { base, _ -> base }
        }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    // A session left running would make the next test's launch resume it.
    @After
    fun endSession() {
        SessionEngine.end(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SessionEngine.EndReason.LEFT_SCREEN
        )
    }

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
    fun themeAndSoundPickersOpen() {
        openNewDayScreen()
        composeRule.onAllNodesWithText("Teal").onFirst().performClick()
        composeRule.waitUntil(5_000) { hasNodeWithText("Indigo") }
        composeRule.onAllNodesWithText("Indigo").onFirst().performClick()
        composeRule.waitUntil(5_000) { hasNodeWithText("Indigo") && !hasNodeWithText("Periwinkle") }
        composeRule.onNodeWithContentDescription("Cancel").performClick()
        // Leaving with unsaved changes asks first.
        composeRule.waitUntil(15_000) { hasNodeWithText("Discard changes?") }
        composeRule.onNodeWithText("Discard").performClick()
        composeRule.waitUntil(15_000) { !hasNodeWithText("Discard changes?") && (hasNodeWithText("Opening Day") || hasNodeWithText("Full Day")) }
    }

    @Test
    fun deletingADayAsksFirst() {
        composeRule.waitUntil(10_000) { hasNodeWithText("Opening Day") || hasNodeWithText("Full Day") }
        composeRule.onAllNodesWithContentDescription("More options for", substring = true).onFirst().performClick()
        composeRule.waitUntil(15_000) { hasNodeWithText("Delete") }
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitUntil(5_000) { hasNodeWithText("This day and its segments will be removed.") }
        composeRule.onNodeWithText("Cancel").performClick()
        assertTrue(hasNodeWithText("Opening Day") || hasNodeWithText("Full Day"))
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
