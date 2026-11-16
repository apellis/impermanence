package com.neversink.impermanence.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope

/**
 * Runs [block] while the screen is visible: it starts when the screen starts, is cancelled when it
 * stops (screen off, app in the background), and starts again when it comes back. Use it for UI
 * refresh loops, so they don't wake the CPU all day during a long session. Bells don't depend on it.
 */
@Composable
fun LaunchedWhileStarted(vararg keys: Any?, block: suspend CoroutineScope.() -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, *keys) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}
