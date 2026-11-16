package com.neversink.impermanence.ui

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.neversink.impermanence.domain.session.TimingSession
import com.neversink.impermanence.service.SessionEngine

import android.widget.Toast
import com.neversink.impermanence.data.datastore.UnreadableDaysBackup
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neversink.impermanence.AppContainer
import com.neversink.impermanence.R
import com.neversink.impermanence.data.portability.PortableDayPlanCodec
import com.neversink.impermanence.model.Day
import com.neversink.impermanence.ui.screens.about.AboutScreen
import com.neversink.impermanence.ui.screens.active.DayActiveScreen
import com.neversink.impermanence.ui.screens.days.DaysScreen
import com.neversink.impermanence.ui.screens.detail.DayDetailScreen
import com.neversink.impermanence.ui.screens.edit.DayEditScreen
import com.neversink.impermanence.ui.screens.quick.QuickSitScreen
import com.neversink.impermanence.ui.screens.settings.SettingsScreen
import com.neversink.impermanence.ui.theme.ImpermanenceTheme
import com.neversink.impermanence.ui.viewmodel.AppViewModel
import java.time.LocalDate

object Destinations {
    const val Days = "days"
    const val DayDetail = "day_detail"
    const val DayActive = "day_active"
    const val DayEdit = "day_edit"
    const val QuickSit = "quick_sit"
    const val Settings = "settings"
    const val About = "about"
}

private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (out.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}

@Composable
private fun PopWhenMissing(isLoading: Boolean, pop: () -> Unit) {
    // Never navigate during composition, and never before saved days have loaded (after process death
    // the restored screen must not be popped just because the list is still empty).
    LaunchedEffect(isLoading) {
        if (!isLoading) pop()
    }
}

