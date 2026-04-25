package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.image.rememberObservedImageBitmap
import com.example.lifetogether.ui.navigation.AppNavigator

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
    recipeId: String,
    appNavigator: AppNavigator,
) {
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recipeImageType = run {
        val content = uiState as? RecipeDetailsUiState.Content ?: return@run null
        val familyId = content.familyId ?: return@run null
        val currentRecipeId = content.recipeId ?: return@run null
        ImageType.RecipeImage(familyId, currentRecipeId)
    }
    val bitmap = rememberObservedImageBitmap(recipeImageType, viewModel::onImageError)

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
