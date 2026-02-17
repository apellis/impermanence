package com.neversink.impermanence.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.ui.components.SegmentCard
import com.neversink.impermanence.util.TimeFormatting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    day: Day,
    use24HourClock: Boolean,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(day.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onStart) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Start or Resume Day")
                    }
                }
            }

            item {
                DayInfo(day = day, use24HourClock = use24HourClock)
            }

            itemsIndexed(day.segments) { index, segment ->
                val schedule = day.segmentSchedule()[index]
                SegmentCard(
                    segment = segment,
                    startTimeSeconds = schedule.first,
                    endTimeSeconds = schedule.second,
                    bell = segment.resolvedBell(day.defaultBell),
                    theme = day.theme,
                    use24HourClock = use24HourClock,
                    highlighted = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DayInfo(day: Day, use24HourClock: Boolean) {
    val (startSeconds, endSeconds) = day.startEndTimeSeconds
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoRow(label = "Schedule", value = "${TimeFormatting.formattedTimeFromSeconds(startSeconds, use24HourClock)} – ${TimeFormatting.formattedTimeFromSeconds(endSeconds, use24HourClock)}")
        InfoRow(label = "Theme", value = day.theme.displayName)
        InfoRow(label = "Segments", value = day.segments.size.toString())
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value)
    }
}
