package com.example.lifetogether.ui.feature.guides.stepplayer

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun GuideStepPlayerRoute(
    appNavigator: AppNavigator? = null,
    guideId: String,
    viewModelStoreOwner: ViewModelStoreOwner? = null,
) {
    val guideStepPlayerViewModel: GuideStepPlayerViewModel = if (viewModelStoreOwner != null) {
        hiltViewModel(viewModelStoreOwner)
    } else {
        hiltViewModel()
    }

    GuideStepPlayerScreen(
        appNavigator = appNavigator,
        guideId = guideId,
        guideStepPlayerViewModel = guideStepPlayerViewModel,
    )
}