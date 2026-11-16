package com.neversink.impermanence.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neversink.impermanence.MainActivity
import org.junit.Assume.assumeTrue
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
class StartDayFlowTest {

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
    fun startDayFromDetailShowsActiveScreen() {
        val dayName = findSeedDayName()
        assumeTrue("No seeded day found to exercise start-day flow", dayName != null)

        waitFor("the day in the list") { hasNodeWithText(dayName!!) }
        composeRule.onNodeWithText(dayName!!).performClick()
        waitFor("the day detail screen") { hasNodeWithText("Start or Resume Day") }
        composeRule.onNodeWithText("Start or Resume Day").performClick()
        waitFor("the active day screen") { hasNodeWithContentDescription("Manual bell controls") }
        composeRule.onNodeWithContentDescription("Manual bell controls").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Manual bell controls").performClick()
        composeRule.onNodeWithText("Ring now").assertIsDisplayed()
    }

    /** Waits for [condition]; on timeout, fails naming the step and showing what is on screen. */
    private fun waitFor(step: String, timeoutMillis: Long = 15_000L, condition: () -> Boolean) {
        val ok = runCatching { composeRule.waitUntil(timeoutMillis, condition) }.isSuccess
        if (!ok) {
            val tree = runCatching { composeRule.onRoot().printToString() }.getOrDefault("(no tree)")
            throw AssertionError("Timed out waiting for $step. Screen:\n$tree")
        }
    }

    private fun findSeedDayName(timeoutMillis: Long = 10_000L): String? {
        var found: String? = null
        runCatching {
            composeRule.waitUntil(timeoutMillis) {
                when {
                    hasNodeWithText("Opening Day") -> {
                        found = "Opening Day"
                        true
                    }

                    hasNodeWithText("Full Day") -> {
                        found = "Full Day"
                        true
                    }

                    else -> false
                }
            }
        }
        return found
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
