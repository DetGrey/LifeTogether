package com.example.lifetogether.ui.feature.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.ui.common.text.TextDisplayLarge
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun LoadingScreen(
    appNavigator: AppNavigator? = null,
) {
    val loadingViewModel: LoadingViewModel = hiltViewModel()
    val sessionState by loadingViewModel.sessionState.collectAsStateWithLifecycle()

    when (sessionState) {
        is SessionState.Authenticated -> appNavigator?.navigateToHome()
        SessionState.Unauthenticated -> appNavigator?.navigateToLogin()
        SessionState.Loading -> {}
    }

    Column(
        modifier = Modifier
            .padding(bottom = 60.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo_small),
                contentDescription = "LifeTogether logo",
            )
        }
        TextDisplayLarge("LifeTogether")
    }
}
