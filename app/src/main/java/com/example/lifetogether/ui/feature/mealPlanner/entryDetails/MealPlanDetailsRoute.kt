package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.MealPlanDetailsNavRoute
import com.example.lifetogether.ui.navigation.NavigationResult
import com.example.lifetogether.ui.navigation.RecipeDetailsNavRoute

@Composable
fun MealPlanDetailsRoute(
    appNavigator: AppNavigator,
    routeKey: MealPlanDetailsNavRoute,
) {
    val viewModel: MealPlanDetailsViewModel =
        hiltViewModel<MealPlanDetailsViewModel, MealPlanDetailsViewModel.Factory> {
            it.create(routeKey.mealPlanId, routeKey.defaultDate, routeKey.preselectedRecipeId)
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUiState by rememberUpdatedState(uiState)
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)
    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                MealPlanDetailsCommand.NavigateBack -> {
                    val content = currentUiState as? MealPlanDetailsUiState.Content
                    val form = content?.form
                    if (routeKey.mealPlanId == null && form != null) {
                        appNavigator.navigateBack(NavigationResult.MealPlannerFocusDate(form.date))
                    } else {
                        appNavigator.navigateBack()
                    }
                }
            }
        }
    }

    MealPlanDetailsScreen(
        uiState = uiState,
        mealPlanId = routeKey.mealPlanId,
        familyId = familyId,
        onUiEvent = viewModel::onUiEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                MealPlanDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                is MealPlanDetailsNavigationEvent.NavigateToRecipeDetails -> {
                    appNavigator.navigate(RecipeDetailsNavRoute(navigationEvent.recipeId))
                }
            }
        },
    )
}
