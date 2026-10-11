package com.pradarshana.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.pradarshana.core.data.SessionRepository

private object WearRoutes {
    const val HOME = "home"
    const val TARGET = "target"
    const val CUSTOM = "custom"
    const val COUNTER = "counter/{id}"
    const val COMPLETION = "completion/{id}"
    fun counter(id: String) = "counter/$id"
    fun completion(id: String) = "completion/$id"
}

@Composable
fun WearNavHost(
    repository: SessionRepository,
    navController: NavHostController = rememberSwipeDismissableNavController(),
) {
    var restoreChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!restoreChecked) {
            restoreChecked = true
            repository.getOngoingSession()?.let { navController.navigate(WearRoutes.counter(it.id)) }
        }
    }

    SwipeDismissableNavHost(navController = navController, startDestination = WearRoutes.HOME) {
        composable(WearRoutes.HOME) {
            val vm = wearViewModel { WearHomeViewModel(it.repository) }
            val ongoing by vm.ongoing.collectAsStateWithLifecycle()
            WearHomeScreen(
                ongoing = ongoing,
                onResume = { navController.navigate(WearRoutes.counter(it)) },
                onStart = { navController.navigate(WearRoutes.TARGET) },
            )
        }
        composable(WearRoutes.TARGET) {
            val vm = wearViewModel { WearHomeViewModel(it.repository) }
            LaunchedEffect(vm) {
                vm.started.collect { id ->
                    navController.navigate(WearRoutes.counter(id)) { popUpTo(WearRoutes.HOME) }
                }
            }
            WearTargetScreen(onSelect = vm::start, onCustom = { navController.navigate(WearRoutes.CUSTOM) })
        }
        composable(WearRoutes.CUSTOM) {
            val vm = wearViewModel { WearHomeViewModel(it.repository) }
            LaunchedEffect(vm) {
                vm.started.collect { id ->
                    navController.navigate(WearRoutes.counter(id)) { popUpTo(WearRoutes.HOME) }
                }
            }
            WearCustomTargetScreen(onStart = vm::start)
        }
        composable(WearRoutes.COUNTER) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            WearCounterRoute(
                sessionId = id,
                onCompleted = {
                    navController.navigate(WearRoutes.completion(id)) {
                        popUpTo(WearRoutes.HOME)
                        launchSingleTop = true
                    }
                },
                onEnded = { navController.popBackStack(WearRoutes.HOME, inclusive = false) },
            )
        }
        composable(WearRoutes.COMPLETION) { entry ->
            WearCompletionRoute(
                sessionId = entry.arguments?.getString("id").orEmpty(),
                onDone = { navController.popBackStack(WearRoutes.HOME, inclusive = false) },
            )
        }
    }
}
