package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.CreateRecipeNavRoute
import com.example.lifetogether.ui.navigation.RecipeDetailsNavRoute

@Composable
fun RecipesRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: RecipesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.RECIPES),
    )

    RecipesScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                RecipesNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                RecipesNavigationEvent.NavigateToCreateRecipe ->
                    appNavigator.navigate(CreateRecipeNavRoute)
                is RecipesNavigationEvent.NavigateToRecipeDetails ->
                    appNavigator.navigate(RecipeDetailsNavRoute(navigationEvent.recipeId))
            }
        },
    )
}
