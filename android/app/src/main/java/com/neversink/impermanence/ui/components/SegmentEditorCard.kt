package com.neversink.impermanence.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.ui.theme.AppUiTokens
import com.neversink.impermanence.util.TimeFormatting

@Composable
fun SegmentEditorCard(
    segment: Day.Segment,
    position: Int,
    total: Int,
    schedule: Pair<Int, Int>,
    defaultBell: Bell,
    onSegmentChange: (Day.Segment) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    use24HourClock: Boolean,
    modifier: Modifier = Modifier
) {
    var localSegment by remember(segment) { mutableStateOf(segment) }
    var useDefaultBell by remember(segment, defaultBell) { mutableStateOf(segment.customEndBell == null) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppUiTokens.CardCorner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppUiTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onMoveUp, enabled = position > 0) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, enabled = position < total - 1) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move down")
                }
                IconButton(onClick = onDuplicate) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate")
                }
                // A day needs at least one segment.
                IconButton(onClick = onDelete, enabled = total > 1) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            OutlinedTextField(
                value = localSegment.name,
                onValueChange = {
                    localSegment = localSegment.copy(name = it)
                    onSegmentChange(localSegment)
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Time: ${TimeFormatting.formattedTimeFromSeconds(schedule.first, use24HourClock = use24HourClock)} – ${TimeFormatting.formattedTimeFromSeconds(schedule.second, use24HourClock = use24HourClock)}",
                style = MaterialTheme.typography.bodyMedium
            )

            DurationControl(
                durationSeconds = localSegment.durationSeconds,
                onDurationChanged = { seconds ->
                    localSegment = localSegment.copy(durationSeconds = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS))
                    onSegmentChange(localSegment)
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use day default bell")
                Switch(
                    checked = useDefaultBell,
                    onCheckedChange = { checked ->
                        useDefaultBell = checked
                        localSegment = if (checked) {
                            localSegment.copy(customEndBell = null)
                        } else {
                            localSegment.copy(customEndBell = defaultBell)
                        }
                        onSegmentChange(localSegment)
                    }
                )
            }

            if (!useDefaultBell) {
                BellSelectionControl(
                    title = "Bell",
                    bell = localSegment.customEndBell ?: defaultBell,
                    onBellChange = { bell ->
                        localSegment = localSegment.copy(customEndBell = bell)
                        onSegmentChange(localSegment)
                    }
                )
            }
        }
    }
}

private const val MIN_SECONDS = 1
private const val MAX_SECONDS = 24 * 60 * 60

/**
 * Minutes and seconds fields for a segment's length. A field may be empty while typing; the
 * duration only changes once the fields hold a valid length, from 1 second to 24 hours.
 */
@Composable
private fun DurationControl(
    durationSeconds: Int,
    onDurationChanged: (Int) -> Unit
) {
    var minutesText by remember { mutableStateOf((durationSeconds / 60).toString()) }
    var secondsText by remember { mutableStateOf((durationSeconds % 60).toString()) }
    // Follow outside changes (the +/- buttons, undo of a move) without fighting the person's typing.
    LaunchedEffect(durationSeconds) {
        val typed = (minutesText.toIntOrNull() ?: 0) * 60 + (secondsText.toIntOrNull() ?: 0)
        if (typed != durationSeconds) {
            minutesText = (durationSeconds / 60).toString()
            secondsText = (durationSeconds % 60).toString()
        }
    }
    fun commit(minutes: String, seconds: String) {
        val m = minutes.toIntOrNull() ?: 0
        val s = seconds.toIntOrNull() ?: 0
        val total = m * 60 + s
        if ((minutes.isNotEmpty() || seconds.isNotEmpty()) && total in MIN_SECONDS..MAX_SECONDS) onDurationChanged(total)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Duration")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { newValue ->
                    minutesText = newValue.filter { it.isDigit() }.take(4)
                    commit(minutesText, secondsText)
                },
                modifier = Modifier.width(110.dp),
                label = { Text("Minutes") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = secondsText,
                onValueChange = { newValue ->
                    val digits = newValue.filter { it.isDigit() }.take(2)
                    secondsText = if ((digits.toIntOrNull() ?: 0) > 59) "59" else digits
                    commit(minutesText, secondsText)
                },
                modifier = Modifier.width(110.dp),
                label = { Text("Seconds") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        Text(
            text = TimeFormatting.formattedDurationSeconds(durationSeconds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDurationChanged((durationSeconds - 60).coerceAtLeast(MIN_SECONDS)) }) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus one minute")
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { onDurationChanged((durationSeconds + 60).coerceAtMost(MAX_SECONDS)) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Plus one minute")
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = { onDurationChanged((durationSeconds - 300).coerceAtLeast(MIN_SECONDS)) }) {
                Text("-5 min")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { onDurationChanged((durationSeconds + 300).coerceAtMost(MAX_SECONDS)) }) {
                Text("+5 min")
            }
        }
    }
}
