package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.ImageViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun ListEntryDetailsRoute(
    listId: String,
    entryId: String?,
    appNavigator: AppNavigator,
) {
    val viewModel: ListEntryDetailsViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()
    val bitmap by imageViewModel.bitmap.collectAsStateWithLifecycle()
    val snackbarHostState = LocalRootSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(listId, entryId) {
        if (listId.isNotBlank()) {
            viewModel.setUp(listId, entryId) { appNavigator.navigateBack() }
        }
    }

    LaunchedEffect(familyId, entryId) {
        if (!familyId.isNullOrBlank() && entryId != null) {
            imageViewModel.collectImageFlow(
                imageType = ImageType.RoutineListEntryImage(familyId!!, entryId),
                onError = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                },
            )
        }
    }

    ListEntryDetailsScreen(
        screenState = screenState,
        entryId = entryId,
        familyId = familyId,
        bitmap = bitmap,
        showImageUploadDialog = imageViewModel.showImageUploadDialog,
        onUiEvent = { event ->
            when (event) {
                ListEntryDetailsUiEvent.EnterEditMode -> viewModel.enterEditMode()
                ListEntryDetailsUiEvent.RequestCancelEdit -> viewModel.requestCancelEdit()
                ListEntryDetailsUiEvent.ConfirmDiscard -> viewModel.confirmDiscard()
                ListEntryDetailsUiEvent.DismissDiscardDialog -> viewModel.dismissDiscardDialog()
                is ListEntryDetailsUiEvent.NameChanged -> viewModel.onNameChange(event.value)
                is ListEntryDetailsUiEvent.RecurrenceUnitChanged -> viewModel.onRecurrenceUnitChange(event.value)
                is ListEntryDetailsUiEvent.IntervalChanged -> viewModel.onIntervalChange(event.value)
                is ListEntryDetailsUiEvent.SelectedWeekdaysChanged -> viewModel.onSelectedWeekdaysChange(event.dayNum)
                ListEntryDetailsUiEvent.SaveClicked -> viewModel.save { appNavigator.navigateBack() }
                is ListEntryDetailsUiEvent.ImageSelected -> {
                    viewModel.onImageSelected(event.uri, context.contentResolver)
                }
                ListEntryDetailsUiEvent.RequestImageUpload -> imageViewModel.showImageUploadDialog = true
                ListEntryDetailsUiEvent.DismissImageUpload,
                ListEntryDetailsUiEvent.ConfirmImageUpload -> imageViewModel.showImageUploadDialog = false
            }
        },
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                ListEntryDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
