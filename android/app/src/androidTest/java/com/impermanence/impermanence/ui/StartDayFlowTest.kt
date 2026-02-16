package com.impermanence.impermanence.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.impermanence.impermanence.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartDayFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startDayFromDetailShowsActiveScreen() {
        val dayName = findSeedDayName()
        assumeTrue("No seeded day found to exercise start-day flow", dayName != null)

        composeRule.waitUntil(5_000) { hasNodeWithText(dayName!!) }
        composeRule.onNodeWithText(dayName!!).performClick()
        composeRule.waitUntil(5_000) { hasNodeWithText("Start or Resume Day") }
        composeRule.onNodeWithText("Start or Resume Day").performClick()
        composeRule.waitUntil(5_000) { hasNodeWithContentDescription("Manual bell controls") }
        composeRule.onNodeWithContentDescription("Manual bell controls").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Manual bell controls").performClick()
        composeRule.onNodeWithText("Ring now").assertIsDisplayed()
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
