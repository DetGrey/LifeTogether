package com.example.lifetogether.ui.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.SignupNavRoute

@Composable
fun LoginRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                LoginCommand.NavigateBackOnSuccess -> appNavigator.navigateBack()
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                LoginNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                LoginNavigationEvent.SignUpClicked -> appNavigator.navigate(SignupNavRoute)
            }
        },
    )
}
