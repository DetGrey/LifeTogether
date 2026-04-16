package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun ListEntryDetailsRoute(
    listId: String,
    entryId: String?,
    appNavigator: AppNavigator,
) {
    // TODO [Issue #3]: remove bridge after ListEntryDetailsScreen migrates off AppSessionViewModel
    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    ListEntryDetailsScreen(
        listId = listId,
        entryId = entryId,
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
    )
}
