package com.example.lifetogether.ui.feature.guides.details

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun GuideDetailsRoute(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
    guideId: String,
    viewModelStoreOwner: ViewModelStoreOwner? = null,
) {
    val guideDetailsViewModel: GuideDetailsViewModel = if (viewModelStoreOwner != null) {
        hiltViewModel(viewModelStoreOwner)
    } else {
        hiltViewModel()
    }

    GuideDetailsScreen(
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
        guideId = guideId,
        guideDetailsViewModel = guideDetailsViewModel,
    )
}