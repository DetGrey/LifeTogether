package com.example.lifetogether.ui.feature.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.ui.common.di.rememberSessionRepository
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.LoginNavRoute
import com.example.lifetogether.ui.navigation.HomeNavRoute

@Composable
fun LoadingRoute(
    appNavigator: AppNavigator,
) {
    val sessionRepository = rememberSessionRepository()
    val sessionState by sessionRepository.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Authenticated -> appNavigator.clearAndNavigate(HomeNavRoute)
            SessionState.Unauthenticated -> appNavigator.clearAndNavigate(LoginNavRoute)
            SessionState.Loading -> Unit
        }
    }

    LoadingScreen()
}
