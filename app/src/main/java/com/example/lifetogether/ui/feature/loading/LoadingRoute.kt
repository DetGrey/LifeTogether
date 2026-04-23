package com.example.lifetogether.ui.feature.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun LoadingRoute(
    appNavigator: AppNavigator,
) {
    val loadingViewModel: LoadingViewModel = hiltViewModel()
    val sessionState by loadingViewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Authenticated -> appNavigator.navigateToHome()
            SessionState.Unauthenticated -> appNavigator.navigateToLogin()
            SessionState.Loading -> Unit
        }
    }

    LoadingScreen()
}
