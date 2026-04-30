package com.example.lifetogether.ui.feature.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.ui.common.di.rememberSessionRepository
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.HomeNavRoute
import com.example.lifetogether.ui.navigation.LoginNavRoute

@Composable
fun LoadingRoute(
    appNavigator: AppNavigator,
) {
    val sessionRepository = rememberSessionRepository()
    val sessionState by sessionRepository.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Authenticated -> appNavigator.navigate(HomeNavRoute)
            SessionState.Unauthenticated -> appNavigator.navigate(LoginNavRoute)
            SessionState.Loading -> Unit
        }
    }

    LoadingScreen()
}