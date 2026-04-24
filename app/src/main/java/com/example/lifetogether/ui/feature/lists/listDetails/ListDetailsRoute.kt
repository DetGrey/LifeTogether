package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun ListDetailsRoute(
    listId: String,
    appNavigator: AppNavigator,
) {
    FeatureObserverLifecycleBinding(
        keys = setOf(ObserverKey.USER_LISTS, ObserverKey.ROUTINE_LIST_ENTRIES),
    )
    ListDetailsScreen(
        listId = listId,
        appNavigator = appNavigator,
    )
}
