package com.example.lifetogether.ui.feature.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.ui.common.event.CollectUiCommands
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun SignupRoute(
    appNavigator: AppNavigator,
) {
    val viewModel: SignUpViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectUiCommands(viewModel.uiCommands)

    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                SignupCommand.NavigateToProfile -> appNavigator.navigateToProfile()
            }
        }
    }

    SignupScreen(
        uiState = uiState,
        onUiEvent = viewModel::onEvent,
        onNavigationEvent = { navigationEvent ->
            when (navigationEvent) {
                SignupNavigationEvent.NavigateBack -> appNavigator.navigateBack()
                SignupNavigationEvent.LoginClicked -> appNavigator.navigateToLogin()
            }
        },
    )
}
