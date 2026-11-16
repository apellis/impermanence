package com.neversink.impermanence.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.ui.theme.AppUiTokens
import com.neversink.impermanence.util.TimeFormatting

/**
 * One segment in the day editor: name and length on the first line, its clock times and end bell
 * on the second, and a drag handle. Tap to edit; drag the handle, or touch and hold the row, to
 * move it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SegmentRow(
    segment: Day.Segment,
    position: Int,
    total: Int,
    startSeconds: Int,
    endSeconds: Int,
    defaultBell: Bell,
    use24HourClock: Boolean,
    reorderState: ReorderState,
    dragging: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = segment.name.ifBlank { "Untitled segment" }
    val bell = segment.customEndBell ?: defaultBell
    val times = "${TimeFormatting.formattedTimeFromSeconds(startSeconds, use24HourClock)} – " +
        TimeFormatting.formattedTimeFromSeconds(endSeconds, use24HourClock)
    val actions = buildList {
        if (position > 0) add(CustomAccessibilityAction("Move up") { onMoveUp(); true })
        if (position < total - 1) add(CustomAccessibilityAction("Move down") { onMoveDown(); true })
        add(CustomAccessibilityAction("Duplicate") { onDuplicate(); true })
    }
    Surface(
        shape = RoundedCornerShape(AppUiTokens.CardCorner),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (dragging) 3.dp else 0.dp,
        shadowElevation = if (dragging) 8.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .reorderOnLongPress(reorderState, segment.id)
            .semantics { customActions = actions }
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClickLabel = "Edit segment",
                    onLongClickLabel = "Move segment",
                    onLongClick = {},
                    onClick = onClick
                )
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (segment.name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = TimeFormatting.formattedDurationSeconds(segment.durationSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = times,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = bell.numRings.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = if (bell.numRings == 1) "1 chime" else "${bell.numRings} chimes"
                        }
                    )
                }
            }
            // 48 dp touch target for the handle.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("segment-drag-handle")
                    .reorderHandle(reorderState, segment.id)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
