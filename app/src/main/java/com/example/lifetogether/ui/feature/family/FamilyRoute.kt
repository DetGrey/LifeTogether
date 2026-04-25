package com.example.lifetogether.ui.feature.family

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
import com.example.lifetogether.domain.logic.copyToClipboard
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.common.image.rememberObservedImageBitmap
import com.example.lifetogether.ui.navigation.AppNavigator
import kotlinx.coroutines.launch

@Composable
fun FamilyRoute(
    appNavigator: AppNavigator,
) {
    val context = LocalContext.current
    val viewModel: FamilyViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageType = uiState.familyId?.let { ImageType.FamilyImage(it) }
    val snackbarHostState = LocalRootSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val bitmap = rememberObservedImageBitmap(imageType) { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
    var showImageUploadDialog by remember { mutableStateOf(false) }

    CollectUiCommands(viewModel.uiCommands)

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
        showImageUploadDialog = showImageUploadDialog,
        onUiEvent = { event ->
            when (event) {
                FamilyUiEvent.AddImageClicked -> showImageUploadDialog = true
                FamilyUiEvent.ImageUploadDismissed,
                FamilyUiEvent.ImageUploadConfirmed -> showImageUploadDialog = false
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
