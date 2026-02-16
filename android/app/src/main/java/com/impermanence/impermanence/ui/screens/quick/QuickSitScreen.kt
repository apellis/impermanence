package com.impermanence.impermanence.ui.screens.quick

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.impermanence.impermanence.domain.audio.BellPlayer
import com.impermanence.impermanence.model.Bell
import com.impermanence.impermanence.model.BellCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSitScreen(
    use24HourClock: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var minutes by remember { mutableStateOf(15) }
    var secondsRemaining by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedBellId by remember { mutableStateOf(BellCatalog.defaultSound.id) }
    var startChimes by remember { mutableStateOf(1) }
    var endChimes by remember { mutableStateOf(1) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    val totalSeconds = minutes * 60

    BackHandler(enabled = isRunning) {
        // Keep behavior aligned with iOS: disable close while a session is running.
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        isRunning = false
        secondsRemaining = 0
    }

    fun playBell(chimes: Int) {
        BellPlayer.play(context, Bell(soundId = selectedBellId, numRings = chimes))
    }

    fun startTimer() {
        stopTimer()
        secondsRemaining = totalSeconds
        isRunning = true
        playBell(startChimes)
        timerJob = coroutineScope.launch {
            while (secondsRemaining > 0) {
                delay(1_000L)
                secondsRemaining -= 1
            }
            stopTimer()
            playBell(endChimes)
            onClose()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Quick Sit") },
                navigationIcon = {
                    IconButton(onClick = {
                        onClose()
                    }, enabled = !isRunning) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DurationSection(
                minutes = minutes,
                isRunning = isRunning,
                onChange = { minutes = it.coerceIn(1, 180) },
                onStart = {
                    if (isRunning) stopTimer() else startTimer()
                }
            )

            BellSection(
                selectedBellId = selectedBellId,
                startChimes = startChimes,
                endChimes = endChimes,
                isRunning = isRunning,
                onSelectedBellChange = { selectedBellId = it },
                onStartChimesChange = { startChimes = it.coerceIn(1, 12) },
                onEndChimesChange = { endChimes = it.coerceIn(1, 12) }
            )

            if (isRunning) {
                ProgressSection(totalSeconds = totalSeconds, secondsRemaining = secondsRemaining, use24HourClock = use24HourClock)
            }
        }
    }
}

@Composable
private fun DurationSection(
    minutes: Int,
    isRunning: Boolean,
    onChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Duration", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(-5, -1, +1, +5).forEach { delta ->
                OutlinedButton(onClick = { if (!isRunning) onChange(minutes + delta) }, enabled = !isRunning) {
                    Text(if (delta > 0) "+$delta" else "$delta")
                }
            }
            Text("${minutes} min", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleLarge)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val presets = listOf(5, 10, 15, 20, 30, 45, 60, 75, 90)
            items(presets.size) { index ->
                val preset = presets[index]
                AssistChip(
                    onClick = { if (!isRunning) onChange(preset) },
                    enabled = !isRunning,
                    label = { Text("${preset} min") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (minutes == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (minutes == preset) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        OutlinedButton(onClick = onStart) {
            Icon(imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRunning) "Cancel" else "Start")
        }
    }
}

@Composable
private fun BellSection(
    selectedBellId: Int,
    startChimes: Int,
    endChimes: Int,
    isRunning: Boolean,
    onSelectedBellChange: (Int) -> Unit,
    onStartChimesChange: (Int) -> Unit,
    onEndChimesChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Bell", style = MaterialTheme.typography.titleMedium)
        val bellOptions = BellCatalog.all
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bellOptions.size) { index ->
                val sound = bellOptions[index]
                AssistChip(
                    onClick = { if (!isRunning) onSelectedBellChange(sound.id) },
                    enabled = !isRunning,
                    label = { Text(sound.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedBellId == sound.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (selectedBellId == sound.id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Start chimes: $startChimes")
            OutlinedButton(onClick = { if (!isRunning) onStartChimesChange(startChimes - 1) }, enabled = !isRunning && startChimes > 1) {
                Text("-1")
            }
            OutlinedButton(onClick = { if (!isRunning) onStartChimesChange(startChimes + 1) }, enabled = !isRunning && startChimes < 12) {
                Text("+1")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("End chimes: $endChimes")
            OutlinedButton(onClick = { if (!isRunning) onEndChimesChange(endChimes - 1) }, enabled = !isRunning && endChimes > 1) {
                Text("-1")
            }
            OutlinedButton(onClick = { if (!isRunning) onEndChimesChange(endChimes + 1) }, enabled = !isRunning && endChimes < 12) {
                Text("+1")
            }
        }
    }
}

@Composable
private fun ProgressSection(totalSeconds: Int, secondsRemaining: Int, use24HourClock: Boolean) {
    val progress = if (totalSeconds == 0) 0f else 1f - secondsRemaining.toFloat() / totalSeconds
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(progress = progress)
        Spacer(modifier = Modifier.height(12.dp))
        val minutesRemaining = secondsRemaining / 60
        val secondsPart = secondsRemaining % 60
        Text(String.format("%02d:%02d", minutesRemaining, secondsPart), style = MaterialTheme.typography.headlineSmall)
        val finishTime = java.time.LocalTime.now().plusSeconds(secondsRemaining.toLong())
        val secondsOfDay = finishTime.toSecondOfDay()
        val formatted = com.impermanence.impermanence.util.TimeFormatting.formattedTimeFromSeconds(secondsOfDay, use24HourClock)
        Text("Ends around $formatted", style = MaterialTheme.typography.bodyMedium)
    }
}
