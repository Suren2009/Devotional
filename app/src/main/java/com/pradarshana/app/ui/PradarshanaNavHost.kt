package com.pradarshana.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pradarshana.app.ui.ask.AskBabaRoute
import com.pradarshana.app.ui.completion.CompletionRoute
import com.pradarshana.app.ui.counter.CounterRoute
import com.pradarshana.app.ui.history.HistoryRoute
import com.pradarshana.app.ui.history.SessionDetailRoute
import com.pradarshana.app.ui.home.HomeRoute
import com.pradarshana.app.ui.settings.SettingsRoute
import com.pradarshana.app.ui.start.StartRoute
import com.pradarshana.app.ui.stotra.StotraListRoute
import com.pradarshana.app.ui.stotra.StotraReaderRoute
import com.pradarshana.core.data.SessionRepository

object Routes {
    const val HOME = "home"
    const val START = "start"
    const val COUNTER = "counter/{id}"
    const val COMPLETION = "completion/{id}"
    const val HISTORY = "history"
    const val DETAIL = "history/{id}"
    const val SETTINGS = "settings"
    const val ASK = "ask"
    const val STOTRAS = "stotras"
    const val STOTRA = "stotras/{id}"

    fun counter(id: String) = "counter/$id"
    fun completion(id: String) = "completion/$id"
    fun detail(id: String) = "history/$id"
    fun stotra(id: String) = "stotras/$id"
}

@Composable
fun PradarshanaNavHost(
    repository: SessionRepository,
    navController: NavHostController = rememberNavController(),
) {
    // Reopen the counter for a session that was in progress when the app was closed or killed.
    var restoreChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restoreChecked) {
            restoreChecked = true
            repository.getOngoingSession()?.let { navController.navigate(Routes.counter(it.id)) }
        }
    }

    val idArg = listOf(navArgument("id") { type = NavType.StringType })

    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                onStart = { navController.navigate(Routes.START) },
                onResume = { navController.navigate(Routes.counter(it)) },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onAskBaba = { navController.navigate(Routes.ASK) },
                onStotras = { navController.navigate(Routes.STOTRAS) },
            )
        }
        composable(Routes.START) {
            StartRoute(
                onBack = { navController.popBackStack() },
                onStarted = { id ->
                    navController.navigate(Routes.counter(id)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.COUNTER, arguments = idArg) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            CounterRoute(
                sessionId = id,
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onCompleted = {
                    navController.navigate(Routes.completion(id)) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onEnded = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.COMPLETION, arguments = idArg) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            CompletionRoute(
                sessionId = id,
                onStartAnother = {
                    navController.navigate(Routes.START) { popUpTo(Routes.HOME) }
                },
                onHistory = {
                    navController.navigate(Routes.HISTORY) { popUpTo(Routes.HOME) }
                },
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.ASK) {
            AskBabaRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.STOTRAS) {
            StotraListRoute(
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate(Routes.stotra(it.file)) },
            )
        }
        composable(Routes.STOTRA, arguments = idArg) { entry ->
            StotraReaderRoute(
                file = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            HistoryRoute(
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate(Routes.detail(it)) },
            )
        }
        composable(Routes.DETAIL, arguments = idArg) { entry ->
            SessionDetailRoute(
                sessionId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
