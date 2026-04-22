package com.example.lifetogether.ui.feature.lists

import androidx.compose.runtime.Composable
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.sync.FeatureSyncLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun ListsRoute(
    appNavigator: AppNavigator,
) {
    FeatureSyncLifecycleBinding(
        keys = setOf(SyncKey.USER_LISTS, SyncKey.ROUTINE_LIST_ENTRIES),
    )
    ListsScreen(appNavigator)
}
