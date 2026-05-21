package com.example.lifetogether.ui.feature.mealPlanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.MealPlanDetailsNavRoute
import com.example.lifetogether.ui.navigation.NavigationResult

@Composable
fun MealPlannerRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: MealPlannerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(appNavigator) {
        appNavigator.navigationResults.collect { result ->
            when (result) {
                is NavigationResult.MealPlannerFocusDate -> viewModel.setFocusDate(result.date)
            }
        }
    }

    MealPlannerScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                MealPlannerNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                is MealPlannerNavigationEvent.NavigateToCreateMealPlan -> {
                    appNavigator.navigate(MealPlanDetailsNavRoute(defaultDate = navigationEvent.defaultDate))
                }
                is MealPlannerNavigationEvent.NavigateToMealPlanDetails -> {
                    appNavigator.navigate(MealPlanDetailsNavRoute(navigationEvent.mealPlanId))
                }
            }
        },
    )
}
