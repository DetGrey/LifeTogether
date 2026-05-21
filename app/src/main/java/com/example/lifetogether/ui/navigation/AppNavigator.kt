package com.example.lifetogether.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppNavigator(private val backStack: NavBackStack<NavKey>) : Navigator {

    private val _navigationResults = MutableSharedFlow<NavigationResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val navigationResults: SharedFlow<NavigationResult> = _navigationResults.asSharedFlow()

    override fun navigate(route: AppRoute) {
        if (backStack.lastOrNull() == route) return
        backStack.add(route)
    }

    fun clearAndNavigate(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }

    fun navigateTopLevel(route: AppRoute) {
        when (route) {
            is HomeNavRoute -> popToHome()
            is ProfileNavRoute -> navigateProfileOrSettings(route)
            is SettingsNavRoute -> navigateProfileOrSettings(route)
            is TipTrackerNavRoute -> {
                popToHome()
                backStack.add(TipTrackerGraph)
                backStack.add(TipTrackerNavRoute)
            }
            else -> {
                popToHome()
                backStack.add(route)
            }
        }
    }

    override fun navigateBack() {
        if (backStack.size <= 1) {
            if (backStack.lastOrNull() !is HomeNavRoute) {
                if (backStack.isNotEmpty()) backStack.removeLast()
                backStack.add(HomeNavRoute)
            }
            return
        }
        backStack.removeLast()
        // Auto-pop invisible graph markers left exposed at the top
        if (backStack.size > 1 && backStack.lastOrNull() is TipTrackerGraph) {
            backStack.removeLast()
        }
    }

    fun navigateBack(result: NavigationResult) {
        _navigationResults.tryEmit(result)
        navigateBack()
    }

    private fun navigateProfileOrSettings(route: AppRoute) {
        when (route) {
            is ProfileNavRoute -> {
                if (isCurrentRoute<SettingsNavRoute>() && isPreviousRoute<ProfileNavRoute>()) {
                    backStack.removeLast()
                } else if (!isCurrentRoute<ProfileNavRoute>()) {
                    if (!isCurrentRoute<HomeNavRoute>() && !isCurrentRoute<SettingsNavRoute>()) {
                        popToHome()
                    }
                    backStack.add(route)
                }
            }
            is SettingsNavRoute -> {
                if (isCurrentRoute<ProfileNavRoute>() && isPreviousRoute<SettingsNavRoute>()) {
                    backStack.removeLast()
                } else if (!isCurrentRoute<SettingsNavRoute>()) {
                    if (!isCurrentRoute<HomeNavRoute>() && !isCurrentRoute<ProfileNavRoute>()) {
                        popToHome()
                    }
                    backStack.add(route)
                }
            }
            else -> Unit
        }
    }

    private fun popToHome() {
        val homeIndex = backStack.indexOfFirst { it is HomeNavRoute }
        if (homeIndex == -1) {
            backStack.clear()
            backStack.add(HomeNavRoute)
        } else {
            while (backStack.size > homeIndex + 1) backStack.removeLast()
        }
    }

    private inline fun <reified T : AppRoute> isCurrentRoute(): Boolean =
        backStack.lastOrNull() is T

    private inline fun <reified T : AppRoute> isPreviousRoute(): Boolean {
        val size = backStack.size
        return size >= 2 && backStack[size - 2] is T
    }
}
