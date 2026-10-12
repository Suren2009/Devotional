package com.pradarshana.wear.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.location.FusedLocationCapture
import com.pradarshana.core.data.location.hasLocationPermission

@Composable
private fun rememberStartWithLocation(onStart: (target: Int, captureLocation: Boolean) -> Unit): (Int) -> Unit {
    val context = LocalContext.current
    var pendingTarget by androidx.compose.runtime.remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (pendingTarget > 0) onStart(pendingTarget, granted || context.hasLocationPermission())
    }
    return { target ->
        if (context.hasLocationPermission()) {
            onStart(target, true)
        } else {
            pendingTarget = target
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}

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
            val context = LocalContext.current
            val vm = wearViewModel { WearHomeViewModel(it.repository, FusedLocationCapture(context.applicationContext)) }
            val start = rememberStartWithLocation { target, allowed -> vm.start(target, allowed) }
            LaunchedEffect(vm) {
                vm.started.collect { id ->
                    navController.navigate(WearRoutes.counter(id)) { popUpTo(WearRoutes.HOME) }
                }
            }
            WearTargetScreen(onSelect = start, onCustom = { navController.navigate(WearRoutes.CUSTOM) })
        }
        composable(WearRoutes.CUSTOM) {
            val context = LocalContext.current
            val vm = wearViewModel { WearHomeViewModel(it.repository, FusedLocationCapture(context.applicationContext)) }
            val start = rememberStartWithLocation { target, allowed -> vm.start(target, allowed) }
            LaunchedEffect(vm) {
                vm.started.collect { id ->
                    navController.navigate(WearRoutes.counter(id)) { popUpTo(WearRoutes.HOME) }
                }
            }
            WearCustomTargetScreen(onStart = start)
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
