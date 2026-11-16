package com.neversink.impermanence.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Drag-to-reorder for rows of a [androidx.compose.foundation.lazy.LazyColumn].
 *
 * The dragged row follows the finger (drawn above the others), the rows it passes slide out of the
 * way, and the list scrolls on its own while the row is held near the top or bottom edge. Only rows
 * whose key passes [canMoveTo] are drop targets, so headers and footers in the same list stay put.
 *
 * Finger movement is measured in root coordinates. The row moves under the finger, so its own
 * local coordinates would cancel out the motion.
 */
class ReorderState internal constructor(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val haptics: HapticFeedback,
    private val edgePx: Float,
    private val maxScrollPerFramePx: Float,
    private val canMoveTo: (Any) -> Boolean,
    private val onMove: (from: Any, to: Any) -> Unit,
    private val onDragEnd: () -> Unit
) {
    /** Key of the row being dragged, or null. */
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var initialOffset by mutableIntStateOf(0)
    private var dragDelta by mutableFloatStateOf(0f)
    private var scrolled by mutableFloatStateOf(0f)
    private var autoScrollJob: Job? = null
    // After a swap, the list index the dragged row should reach once the list lays out again.
    // Swaps wait for that layout, so stale positions can't swap the row straight back.
    private var expectedIndex: Int? = null

    private val draggingInfo: LazyListItemInfo?
        get() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingKey }

    /** How far to draw the dragged row from where the list has laid it out. */
    val draggingOffset: Float
        get() = draggingInfo?.let { initialOffset + dragDelta + scrolled - it.offset } ?: 0f

    fun isDragging(key: Any) = draggingKey == key

    /** Starts dragging [key]; false if a drag is already under way or the row isn't visible. */
    internal fun start(key: Any): Boolean {
        if (draggingKey != null) return false
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return false
        draggingKey = key
        initialOffset = info.offset
        dragDelta = 0f
        scrolled = 0f
        expectedIndex = null
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        return true
    }

    internal fun drag(deltaY: Float) {
        if (draggingKey == null) return
        dragDelta += deltaY
        swapIfNeeded()
        updateAutoScroll()
    }

    internal fun stop() {
        if (draggingKey == null) return
        autoScrollJob?.cancel()
        autoScrollJob = null
        draggingKey = null
        dragDelta = 0f
        scrolled = 0f
        onDragEnd()
    }

    /** Moves the dragged row past a neighbour once its middle crosses into that neighbour. */
    private fun swapIfNeeded() {
        val dragging = draggingInfo ?: return
        if (expectedIndex != null && dragging.index != expectedIndex) return
        expectedIndex = null
        val top = dragging.offset + draggingOffset
        val middle = top + dragging.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.key != dragging.key &&
                canMoveTo(item.key) &&
                middle >= item.offset && middle <= item.offset + item.size
        } ?: return
        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        onMove(dragging.key, target.key)
        expectedIndex = target.index
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // A LazyColumn keeps its first visible item in place by key; when that item is one of the
        // two that swapped, hold the scroll position instead so the list doesn't jump.
        if (dragging.index == firstIndex || target.index == firstIndex) {
            scope.launch { listState.scrollToItem(firstIndex, firstOffset) }
        }
    }

    private fun edgeSpeed(): Float {
        val dragging = draggingInfo ?: return 0f
        val info = listState.layoutInfo
        val top = dragging.offset + draggingOffset
        val bottom = top + dragging.size
        val start = info.viewportStartOffset.toFloat()
        val end = info.viewportEndOffset.toFloat()
        val fraction = when {
            bottom > end - edgePx && dragDelta > 0 -> ((bottom - (end - edgePx)) / edgePx)
            top < start + edgePx && dragDelta < 0 -> -((start + edgePx - top) / edgePx)
            else -> 0f
        }.coerceIn(-1f, 1f)
        return fraction * maxScrollPerFramePx
    }

    private fun updateAutoScroll() {
        if (edgeSpeed() == 0f || autoScrollJob?.isActive == true) return
        autoScrollJob = scope.launch {
            while (isActive && draggingKey != null) {
                val speed = edgeSpeed()
                if (speed == 0f) break
                val consumed = listState.scrollBy(speed)
                if (abs(consumed) < 0.5f) break
                scrolled += consumed
                swapIfNeeded()
                withFrameNanos { }
            }
        }
    }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    canMoveTo: (Any) -> Boolean,
    onMove: (from: Any, to: Any) -> Unit,
    onDragEnd: () -> Unit = {}
): ReorderState {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentCanMoveTo by rememberUpdatedState(canMoveTo)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val density = androidx.compose.ui.platform.LocalDensity.current
    return remember(listState, scope, haptics, density) {
        with(density) {
            ReorderState(
                listState = listState,
                scope = scope,
                haptics = haptics,
                edgePx = 72.dp.toPx(),
                maxScrollPerFramePx = 18.dp.toPx(),
                canMoveTo = { currentCanMoveTo(it) },
                onMove = { from, to -> currentOnMove(from, to) },
                onDragEnd = { currentOnDragEnd() }
            )
        }
    }
}

/**
 * Starts a drag as soon as the finger moves on this element (for a drag handle). Taps pass through.
 */
fun Modifier.reorderHandle(state: ReorderState, key: Any): Modifier = composed {
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val slop = LocalViewConfiguration.current.touchSlop
    onGloballyPositioned { coords[0] = it }.pointerInput(state, key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // Wait for a real drag, so a tap on the handle does nothing.
            var travelled = 0f
            var started = false
            while (!started) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (!change.pressed) return@awaitEachGesture
                travelled += change.positionChange().y
                change.consume()
                if (abs(travelled) > slop) started = true
            }
            followFinger(state, key, down, coords)
        }
    }
}

/** Starts a drag after a long press anywhere on this element, the way Android lists reorder. */
fun Modifier.reorderOnLongPress(state: ReorderState, key: Any): Modifier = composed {
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val config = LocalViewConfiguration.current
    onGloballyPositioned { coords[0] = it }.pointerInput(state, key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // A move past the slop before the timeout is a scroll or a tap; leave it to the list.
            // withTimeoutOrNull returns null only when the press outlasted the timeout.
            val heldStill = withTimeoutOrNull(config.longPressTimeoutMillis) {
                var moved = Offset.Zero
                var ended = false
                while (!ended) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    // Consumption isn't checked: the row's own click handling consumes the down.
                    if (change == null || !change.pressed) {
                        ended = true
                    } else {
                        moved += change.positionChange()
                        if (moved.getDistance() > config.touchSlop) ended = true
                    }
                }
                false
            }
            if (heldStill == null) followFinger(state, key, down, coords)
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.followFinger(
    state: ReorderState,
    key: Any,
    down: PointerInputChange,
    coords: Array<LayoutCoordinates?>
) {
    val startCoords = coords[0] ?: return
    if (!state.start(key)) return
    var lastRootY = startCoords.localToRoot(down.position).y
    try {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                change.consume()
                break
            }
            val c = coords[0]?.takeIf { it.isAttached } ?: break
            val rootY = c.localToRoot(change.position).y
            state.drag(rootY - lastRootY)
            lastRootY = rootY
            change.consume()
        }
    } finally {
        state.stop()
    }
}
