package com.impermanence.impermanence.ui.screens.edit

import android.app.TimePickerDialog
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.impermanence.impermanence.model.Bell
import com.impermanence.impermanence.model.Day
import com.impermanence.impermanence.model.Theme
import com.impermanence.impermanence.ui.components.BellSelectionControl
import com.impermanence.impermanence.ui.components.SegmentEditorCard
import com.impermanence.impermanence.ui.theme.AppUiTokens
import com.impermanence.impermanence.util.TimeFormatting
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DayEditScreen(
    existingDay: Day?,
    use24HourClock: Boolean,
    onDismiss: () -> Unit,
    onSave: (Day) -> Unit
) {
    val isNew = existingDay == null
    var editingDay by remember(existingDay?.id) {
        mutableStateOf(
            existingDay ?: Day(
                name = "",
                startTimeSeconds = 0,
                segments = emptyList(),
                startBell = Bell.SingleBell,
                manualBell = Bell.SingleBell,
                defaultBell = Bell.SingleBell,
                theme = Theme.TEAL
            )
        )
    }

    LaunchedEffect(editingDay.defaultBell.soundId, editingDay.defaultBell.numRings) {
        editingDay = editingDay.copy(
            startBell = editingDay.defaultBell,
            manualBell = editingDay.defaultBell
        )
    }

    val canSave = editingDay.name.isNotBlank() && editingDay.segments.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Day" else "Edit Day") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = { if (canSave) onSave(editingDay) }, enabled = canSave) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
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
            contentPadding = PaddingValues(AppUiTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.SectionSpacing)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editingDay.name,
                        onValueChange = { editingDay = editingDay.copy(name = it) },
                        label = { Text("Day name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TimePickerField(
                        label = "Start of day",
                        seconds = editingDay.startTimeSeconds,
                        use24HourClock = use24HourClock,
                        onTimeChanged = { seconds -> editingDay = editingDay.copy(startTimeSeconds = seconds) }
                    )

                    ThemePickerField(
                        theme = editingDay.theme,
                        onThemeSelected = { editingDay = editingDay.copy(theme = it) }
                    )

                    BellSelectionControl(
                        title = "Default bell",
                        bell = editingDay.defaultBell,
                        onBellChange = { bell -> editingDay = editingDay.copy(defaultBell = bell) }
                    )
                }
            }

            val schedules = editingDay.segmentSchedule()
            val (dayStartSeconds, dayEndSeconds) = editingDay.startEndTimeSeconds
            val totalMinutes = ((dayEndSeconds - dayStartSeconds).coerceAtLeast(0)) / 60
            item {
                TimelineSummaryCard(
                    startSeconds = dayStartSeconds,
                    endSeconds = dayEndSeconds,
                    totalMinutes = totalMinutes,
                    use24HourClock = use24HourClock
                )
            }

            itemsIndexed(editingDay.segments, key = { _, segment -> segment.id }) { index, segment ->
                val schedule = schedules.getOrElse(index) { 0 to 0 }
                TimelineSegmentEditorItem(
                    startSeconds = schedule.first,
                    endSeconds = schedule.second,
                    use24HourClock = use24HourClock,
                    modifier = Modifier.animateItemPlacement(animationSpec = tween(durationMillis = 180))
                ) {
                    SegmentEditorCard(
                        segment = segment,
                        position = index,
                        total = editingDay.segments.size,
                        schedule = schedule,
                        defaultBell = editingDay.defaultBell,
                        onSegmentChange = { updated ->
                            val updatedSegments = editingDay.segments.toMutableList()
                            updatedSegments[index] = updated
                            editingDay = editingDay.copy(segments = updatedSegments)
                        },
                        onDelete = {
                            val updatedSegments = editingDay.segments.toMutableList()
                            updatedSegments.removeAt(index)
                            editingDay = editingDay.copy(segments = updatedSegments)
                        },
                        onDuplicate = {
                            val duplicate = segment.copy(id = UUID.randomUUID().toString())
                            val updatedSegments = editingDay.segments.toMutableList()
                            updatedSegments.add(index + 1, duplicate)
                            editingDay = editingDay.copy(segments = updatedSegments)
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val updatedSegments = editingDay.segments.toMutableList()
                                updatedSegments.removeAt(index).also { item ->
                                    updatedSegments.add(index - 1, item)
                                }
                                editingDay = editingDay.copy(segments = updatedSegments)
                            }
                        },
                        onMoveDown = {
                            if (index < editingDay.segments.lastIndex) {
                                val updatedSegments = editingDay.segments.toMutableList()
                                val item = updatedSegments.removeAt(index)
                                updatedSegments.add(index + 1, item)
                                editingDay = editingDay.copy(segments = updatedSegments)
                            }
                        },
                        use24HourClock = use24HourClock,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                FilledTonalButton(onClick = {
                    val newSegment = Day.Segment(
                        name = "",
                        durationSeconds = 15 * 60,
                        customEndBell = null
                    )
                    editingDay = editingDay.copy(segments = editingDay.segments + newSegment)
                }) {
                    Text("Add Segment")
                }
            }
        }
    }
}

@Composable
private fun TimelineSummaryCard(
    startSeconds: Int,
    endSeconds: Int,
    totalMinutes: Int,
    use24HourClock: Boolean
) {
    Surface(
        shape = RoundedCornerShape(AppUiTokens.CardCorner),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppUiTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Schedule Timeline", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(
                text = "${TimeFormatting.formattedTimeFromSeconds(startSeconds, use24HourClock)} – ${TimeFormatting.formattedTimeFromSeconds(endSeconds, use24HourClock)}",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Total duration: ${TimeFormatting.formattedDurationMinutes(totalMinutes)}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimelineSegmentEditorItem(
    startSeconds: Int,
    endSeconds: Int,
    use24HourClock: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(84.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = TimeFormatting.formattedTimeFromSeconds(startSeconds, use24HourClock),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(
                modifier = Modifier
                    .width(AppUiTokens.RailWidth)
                    .height(92.dp)
                    .background(
                        androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(2.dp)
                    )
            )
            Text(
                text = TimeFormatting.formattedTimeFromSeconds(endSeconds, use24HourClock),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun ThemePickerField(
    theme: Theme,
    onThemeSelected: (Theme) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Theme", fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = theme.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Theme") },
                trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Theme.values().forEach { candidate ->
                    DropdownMenuItem(
                        text = {
                            ThemeOptionLabel(theme = candidate)
                        },
                        onClick = {
                            onThemeSelected(candidate)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionLabel(theme: Theme) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(theme.mainColor, RoundedCornerShape(4.dp))
                .border(
                    width = 1.dp,
                    color = if (theme.accentColor == Color.Black) Color.Black.copy(alpha = 0.2f) else theme.accentColor,
                    shape = RoundedCornerShape(4.dp)
                )
        )
        Text(theme.displayName, color = theme.accentColor)
    }
}

@Composable
private fun TimePickerField(
    label: String,
    seconds: Int,
    use24HourClock: Boolean,
    onTimeChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val hour = seconds / 3600
    val minute = (seconds % 3600) / 60
    val displayValue = TimeFormatting.formattedTimeFromSeconds(seconds, use24HourClock)
    androidx.compose.material3.OutlinedButton(
        onClick = {
            TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                onTimeChanged(selectedHour * 3600 + selectedMinute * 60)
            }, hour, minute, use24HourClock).show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(displayValue)
        }
    }
}
