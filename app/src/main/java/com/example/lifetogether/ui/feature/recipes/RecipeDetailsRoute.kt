package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.ImageViewModel
import androidx.navigation.NavBackStackEntry
import com.example.lifetogether.ui.navigation.AppRoutes

@Composable
fun CreateRecipeRoute(
    appNavigator: AppNavigator,
) {
    RecipeDetailsDestination(
        recipeId = null,
        appNavigator = appNavigator,
    )
}

@Composable
fun RecipeDetailsRoute(
    backStackEntry: NavBackStackEntry,
    appNavigator: AppNavigator,
) {
    val recipeId = backStackEntry.arguments?.getString(AppRoutes.RECIPE_ID_ARG)
    RecipeDetailsDestination(
        recipeId = recipeId,
        appNavigator = appNavigator,
    )
}

@Composable
private fun RecipeDetailsDestination(
    recipeId: String?,
    appNavigator: AppNavigator,
) {
    val viewModel: RecipeDetailsViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bitmap by imageViewModel.bitmap.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                RecipeDetailsCommand.NavigateBack -> appNavigator.navigateBack()
            }
        }
    }

    LaunchedEffect(recipeId) {
        viewModel.setUp(recipeId)
    }

    LaunchedEffect((uiState as? RecipeDetailsUiState.Content)?.familyId, recipeId) {
        val content = uiState as? RecipeDetailsUiState.Content ?: return@LaunchedEffect
        val familyId = content.familyId ?: return@LaunchedEffect
        val currentRecipeId = content.recipeId ?: return@LaunchedEffect

        imageViewModel.collectImageFlow(
            imageType = ImageType.RecipeImage(familyId, currentRecipeId),
            onError = { message ->
                viewModel.onImageError(message)
            },
        )
    }

    RecipeDetailsScreen(
        uiState = uiState,
        bitmap = bitmap,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                RecipeDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
