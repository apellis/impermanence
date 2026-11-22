package com.neversink.impermanence.ui

import android.Manifest
import android.os.Build
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.neversink.impermanence.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class SegmentReorderTest {
    private val permissionRule: TestRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TestRule { base: Statement, _: Description -> base }
        }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(permissionRule).around(composeRule)

    @Test
    fun dragHandleMovesASegmentAndMoveUpActionMovesItBack() {
        composeRule.waitUntil(15_000) { has("New Day", byDescription = true) }
        composeRule.onNodeWithContentDescription("New Day").performClick()
        composeRule.waitUntil(15_000) { has("Start of day") }

        addNamedSegment("Alpha")
        addNamedSegment("Bravo")
        assertTrue("Alpha starts above Bravo", top("Alpha") < top("Bravo"))

        // Drag Alpha's handle down past Bravo, in small steps so the list lays out between moves.
        // The handle sits inside the clickable row, so its tag is only in the unmerged tree.
        val handle = composeRule.onAllNodesWithTag("segment-drag-handle", useUnmergedTree = true)[0]
        handle.performTouchInput { down(center) }
        repeat(12) {
            handle.performTouchInput { moveBy(Offset(0f, 16f)) }
            composeRule.waitForIdle()
        }
        handle.performTouchInput { up() }
        composeRule.waitForIdle()
        assertTrue("Alpha moved below Bravo", top("Alpha") > top("Bravo"))

        // The screen-reader action moves it back up one place.
        val alpha = composeRule.onNodeWithText("Alpha")
        val moveUp = alpha.fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first { it.label == "Move up" }
        composeRule.runOnUiThread { moveUp.action() }
        composeRule.waitForIdle()
        assertTrue("Alpha is back above Bravo", top("Alpha") < top("Bravo"))

        composeRule.onNodeWithContentDescription("Cancel").performClick()
        composeRule.waitUntil(15_000) { has("Discard changes?") }
        composeRule.onNodeWithText("Discard").performClick()
    }

    private fun addNamedSegment(name: String) {
        composeRule.onNodeWithText("Add Segment").performScrollTo().performClick()
        composeRule.waitUntil(15_000) { has("Untitled segment") }
        composeRule.onAllNodesWithText("Untitled segment")[0].performClick()
        composeRule.waitUntil(15_000) { has("Name") }
        composeRule.onNodeWithText("Name").performTextInput(name)
        composeRule.waitForIdle()
        // System back: the first may only hide the keyboard, the next dismisses the sheet. Espresso's
        // pressBack can't be used, because the sheet is its own window and Espresso waits for focus.
        // After each press, give the sheet time to animate away before pressing again: a press that
        // lands after the sheet is gone would go to the editor instead (it asks to discard changes).
        repeat(4) {
            if (has("Use day default bell")) {
                InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                composeRule.waitForIdle()
                waitFor(2_000) { !has("Use day default bell") }
            }
        }
        composeRule.waitUntil(15_000) { has(name) && !has("Use day default bell") }
    }

    /** Like waitUntil, but returns false instead of failing when [condition] stays false. */
    private fun waitFor(timeoutMillis: Long, condition: () -> Boolean): Boolean =
        runCatching { composeRule.waitUntil(timeoutMillis) { condition() } }.isSuccess

    private fun top(text: String): Float =
        composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    private fun has(text: String, byDescription: Boolean = false): Boolean = runCatching {
        if (byDescription) {
            composeRule.onNodeWithContentDescription(text).fetchSemanticsNode()
        } else {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() || error("none")
        }
        true
    }.getOrDefault(false)
}
