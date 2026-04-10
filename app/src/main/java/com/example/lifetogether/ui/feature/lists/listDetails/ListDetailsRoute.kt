package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.ui.common.observer.FeatureObserverLifecycleBinding
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun ListDetailsRoute(
    listId: String,
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
) {
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    if (!userInformation?.familyId.isNullOrBlank() && !userInformation?.uid.isNullOrBlank()) {
        FeatureObserverLifecycleBinding(
            appSessionViewModel = appSessionViewModel,
            keys = setOf(ObserverKey.USER_LISTS, ObserverKey.ROUTINE_LIST_ENTRIES),
            uid = userInformation?.uid,
            familyId = userInformation?.familyId,
        )
    }

    ListDetailsScreen(
        listId = listId,
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
    )
}
