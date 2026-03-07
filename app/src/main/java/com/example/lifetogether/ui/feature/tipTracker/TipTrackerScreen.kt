package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.observer.ObserverUpdatingText
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel
import com.example.lifetogether.domain.observer.ObserverKey

@Composable
fun TipTrackerScreen(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
    tipTrackerViewModel: TipTrackerViewModel,
) {

    val userInformationState by appSessionViewModel.userInformation.collectAsState()
    val uiState by tipTrackerViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = userInformationState?.familyId) {
        userInformationState?.familyId?.let { tipTrackerViewModel.setUpTipTracker(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(
                        resId = R.drawable.ic_back_arrow,
                        description = "back arrow icon",
                    ),
                    onLeftClick = {
                        appNavigator?.navigateBack()
                    },
                    text = "Tip Tracker",
                    rightIcon = Icon(
                        resId = R.drawable.ic_back_arrow, // TODO make statistics icon
                        description = "back arrow icon",
                    ),
                    onRightClick = {
                        appNavigator?.navigateToTipStatistics()
                    },
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ObserverUpdatingText(
                        appSessionViewModel = appSessionViewModel,
                        keys = setOf(ObserverKey.TIP_TRACKER),
                    )

                    if (uiState.tips.isNotEmpty()) {
                        TagOptionRow(
                            options = listOf("Calendar", "List"),
                            selectedOption = uiState.overviewOption,
                            onSelectedOptionChange = {
                                tipTrackerViewModel.setOverviewOption(it)
                            },
                            center = true,
                        )
                        when (uiState.overviewOption) {
                            "Calendar" -> {
                                TipsCalendar(uiState.groupedTips)
                            }

                            "List" -> {
                                TipsList(
                                    uiState.groupedTips,
                                    onDeleteClick = {
                                        tipTrackerViewModel.setSelectedTip(it)
                                        tipTrackerViewModel.setShowConfirmationDialog(true)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- ADD NEW TIP ITEM
    Box(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AddNewTipItem(
            textValue = uiState.newItemAmount,
            onTextChange = { tipTrackerViewModel.setNewItemAmount(it) },
            onAddClick = {
                tipTrackerViewModel.addItemToList {
                    tipTrackerViewModel.setShowConfirmationDialog(false)
                }
            },
            dateValue = uiState.newItemDate,
            onDateChange = {
                tipTrackerViewModel.setNewItemDate(it)
            },
        )
    }

    // ---------------------------------------------------------------- CONFIRM DELETION OF COMPLETED ITEMS
    if (uiState.showConfirmationDialog && uiState.selectedTip != null) {
        ConfirmationDialog(
            onDismiss = { tipTrackerViewModel.setShowConfirmationDialog(false) },
            onConfirm = {
                tipTrackerViewModel.deleteItem()
            },
            dialogTitle = "Delete tip",
            dialogMessage = "Are you sure you want to delete this tip?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete",
        )
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (uiState.showAlertDialog) {
        LaunchedEffect(uiState.error) {
            tipTrackerViewModel.dismissAlert()
        }
        ErrorAlertDialog(uiState.error)
    }
}
