package com.example.lifetogether.ui.feature.family

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.logic.copyToClipboard
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.ImageViewModel
import kotlinx.coroutines.launch

@Composable
fun FamilyRoute(
    appNavigator: AppNavigator,
) {
    val context = LocalContext.current
    val viewModel: FamilyViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bitmap by imageViewModel.bitmap.collectAsStateWithLifecycle()
    val snackbarHostState = LocalRootSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(uiState.familyId) {
        uiState.familyId?.let { familyId ->
            imageViewModel.collectImageFlow(
                imageType = ImageType.FamilyImage(familyId),
                onError = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                },
            )
        }
    }

    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                FamilyCommand.NavigateBack -> appNavigator.navigateBack()
                is FamilyCommand.CopyFamilyId -> copyToClipboard(
                    context = context,
                    text = command.familyId,
                )
            }
        }
    }

    FamilyScreen(
        uiState = uiState,
        bitmap = bitmap,
        showImageUploadDialog = imageViewModel.showImageUploadDialog,
        onUiEvent = { event ->
            when (event) {
                FamilyUiEvent.AddImageClicked -> imageViewModel.showImageUploadDialog = true
                FamilyUiEvent.ImageUploadDismissed,
                FamilyUiEvent.ImageUploadConfirmed -> imageViewModel.showImageUploadDialog = false
                else -> viewModel.onEvent(event)
            }
        },
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                FamilyNavigationEvent.NavigateBack -> appNavigator.navigateBack()
            }
        },
    )
}
