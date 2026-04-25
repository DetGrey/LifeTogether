package com.example.lifetogether.ui.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.BuildConfig
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.common.event.LocalRootSnackbarHostState
import com.example.lifetogether.ui.common.image.rememberObservedImageBitmap
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.HomeNavRoute
import com.example.lifetogether.ui.navigation.SettingsNavRoute
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageType = uiState.userInformation?.uid?.let { ImageType.ProfileImage(it) }
    val snackbarHostState = LocalRootSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val bitmap = rememberObservedImageBitmap(imageType) { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
    var showImageUploadDialog by remember { mutableStateOf(false) }
    val isAdmin = uiState.userInformation?.uid in BuildConfig.ADMIN_LIST.split(",")

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                ProfileCommand.NavigateToHome -> appNavigator.navigate(HomeNavRoute)
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        bitmap = bitmap,
        isAdmin = isAdmin,
        showImageUploadDialog = showImageUploadDialog,
        onUiEvent = { event ->
            when (event) {
                ProfileUiEvent.AddImageClicked -> showImageUploadDialog = true
                ProfileUiEvent.ImageUploadDismissed,
                ProfileUiEvent.ImageUploadConfirmed -> showImageUploadDialog = false
                else -> viewModel.onEvent(event)
            }
        },
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                ProfileNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                ProfileNavigationEvent.NavigateToSettings -> appNavigator.navigate(SettingsNavRoute)
            }
        },
    )
}
