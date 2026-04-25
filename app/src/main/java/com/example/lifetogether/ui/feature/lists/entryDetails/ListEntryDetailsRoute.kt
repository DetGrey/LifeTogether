package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.common.image.rememberObservedImageBitmap
import com.example.lifetogether.ui.navigation.AppNavigator
import kotlinx.coroutines.launch

@Composable
fun ListEntryDetailsRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: ListEntryDetailsViewModel = hiltViewModel()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()
    val entryId = viewModel.entryId
    val imageType = if (!familyId.isNullOrBlank() && !entryId.isNullOrBlank()) {
        ImageType.RoutineListEntryImage(familyId!!, entryId)
    } else {
        null
    }
    val snackbarHostState = LocalRootSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val bitmap = rememberObservedImageBitmap(imageType) { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    CollectUiCommands(viewModel.uiCommands)

    ListEntryDetailsScreen(
        screenState = screenState,
        entryId = entryId,
        familyId = familyId,
        bitmap = bitmap,
        onImageUpload = viewModel::uploadCurrentEntryImage,
        onUiEvent = viewModel::onUiEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                ListEntryDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
