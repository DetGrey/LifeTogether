package com.example.lifetogether.ui.feature.guides

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun GuidesRoute(
    appNavigator: AppNavigator,
) {
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.GUIDES),
    )
    GuidesScreen(appNavigator)
}
