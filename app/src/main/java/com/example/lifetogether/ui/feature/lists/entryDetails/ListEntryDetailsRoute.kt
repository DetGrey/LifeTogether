package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.common.image.rememberObservedImageBitmap
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.feature.lists.listDetails.MEAL_PLAN_FOCUS_DATE_RESULT_KEY
import com.example.lifetogether.ui.navigation.RecipeDetailsNavRoute
import kotlinx.coroutines.launch

@Composable
fun ListEntryDetailsRoute(
    appNavigator: AppNavigator,
    navController: NavHostController,
) {
    val viewModel: ListEntryDetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUiState by rememberUpdatedState(uiState)
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()
    val entryId = viewModel.entryId
    val content = uiState as? EntryDetailsUiState.Content
    val imageType = when (content?.details) {
        is EntryDetailsContent.Routine ->
            if (!familyId.isNullOrBlank() && !entryId.isNullOrBlank()) {
                ImageType.RoutineListEntryImage(familyId!!, entryId)
            } else {
                null
            }

        else -> null
    }
    val snackbarHostState = LocalRootSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val bitmap = rememberObservedImageBitmap(imageType) { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    CollectUiCommands(viewModel.uiCommands)
    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                ListEntryDetailsCommand.NavigateBack -> {
                    val content = currentUiState as? EntryDetailsUiState.Content
                    val mealDetails = content?.details as? EntryDetailsContent.Meal
                    if (entryId == null && mealDetails != null) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            MEAL_PLAN_FOCUS_DATE_RESULT_KEY,
                            mealDetails.form.date,
                        )
                    }
                    appNavigator.navigateBack()
                }
            }
        }
    }

    ListEntryDetailsScreen(
        uiState = uiState,
        entryId = entryId,
        familyId = familyId,
        bitmap = bitmap,
        onUiEvent = viewModel::onUiEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                ListEntryDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                is ListEntryDetailsNavigationEvent.NavigateToRecipeDetails -> {
                    appNavigator.navigate(RecipeDetailsNavRoute(navigationEvent.recipeId))
                }
            }
        },
    )
}
