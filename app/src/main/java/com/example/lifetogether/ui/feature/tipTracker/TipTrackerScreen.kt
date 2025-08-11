package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel

@Composable
fun TipTrackerScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
) {
    val tipTrackerViewModel: TipTrackerViewModel = hiltViewModel()

    val userInformationState by firebaseViewModel?.userInformation!!.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        println("TipTracker familyId: ${userInformationState?.familyId}")
        userInformationState?.familyId?.let { tipTrackerViewModel.setUpTipTracker(it) }
    }

    // Collecting the StateFlows as state
    val tipItems by tipTrackerViewModel.tips.collectAsState()

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
                )
            }

            item {
                if (tipItems.isNotEmpty()) {
                    TagOptionRow(
                        options = listOf("Week", "Month", "Year", "All"),
                        selectedOption = tipTrackerViewModel.timePeriod,
                        onSelectedOptionChange = {
                            tipTrackerViewModel.timePeriod = it
                        },
                        center = true,
                    )
                    StatsCard(
                        title = when (tipTrackerViewModel.timePeriod) {
                            "Week" -> "Last 7 days"
                            "Month" -> "Last 30 days"
                            "Year" -> "Last 365 days"
                            else -> "All time"
                        },
                        total = when (tipTrackerViewModel.timePeriod) {
                            "Week" -> tipTrackerViewModel.weeklyTotal.toString()
                            "Month" -> tipTrackerViewModel.monthlyTotal.toString()
                            "Year" -> tipTrackerViewModel.yearlyTotal.toString()
                            else -> tipTrackerViewModel.total.toString()
                        },
                        average = when (tipTrackerViewModel.timePeriod) {
                            "Week" -> tipTrackerViewModel.weeklyAverage.toString()
                            "Month" -> tipTrackerViewModel.monthlyAverage.toString()
                            "Year" -> tipTrackerViewModel.yearlyAverage.toString()
                            else -> tipTrackerViewModel.totalAverage.toString()
                        },

                    )
                }
            }

            item {
                if (tipItems.isNotEmpty()) {
                    TagOptionRow(
                        options = listOf("Calendar", "List"),
                        selectedOption = tipTrackerViewModel.overviewOption,
                        onSelectedOptionChange = {
                            tipTrackerViewModel.overviewOption = it
                        },
                        center = true,
                    )
                    when (tipTrackerViewModel.overviewOption) {
                        "Calendar" -> {
                            TipsCalendar(tipTrackerViewModel.groupedTips)
                        }

                        "List" -> {
                            TipsList(
                                tipTrackerViewModel.groupedTips,
                                onDeleteClick = {
                                    tipTrackerViewModel.selectedTip = it
                                    tipTrackerViewModel.showConfirmationDialog = true
                                },
                            )
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
            textValue = tipTrackerViewModel.newItemAmount,
            onTextChange = { tipTrackerViewModel.newItemAmount = it },
            onAddClick = {
                tipTrackerViewModel.addItemToList {
                    tipTrackerViewModel.showConfirmationDialog = false
                }
            },
            dateValue = tipTrackerViewModel.newItemDate,
            onDateChange = {
                tipTrackerViewModel.newItemDate = it
            },
        )
    }

    // ---------------------------------------------------------------- CONFIRM DELETION OF COMPLETED ITEMS
    if (tipTrackerViewModel.showConfirmationDialog && tipTrackerViewModel.selectedTip != null) {
        ConfirmationDialog(
            onDismiss = { tipTrackerViewModel.showConfirmationDialog = false },
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
    if (tipTrackerViewModel.showAlertDialog) {
        ErrorAlertDialog(tipTrackerViewModel.error)
        tipTrackerViewModel.toggleAlertDialog()
    }
}
