package com.neversink.impermanence.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long Undo is offered after a delete, the same on iOS. */
const val UNDO_MILLIS = 5_000L

/** “Name” deleted, or [fallback] for a blank name. */
fun deletedMessage(name: String, fallback: String): String =
    if (name.isBlank()) fallback else "“$name” deleted"

/** The Undo time, lengthened when the person's accessibility settings ask for longer timeouts. */
@Composable
fun undoTimeoutMillis(): Long =
    LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
        UNDO_MILLIS,
        containsIcons = false,
        containsText = true,
        containsControls = true
    ) ?: UNDO_MILLIS

/**
 * Shows [message] with an Undo action for [timeoutMillis], replacing any snackbar already showing.
 * Returns true if the person tapped Undo.
 */
suspend fun SnackbarHostState.showUndo(message: String, timeoutMillis: Long): Boolean = coroutineScope {
    currentSnackbarData?.dismiss()
    val timeout = launch {
        delay(timeoutMillis)
        currentSnackbarData?.dismiss()
    }
    val result = showSnackbar(message = message, actionLabel = "Undo", duration = SnackbarDuration.Indefinite)
    timeout.cancel()
    result == SnackbarResult.ActionPerformed
}
