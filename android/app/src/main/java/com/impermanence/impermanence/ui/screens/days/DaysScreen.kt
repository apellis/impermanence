package com.impermanence.impermanence.ui.screens.days

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.impermanence.impermanence.model.Day
import com.impermanence.impermanence.ui.components.DayCard
import com.impermanence.impermanence.ui.viewmodel.AppUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysScreen(
    uiState: AppUiState,
    paddingValues: PaddingValues,
    onDaySelected: (String) -> Unit,
    onQuickSit: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onAddDay: () -> Unit,
    onDeleteDay: (String) -> Unit,
    onMoveDay: (fromIndex: Int, toIndex: Int) -> Unit
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Days") },
                actions = {
                    IconButton(onClick = onAbout) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "About")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onQuickSit) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = "Quick Sit")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.semantics { contentDescription = "New Day" },
                onClick = onAddDay,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("New Day") }
            )
        }
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + paddingValues.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding() + paddingValues.calculateBottomPadding() + 80.dp,
            start = 16.dp,
            end = 16.dp
        )
        DaysList(
            days = uiState.days,
            use24HourClock = uiState.use24HourClock,
            contentPadding = contentPadding,
            onDaySelected = onDaySelected,
            onDeleteDay = onDeleteDay,
            onMoveDay = onMoveDay
        )
    }
}

@Composable
private fun DaysList(
    days: List<Day>,
    use24HourClock: Boolean,
    contentPadding: PaddingValues,
    onDaySelected: (String) -> Unit,
    onDeleteDay: (String) -> Unit,
    onMoveDay: (fromIndex: Int, toIndex: Int) -> Unit
) {
    if (days.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No days yet. Tap New Day to begin.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(days, key = { _, day -> day.id }) { index, day ->
            DayCard(
                day = day,
                index = index,
                lastIndex = days.lastIndex,
                use24HourClock = use24HourClock,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onDaySelected(day.id) },
                onMoveUp = { if (index > 0) onMoveDay(index, index - 1) },
                onMoveDown = { if (index < days.lastIndex) onMoveDay(index, index + 1) },
                onDelete = { onDeleteDay(day.id) }
            )
        }
    }
}
