package com.example.lifetogether.ui.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.BuildConfig
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.ImageViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bitmap by imageViewModel.bitmap.collectAsStateWithLifecycle()
    val snackbarHostState = LocalRootSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val isAdmin = uiState.userInformation?.uid in BuildConfig.ADMIN_LIST.split(",")

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(uiState.userInformation?.uid) {
        uiState.userInformation?.uid?.let { uid ->
            imageViewModel.collectImageFlow(
                imageType = ImageType.ProfileImage(uid),
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
                ProfileCommand.NavigateToHome -> appNavigator.navigateToHome()
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        bitmap = bitmap,
        isAdmin = isAdmin,
        showImageUploadDialog = imageViewModel.showImageUploadDialog,
        onUiEvent = { event ->
            when (event) {
                ProfileUiEvent.AddImageClicked -> imageViewModel.showImageUploadDialog = true
                ProfileUiEvent.ImageUploadDismissed,
                ProfileUiEvent.ImageUploadConfirmed -> imageViewModel.showImageUploadDialog = false
                else -> viewModel.onEvent(event)
            }
        },
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                ProfileNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                ProfileNavigationEvent.NavigateToSettings -> appNavigator.navigateToSettings()
            }
        },
    )
}
