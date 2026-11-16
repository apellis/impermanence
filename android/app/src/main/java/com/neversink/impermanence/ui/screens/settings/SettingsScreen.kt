package com.neversink.impermanence.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    use24HourClock: Boolean,
    loopDays: Boolean,
    keepScreenAwakeDuringDay: Boolean,
    onUse24HourClockChanged: (Boolean) -> Unit,
    onLoopDaysChanged: (Boolean) -> Unit,
    onKeepScreenAwakeDuringDayChanged: (Boolean) -> Unit,
    onImportDays: () -> Unit,
    onExportDays: () -> Unit,
    onBack: () -> Unit
) {
    var confirmImport by remember { mutableStateOf(false) }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Replace all days?") },
            text = { Text("Importing replaces every day in the app with the days in the file. Export first to keep a copy.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    onImportDays()
                }) { Text("Choose File") }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
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
                title = stringResource(R.string.setting_use_24h),
                subtitle = stringResource(R.string.setting_use_24h_subtitle),
                checked = use24HourClock,
                onToggle = onUse24HourClockChanged
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            SettingRow(
                title = stringResource(R.string.setting_loop_days),
                subtitle = stringResource(R.string.setting_loop_days_subtitle),
                checked = loopDays,
                onToggle = onLoopDaysChanged
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            SettingRow(
                title = stringResource(R.string.setting_keep_screen_awake),
                subtitle = stringResource(R.string.setting_keep_screen_awake_subtitle),
                checked = keepScreenAwakeDuringDay,
                onToggle = onKeepScreenAwakeDuringDayChanged
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.setting_data), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.setting_data_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.padding(vertical = 6.dp))
            Button(
                onClick = { confirmImport = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.import_days))
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            OutlinedButton(
                onClick = onExportDays,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.export_days))
            }
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
    // The whole row is the switch, so TalkBack reads the setting's name and state together.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 16.dp))
    }
}
