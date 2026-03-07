package com.example.lifetogether.ui.feature.guides.stepplayer

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun GuideStepPlayerRoute(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
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
        appSessionViewModel = appSessionViewModel,
        guideId = guideId,
        guideStepPlayerViewModel = guideStepPlayerViewModel,
    )
}