@Composable
fun ImpermanenceApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.provideFactory(appContainer.dayRepository, appContainer.settingsRepository))
    val uiState by appViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to open selected file." }
                val bytes = input.readNBytesCompat(MAX_IMPORT_BYTES + 1)
                require(bytes.size <= MAX_IMPORT_BYTES) { "File is too large to be a day plan export." }
                bytes.decodeToString()
            }
            PortableDayPlanCodec.decode(text)
        }.onSuccess { importedDays ->
            appViewModel.overwriteDays(importedDays)
            Toast.makeText(
                context,
                context.getString(R.string.import_complete, importedDays.size),
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure { error ->
            val detail = error.message ?: context.getString(R.string.import_failed_unknown)
            Toast.makeText(
                context,
                context.getString(R.string.import_failed, detail),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val payload = PortableDayPlanCodec.encode(uiState.days)
            context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                requireNotNull(writer) { "Unable to open destination file." }
                writer.write(payload)
            }
        }.onSuccess {
            Toast.makeText(
                context,
                context.getString(R.string.export_complete, uiState.days.size),
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure { error ->
            val detail = error.message ?: context.getString(R.string.export_failed_unknown)
            Toast.makeText(
                context,
                context.getString(R.string.export_failed, detail),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Saved days that couldn't be read were replaced by the defaults; say so, and offer the kept copy.
    val unreadableCopy by UnreadableDaysBackup.pending.collectAsState()
    val saveUnreadableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val source = unreadableCopy
        UnreadableDaysBackup.dismiss()
        if (uri == null || source == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri).use { out ->
                requireNotNull(out) { "Unable to open destination file." }
                out.write(source.readBytes())
            }
        }.onFailure { error ->
            Toast.makeText(context, context.getString(R.string.export_failed, error.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }
    unreadableCopy?.let {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.unreadable_days_title)) },
            text = { Text(stringResource(R.string.unreadable_days_message)) },
            confirmButton = {
                TextButton(onClick = { saveUnreadableLauncher.launch("impermanence-unreadable-days.json") }) {
                    Text(stringResource(R.string.unreadable_days_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { UnreadableDaysBackup.dismiss() }) { Text(stringResource(R.string.unreadable_days_ok)) }
            }
        )
    }

    // If a timing session is running (for example after the process was killed, or after a reboot), the
    // app opens on its screen: being on that screen and timing are the same thing.
    var resumeChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading || resumeChecked) return@LaunchedEffect
        resumeChecked = true
        val route = navController.currentBackStackEntry?.destination?.route
        when (val session = SessionEngine.session.value) {
            is TimingSession.DaySession -> {
                val onActive = route?.startsWith(Destinations.DayActive) == true
                if (!onActive) {
                    if (uiState.days.any { it.id == session.day.id }) {
                        navController.navigate("${Destinations.DayActive}/${Uri.encode(session.day.id)}")
                    } else {
                        SessionEngine.end(context, SessionEngine.EndReason.LEFT_SCREEN)
                    }
                }
            }
            is TimingSession.QuickSitSession -> {
                if (route != Destinations.QuickSit) navController.navigate(Destinations.QuickSit)
            }
            null -> Unit
        }
    }

    ImpermanenceTheme {
        // Each screen's own Scaffold handles system bar insets; a second Scaffold here doubled them.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(
                navController = navController,
                startDestination = Destinations.Days,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Destinations.Days) {
                    DaysScreen(
                        uiState = uiState,
                        onDaySelected = { dayId -> navController.navigate("${Destinations.DayDetail}/${Uri.encode(dayId)}") },
                        onQuickSit = { navController.navigate(Destinations.QuickSit) },
                        onSettings = { navController.navigate(Destinations.Settings) },
                        onAbout = { navController.navigate(Destinations.About) },
                        onAddDay = { navController.navigate(Destinations.DayEdit) },
                        onDeleteDay = appViewModel::deleteDay,
                        onMoveDay = appViewModel::moveDay
                    )
                }

                composable(
                    route = "${Destinations.DayDetail}/{dayId}",
                    arguments = listOf(navArgument("dayId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val dayId = backStackEntry.arguments?.getString("dayId")
                    val day = uiState.days.firstOrNull { it.id == dayId }
                    if (day != null) {
                        DayDetailScreen(
                            day = day,
                            use24HourClock = uiState.use24HourClock,
                            onEdit = { navController.navigate("${Destinations.DayEdit}?dayId=${Uri.encode(day.id)}") },
                            onStart = { navController.navigate("${Destinations.DayActive}/${Uri.encode(day.id)}") },
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        PopWhenMissing(isLoading = uiState.isLoading) { navController.popBackStack() }
                    }
                }

                composable(
                    route = "${Destinations.DayActive}/{dayId}",
                    arguments = listOf(navArgument("dayId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val dayId = backStackEntry.arguments?.getString("dayId")
                    val day = uiState.days.firstOrNull { it.id == dayId }
                    if (day != null) {
                        DayActiveScreen(
                            day = day,
                            loopDays = uiState.loopDays,
                            keepScreenAwakeDuringDay = uiState.keepScreenAwakeDuringDay,
                            use24HourClock = uiState.use24HourClock,
                            onExit = {
                                if (navController.currentDestination?.route?.startsWith(Destinations.DayActive) == true) {
                                    navController.popBackStack()
                                }
                            },
                            onManualBellChange = appViewModel::updateDay
                        )
                    } else {
                        PopWhenMissing(isLoading = uiState.isLoading) { navController.popBackStack() }
                    }
                }

                composable(
                    route = Destinations.DayEdit + "?dayId={dayId}",
                    arguments = listOf(navArgument("dayId") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    })
                ) { backStackEntry ->
                    val dayId = backStackEntry.arguments?.getString("dayId").orEmpty()
                    val editingDay: Day? = uiState.days.firstOrNull { it.id == dayId }
                    DayEditScreen(
                        existingDay = editingDay,
                        use24HourClock = uiState.use24HourClock,
                        onDismiss = { navController.popBackStack() },
                        onSave = { day ->
                            if (editingDay == null) {
                                appViewModel.addDay(day)
                            } else {
                                appViewModel.updateDay(day)
                            }
                            navController.popBackStack()
                        }
                    )
                }

                composable(Destinations.QuickSit) {
                    QuickSitScreen(
                        onClose = {
                            if (navController.currentDestination?.route == Destinations.QuickSit) {
                                navController.popBackStack()
                            }
                        }
                    )
                }

                composable(Destinations.Settings) {
                    SettingsScreen(
                        use24HourClock = uiState.use24HourClock,
                        loopDays = uiState.loopDays,
                        keepScreenAwakeDuringDay = uiState.keepScreenAwakeDuringDay,
                        onUse24HourClockChanged = appViewModel::setUse24HourClock,
                        onLoopDaysChanged = appViewModel::setLoopDays,
                        onKeepScreenAwakeDuringDayChanged = appViewModel::setKeepScreenAwakeDuringDay,
                        onImportDays = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        onExportDays = {
                            exportLauncher.launch("impermanence-days-${LocalDate.now()}.json")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Destinations.About) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
