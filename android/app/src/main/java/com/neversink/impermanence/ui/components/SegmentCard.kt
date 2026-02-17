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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.model.Theme
import com.neversink.impermanence.ui.theme.AppUiTokens
import com.neversink.impermanence.util.TimeFormatting

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
        modifier = modifier.semantics {
            val range = timeRange(startTimeSeconds, endTimeSeconds, use24HourClock)
            val activeStatus = if (highlighted) "Now." else ""
            contentDescription = "$activeStatus ${segment.name.ifBlank { "Untitled segment" }}. $range. ${bell.numRings} chimes."
        },
        tonalElevation = if (highlighted) 4.dp else 1.dp,
        shape = RoundedCornerShape(AppUiTokens.CardCorner),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppUiTokens.CardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = segment.name.ifBlank { "Untitled segment" },
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                if (highlighted) {
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppUiTokens.ItemSpacing))
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
