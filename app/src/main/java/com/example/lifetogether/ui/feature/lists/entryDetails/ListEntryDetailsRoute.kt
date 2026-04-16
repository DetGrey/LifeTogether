package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.runtime.Composable
import com.example.lifetogether.ui.navigation.AppNavigator

@Composable
fun ListEntryDetailsRoute(
    listId: String,
    entryId: String?,
    appNavigator: AppNavigator,
) {
    ListEntryDetailsScreen(
        listId = listId,
        entryId = entryId,
        appNavigator = appNavigator,
    )
}
