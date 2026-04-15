package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun ListDetailsRoute(
    listId: String,
    appNavigator: AppNavigator,
) {
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.USER_LISTS, ObserverKey.ROUTINE_LIST_ENTRIES),
    )
    // TODO [Issue #3]: remove bridge after ListDetailsScreen migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    ListDetailsScreen(
        listId = listId,
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
    )
}
