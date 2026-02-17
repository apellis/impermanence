package com.impermanence.impermanence.ui.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.impermanence.impermanence.model.Day
import com.impermanence.impermanence.model.Theme
import com.impermanence.impermanence.ui.theme.AppUiTokens
import com.impermanence.impermanence.util.TimeFormatting

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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = day.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onMoveUp, enabled = index > 0) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown, enabled = index < lastIndex) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Move down")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
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
