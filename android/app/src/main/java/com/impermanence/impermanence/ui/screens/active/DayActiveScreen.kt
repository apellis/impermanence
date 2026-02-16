package com.impermanence.impermanence.ui.screens.active

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.impermanence.impermanence.R
import com.impermanence.impermanence.domain.audio.BellPlayer
import com.impermanence.impermanence.model.Bell
import com.impermanence.impermanence.model.BellCatalog
import com.impermanence.impermanence.model.Day
import com.impermanence.impermanence.domain.timer.DayTimerEngine
import com.impermanence.impermanence.ui.components.SegmentCard
import com.impermanence.impermanence.ui.viewmodel.DayActiveViewModel
import com.impermanence.impermanence.util.KeepScreenOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayActiveScreen(
    day: Day,
    loopDays: Boolean,
    use24HourClock: Boolean,
    onExit: () -> Unit,
    onManualBellChange: (Day) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: DayActiveViewModel = viewModel(
        factory = DayActiveViewModel.provideFactory(application, day, loopDays)
    )
    val timerState by viewModel.timerState.collectAsState()
    var manualBell by remember(day.id) { mutableStateOf(day.manualBell) }
    var isManualBellMenuExpanded by remember { mutableStateOf(false) }

    fun updateManualBell(newBell: Bell) {
        manualBell = newBell
        onManualBellChange(day.copy(manualBell = newBell))
    }

    LaunchedEffect(loopDays) {
        viewModel.updateLoopDays(loopDays)
    }

    KeepScreenOn(active = timerState.status == DayTimerEngine.TimerStatus.ACTIVE)

    Scaffold(
        containerColor = day.theme.mainColor,
        topBar = {
            TopAppBar(
                title = { Text(day.name) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isManualBellMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.manual_bell_controls)
                        )
                    }
                    DropdownMenu(
                        expanded = isManualBellMenuExpanded,
                        onDismissRequest = { isManualBellMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ring_now)) },
                            onClick = {
                                BellPlayer.play(application, manualBell)
                                isManualBellMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.manual_bell_sound)) },
                            onClick = {},
                            enabled = false
                        )
                        BellCatalog.all.forEach { sound ->
                            val isSelected = sound.id == manualBell.soundId
                            DropdownMenuItem(
                                text = { Text(sound.name) },
                                onClick = {
                                    updateManualBell(manualBell.copy(soundId = sound.id))
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.manual_bell_chimes, manualBell.numRings)) },
                            onClick = {},
                            enabled = false
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.decrease_chimes)) },
                            onClick = {
                                updateManualBell(
                                    manualBell.copy(numRings = (manualBell.numRings - 1).coerceIn(1, 12))
                                )
                            },
                            enabled = manualBell.numRings > 1,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.increase_chimes)) },
                            onClick = {
                                updateManualBell(
                                    manualBell.copy(numRings = (manualBell.numRings + 1).coerceIn(1, 12))
                                )
                            },
                            enabled = manualBell.numRings < 12,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = day.theme.mainColor,
                    titleContentColor = day.theme.accentColor,
                    navigationIconContentColor = day.theme.accentColor,
                    actionIconContentColor = day.theme.accentColor
                )
            )
        }
    ) { padding ->
        val displayDay = day.copy(manualBell = manualBell)
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    timerState.activeSegmentName,
                    style = MaterialTheme.typography.titleLarge,
                    color = day.theme.accentColor
                )
            }

            if (timerState.activeSegmentTimeElapsed >= 0 && timerState.activeSegmentTimeRemaining >= 0) {
                item {
                    SegmentProgress(
                        elapsedSeconds = timerState.activeSegmentTimeElapsed,
                        remainingSeconds = timerState.activeSegmentTimeRemaining,
                        accentColor = day.theme.accentColor
                    )
                }
            }

            itemsIndexed(displayDay.segments, key = { _, segment -> segment.id }) { index, segment ->
                val schedule = displayDay.segmentSchedule().getOrNull(index) ?: (0 to 0)
                SegmentCard(
                    segment = segment,
                    startTimeSeconds = schedule.first,
                    endTimeSeconds = schedule.second,
                    bell = segment.resolvedBell(displayDay.defaultBell),
                    theme = displayDay.theme,
                    use24HourClock = use24HourClock,
                    useTheme = true,
                    highlighted = index == timerState.segmentIndex,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SegmentProgress(
    elapsedSeconds: Long,
    remainingSeconds: Long,
    accentColor: Color
) {
    val total = elapsedSeconds + remainingSeconds
    val progress = if (total <= 0) 0f else elapsedSeconds.toFloat() / total
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = accentColor,
            trackColor = accentColor.copy(alpha = 0.24f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Elapsed: ${formatDuration(elapsedSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
            Text(
                text = "Remaining: ${formatDuration(remainingSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val totalSeconds = seconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%02d:%02d", minutes, secs)
    }
}
