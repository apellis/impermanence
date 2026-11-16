package com.neversink.impermanence.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.model.Theme
import com.neversink.impermanence.ui.theme.AppUiTokens
import com.neversink.impermanence.util.TimeFormatting

@Composable
fun DayCard(
    day: Day,
    index: Int,
    lastIndex: Int,
    use24HourClock: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = "${day.name}. ${provideTimeRange(day, use24HourClock)}. ${day.segments.size} segments."
        },
        onClick = onClick,
        shape = RoundedCornerShape(AppUiTokens.CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = themeContainerColor(day.theme),
            contentColor = day.theme.accentColor
        )
    ) {
        Column(modifier = Modifier.padding(AppUiTokens.CardPadding)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Reorder and delete live in an overflow menu so long names keep their room.
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options for ${day.name}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Move up") },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                            enabled = index > 0,
                            onClick = { menuOpen = false; onMoveUp() }
                        )
                        DropdownMenuItem(
                            text = { Text("Move down") },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                            enabled = index < lastIndex,
                            onClick = { menuOpen = false; onMoveDown() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = provideTimeRange(day, use24HourClock),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun provideTimeRange(day: Day, use24HourClock: Boolean): String {
    val schedules = day.segmentSchedule()
    if (schedules.isEmpty()) return "No segments"
    val start = schedules.first().first
    val end = schedules.last().second
    return "${TimeFormatting.formattedTimeFromSeconds(start, use24HourClock)} – ${TimeFormatting.formattedTimeFromSeconds(end, use24HourClock)}"
}

private fun themeContainerColor(theme: Theme): Color = theme.mainColor.copy(alpha = 0.9f)
