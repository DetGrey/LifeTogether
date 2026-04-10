package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.runtime.Composable
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun ListEntryDetailsRoute(
    listId: String,
    entryId: String?,
    appNavigator: AppNavigator,
    appSessionViewModel: AppSessionViewModel,
) {
    ListEntryDetailsScreen(
        listId = listId,
        entryId = entryId,
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
    )
}
