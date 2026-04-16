package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun RecipesRoute(
    appNavigator: AppNavigator,
) {
    // TODO [Issue #3]: remove bridge after RecipesScreen migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.RECIPES),
    )
    RecipesScreen(appNavigator, appSessionViewModel)
}
