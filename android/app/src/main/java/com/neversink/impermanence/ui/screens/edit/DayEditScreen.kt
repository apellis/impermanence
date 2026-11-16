package com.neversink.impermanence.ui.screens.edit

import android.app.TimePickerDialog
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.model.Theme
import com.neversink.impermanence.ui.components.ThemeSwatch
import com.neversink.impermanence.ui.components.BellSelectionControl
import com.neversink.impermanence.ui.components.SegmentEditorCard
import com.neversink.impermanence.ui.theme.AppUiTokens
import com.neversink.impermanence.util.TimeFormatting
import java.util.UUID
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.neversink.impermanence.ui.components.SegmentRow
import com.neversink.impermanence.ui.components.rememberReorderState

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


    val canSave = editingDay.name.isNotBlank() && editingDay.segments.isNotEmpty()
    // Leaving with unsaved changes asks first, by the back arrow or the system back gesture.
    val initialDay = remember(existingDay?.id) { editingDay }
    val hasChanges = editingDay != initialDay
    var confirmDiscard by remember { mutableStateOf(false) }
    fun requestDismiss() {
        if (hasChanges) confirmDiscard = true else onDismiss()
    }
    BackHandler(enabled = hasChanges) { confirmDiscard = true }

    val view = LocalView.current
    val listState = rememberLazyListState()
    // The segment open in the editor sheet, by id so it survives reordering.
    var editingSegmentId by remember { mutableStateOf<String?>(null) }

    fun updateSegments(change: (MutableList<Day.Segment>) -> Unit) {
        val segments = editingDay.segments.toMutableList()
        change(segments)
        editingDay = editingDay.copy(segments = segments)
    }

    fun moveSegment(from: Int, to: Int, announce: Boolean = false) {
        val segments = editingDay.segments
        if (from == to || from !in segments.indices || to !in segments.indices) return
        updateSegments { it.add(to, it.removeAt(from)) }
        if (announce) {
            val name = segments[from].name.ifBlank { "Segment" }
            @Suppress("DEPRECATION")
            view.announceForAccessibility("$name, position ${to + 1} of ${segments.size}")
        }
    }

    fun duplicateSegment(index: Int) {
        val original = editingDay.segments.getOrNull(index) ?: return
        updateSegments { it.add(index + 1, original.copy(id = UUID.randomUUID().toString())) }
    }

    val reorderState = rememberReorderState(
        listState = listState,
        canMoveTo = { key -> editingDay.segments.any { it.id == key } },
        onMove = { fromKey, toKey ->
            val segments = editingDay.segments
            moveSegment(segments.indexOfFirst { it.id == fromKey }, segments.indexOfFirst { it.id == toKey })
        }
    )

    val sheetIndex = editingDay.segments.indexOfFirst { it.id == editingSegmentId }
    if (sheetIndex >= 0) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetSegment = editingDay.segments[sheetIndex]
        ModalBottomSheet(
            onDismissRequest = { editingSegmentId = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                SegmentEditorCard(
                    segment = sheetSegment,
                    total = editingDay.segments.size,
                    schedule = editingDay.segmentSchedule().getOrElse(sheetIndex) { 0 to 0 },
                    defaultBell = editingDay.defaultBell,
                    onSegmentChange = { updated -> updateSegments { it[sheetIndex] = updated } },
                    onDelete = {
                        updateSegments { it.removeAt(sheetIndex) }
                        editingSegmentId = null
                    },
                    onDuplicate = {
                        duplicateSegment(sheetIndex)
                        editingSegmentId = null
                    },
                    use24HourClock = use24HourClock,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep Editing") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Day" else "Edit Day") },
                navigationIcon = {
                    IconButton(onClick = { requestDismiss() }) {
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
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(AppUiTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "day-fields") {
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
                        onBellChange = { bell ->
                            // iOS: changing the default bell also sets the start and manual bells. Only an
                            // actual change does this; opening and saving a day leaves them alone.
                            editingDay = editingDay.copy(defaultBell = bell, startBell = bell, manualBell = bell)
                        }
                    )
                }
            }

            val schedules = editingDay.segmentSchedule()
            val (dayStartSeconds, dayEndSeconds) = editingDay.startEndTimeSeconds
            val totalSeconds = (dayEndSeconds - dayStartSeconds).coerceAtLeast(0)
            item(key = "summary") {
                TimelineSummaryCard(
                    startSeconds = dayStartSeconds,
                    endSeconds = dayEndSeconds,
                    totalSeconds = totalSeconds,
                    use24HourClock = use24HourClock
                )
            }

            item(key = "segments-header") {
                Text(
                    "Segments",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(editingDay.segments, key = { _, segment -> segment.id }) { index, segment ->
                val schedule = schedules.getOrElse(index) { 0 to 0 }
                val dragging = reorderState.isDragging(segment.id)
                SegmentRow(
                    segment = segment,
                    position = index,
                    total = editingDay.segments.size,
                    startSeconds = schedule.first,
                    endSeconds = schedule.second,
                    defaultBell = editingDay.defaultBell,
                    use24HourClock = use24HourClock,
                    reorderState = reorderState,
                    dragging = dragging,
                    onClick = { editingSegmentId = segment.id },
                    onMoveUp = { moveSegment(index, index - 1, announce = true) },
                    onMoveDown = { moveSegment(index, index + 1, announce = true) },
                    onDuplicate = { duplicateSegment(index) },
                    modifier = if (dragging) {
                        // The dragged row follows the finger above the others; the rest slide aside.
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer { translationY = reorderState.draggingOffset }
                    } else {
                        Modifier.animateItemPlacement(animationSpec = tween(durationMillis = 200))
                    }
                )
            }

            item(key = "add-segment") {
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
    totalSeconds: Int,
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
                text = "Total duration: ${TimeFormatting.formattedDurationSeconds(totalSeconds)}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerField(
    theme: Theme,
    onThemeSelected: (Theme) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Theme", fontWeight = FontWeight.SemiBold)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = theme.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Theme") },
                leadingIcon = { ThemeSwatch(theme) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Theme.values().forEach { candidate ->
                    DropdownMenuItem(
                        text = { Text(candidate.displayName) },
                        leadingIcon = { ThemeSwatch(candidate) },
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
