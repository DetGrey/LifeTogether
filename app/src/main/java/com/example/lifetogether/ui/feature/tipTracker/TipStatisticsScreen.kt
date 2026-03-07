package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun TipStatisticsScreen(
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
                    text = "Tip Statistics",
                )
            }

            item {
                if (uiState.tips.isNotEmpty()) {
                    TagOptionRow(
                        options = listOf("Week", "Month", "Year", "All"),
                        selectedOption = uiState.timePeriod,
                        onSelectedOptionChange = {
                            tipTrackerViewModel.setTimePeriod(it)
                        },
                        center = true,
                    )
                    StatsCard(
                        title = when (uiState.timePeriod) {
                            "Week" -> "This week"
                            "Month" -> "This month"
                            "Year" -> "This year"
                            else -> "All time"
                        },
                        total = when (uiState.timePeriod) {
                            "Week" -> uiState.stats.weeklyTotal.toString()
                            "Month" -> uiState.stats.monthlyTotal.toString()
                            "Year" -> uiState.stats.yearlyTotal.toString()
                            else -> uiState.stats.total.toString()
                        },
                        average = when (uiState.timePeriod) {
                            "Week" -> uiState.stats.weeklyAverage.toString()
                            "Month" -> uiState.stats.monthlyAverage.toString()
                            "Year" -> uiState.stats.yearlyAverage.toString()
                            else -> uiState.stats.totalAverage.toString()
                        },

                    )
                }
            }
            // TODO check if the item is still there if highest tip is null because I don't want the spacedBy to be there always
            item {
                uiState.stats.highestTip?.let { tip ->
                    TextSubHeadingMedium("Highest tip", color = MaterialTheme.colorScheme.primary)
                    TextDefault(
                        text = "Tip amount: ${tip.amount}",
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )
                    TextDefault(
                        text = "Date: ${tip.date.toFullDateString()}",
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                uiState.stats.bestMonth?.let { bestMonth ->
                    TextSubHeadingMedium("Best month", color = MaterialTheme.colorScheme.primary)
                    TextDefault(
                        text = "Tip amount: ${bestMonth.second}",
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )
                    TextDefault(
                        text = "Month: ${bestMonth.first}",
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (uiState.showAlertDialog) {
        ErrorAlertDialog(uiState.error)
        tipTrackerViewModel.dismissAlert()
    }
}
