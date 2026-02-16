package com.impermanence.impermanence.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.impermanence.impermanence.model.Bell
import com.impermanence.impermanence.model.Day
import com.impermanence.impermanence.model.Theme
import com.impermanence.impermanence.util.TimeFormatting

@Composable
fun SegmentCard(
    segment: Day.Segment,
    startTimeSeconds: Int,
    endTimeSeconds: Int,
    bell: Bell,
    theme: Theme,
    use24HourClock: Boolean,
    useTheme: Boolean = false,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor: Color = when {
        useTheme || highlighted -> theme.mainColor
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor: Color = when {
        useTheme || highlighted -> theme.accentColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        tonalElevation = if (highlighted) 4.dp else 1.dp,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = segment.name.ifBlank { "Untitled segment" },
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeRange(startTimeSeconds, endTimeSeconds, use24HourClock),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
                Row {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${bell.numRings}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

private fun timeRange(startSeconds: Int, endSeconds: Int, use24: Boolean): String {
    val start = TimeFormatting.formattedTimeFromSeconds(startSeconds, use24)
    val end = TimeFormatting.formattedTimeFromSeconds(endSeconds, use24)
    return "$start – $end"
}
