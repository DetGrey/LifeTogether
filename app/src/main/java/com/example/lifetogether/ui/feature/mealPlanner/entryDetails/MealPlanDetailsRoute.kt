package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.feature.mealPlanner.MEAL_PLANNER_FOCUS_DATE_RESULT_KEY
import com.example.lifetogether.ui.navigation.RecipeDetailsNavRoute

@Composable
fun MealPlanDetailsRoute(
    appNavigator: AppNavigator,
    navController: NavHostController,
) {
    val viewModel: MealPlanDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUiState by rememberUpdatedState(uiState)
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()
    val mealPlanId = viewModel.mealPlanId

    CollectUiCommands(viewModel.uiCommands)
    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                MealPlanDetailsCommand.NavigateBack -> {
                    val content = currentUiState as? MealPlanDetailsUiState.Content
                    val form = content?.form
                    if (mealPlanId == null && form != null) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            MEAL_PLANNER_FOCUS_DATE_RESULT_KEY,
                            form.date,
                        )
                    }
                    appNavigator.navigateBack()
                }
            }
        }
    }

    MealPlanDetailsScreen(
        uiState = uiState,
        mealPlanId = mealPlanId,
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
