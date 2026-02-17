package com.impermanence.impermanence.ui.components

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.impermanence.impermanence.model.Bell
import com.impermanence.impermanence.model.Day
import com.impermanence.impermanence.ui.theme.AppUiTokens
import com.impermanence.impermanence.util.TimeFormatting

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
                IconButton(onClick = onDelete) {
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
                durationMinutes = localSegment.durationSeconds / 60,
                onDurationChanged = { minutes ->
                    val clamped = minutes.coerceIn(1, 24 * 60)
                    localSegment = localSegment.copy(durationSeconds = clamped * 60)
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

@Composable
private fun DurationControl(
    durationMinutes: Int,
    onDurationChanged: (Int) -> Unit
) {
    val durationText = durationMinutes.toString()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Duration")
        OutlinedTextField(
            value = durationText,
            onValueChange = { newValue ->
                val digits = newValue.filter { it.isDigit() }
                if (digits.isEmpty()) return@OutlinedTextField
                onDurationChanged(digits.toIntOrNull() ?: durationMinutes)
            },
            modifier = Modifier.width(140.dp),
            label = { Text("Minutes") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Text(
            text = TimeFormatting.formattedDurationMinutes(durationMinutes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDurationChanged(durationMinutes - 1) }) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus one minute")
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { onDurationChanged(durationMinutes + 1) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Plus one minute")
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(onClick = { onDurationChanged(durationMinutes - 5) }) {
                Text("-5 min")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { onDurationChanged(durationMinutes + 5) }) {
                Text("+5 min")
            }
        }
    }
}
