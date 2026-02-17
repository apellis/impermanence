package com.impermanence.impermanence.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
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
import com.impermanence.impermanence.AppContainer
import com.impermanence.impermanence.R
import com.impermanence.impermanence.data.portability.PortableDayPlanCodec
import com.impermanence.impermanence.model.Day
import com.impermanence.impermanence.ui.screens.about.AboutScreen
import com.impermanence.impermanence.ui.screens.active.DayActiveScreen
import com.impermanence.impermanence.ui.screens.days.DaysScreen
import com.impermanence.impermanence.ui.screens.detail.DayDetailScreen
import com.impermanence.impermanence.ui.screens.edit.DayEditScreen
import com.impermanence.impermanence.ui.screens.quick.QuickSitScreen
import com.impermanence.impermanence.ui.screens.settings.SettingsScreen
import com.impermanence.impermanence.ui.theme.ImpermanenceTheme
import com.impermanence.impermanence.ui.viewmodel.AppViewModel
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
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                requireNotNull(reader) { "Unable to open selected file." }
                reader.readText()
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

    ImpermanenceTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Destinations.Days,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Destinations.Days) {
                    DaysScreen(
                        uiState = uiState,
                        paddingValues = paddingValues,
                        onDaySelected = { dayId -> navController.navigate("${Destinations.DayDetail}/$dayId") },
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
                            onEdit = { navController.navigate("${Destinations.DayEdit}?dayId=${day.id}") },
                            onStart = { navController.navigate("${Destinations.DayActive}/$dayId") },
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        navController.popBackStack()
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
                            onExit = { navController.popBackStack() },
                            onManualBellChange = appViewModel::updateDay
                        )
                    } else {
                        navController.popBackStack()
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
                        use24HourClock = uiState.use24HourClock,
                        onClose = { navController.popBackStack() }
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
