package com.impermanence.impermanence.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    use24HourClock: Boolean,
    loopDays: Boolean,
    onUse24HourClockChanged: (Boolean) -> Unit,
    onLoopDaysChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                .fillMaxSize()
        ) {
            SettingRow(
                title = "Use 24-hour clock",
                subtitle = "Match your system preference.",
                checked = use24HourClock,
                onToggle = onUse24HourClockChanged
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            SettingRow(
                title = "Loop days at midnight",
                subtitle = "Automatically restart each day's schedule.",
                checked = loopDays,
                onToggle = onLoopDaysChanged
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
