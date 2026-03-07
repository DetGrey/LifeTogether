package com.example.lifetogether.ui.feature.guides

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun GuidesRoute(
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
) {
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    if (!userInformation?.familyId.isNullOrBlank() && !userInformation?.uid.isNullOrBlank()) {
        FeatureObserverLifecycleBinding(
            appSessionViewModel = appSessionViewModel,
            keys = setOf(ObserverKey.GUIDES),
            uid = userInformation?.uid,
            familyId = userInformation?.familyId,
        )
    }

    GuidesScreen(appNavigator, appSessionViewModel)
}
