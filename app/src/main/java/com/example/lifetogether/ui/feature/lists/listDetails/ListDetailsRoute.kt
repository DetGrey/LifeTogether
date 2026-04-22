package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun ListDetailsRoute(
    listId: String,
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.USER_LISTS, SyncKey.ROUTINE_LIST_ENTRIES),
    )
    ListDetailsScreen(
        listId = listId,
        appNavigator = appNavigator,
    )
}
