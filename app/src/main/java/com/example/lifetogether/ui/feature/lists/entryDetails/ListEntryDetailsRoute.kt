package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
    listId: String,
    entryId: String?,
    appNavigator: AppNavigator,
) {
    val viewModel: ListEntryDetailsViewModel = hiltViewModel()
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val familyId by viewModel.familyId.collectAsStateWithLifecycle()
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
    var showImageUploadDialog by remember { mutableStateOf(false) }

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(listId, entryId) {
        if (listId.isNotBlank()) {
            viewModel.setUp(listId, entryId) { appNavigator.navigateBack() }
        }
    }

    ListEntryDetailsScreen(
        screenState = screenState,
        entryId = entryId,
        familyId = familyId,
        bitmap = bitmap,
        showImageUploadDialog = showImageUploadDialog,
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
                ListEntryDetailsUiEvent.RequestImageUpload -> showImageUploadDialog = true
                ListEntryDetailsUiEvent.DismissImageUpload,
                ListEntryDetailsUiEvent.ConfirmImageUpload -> showImageUploadDialog = false
            }
        },
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                ListEntryDetailsNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
