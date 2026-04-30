package com.example.lifetogether.ui.feature.tipTracker.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.feature.tipTracker.components.StatsCard
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerNavigationEvent
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerStats
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerUiEvent
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerUiState
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@Composable
fun TipStatisticsScreen(
    uiState: TipTrackerUiState,
    onUiEvent: (TipTrackerUiEvent) -> Unit,
    onNavigationEvent: (TipTrackerNavigationEvent) -> Unit,
) {
    val content = uiState as TipTrackerUiState.Content

    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(TipTrackerNavigationEvent.NavigateBack)
                },
                text = "Tip Statistics",
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LifeTogetherTokens.spacing.small)
                .padding(bottom = LifeTogetherTokens.spacing.bottomInsetLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
        ) {
            item {
                if (content.tips.isNotEmpty()) {
                    TagOptionRow(
                        options = listOf("Week", "Month", "Year", "All"),
                        selectedOption = content.timePeriod,
                        onSelectedOptionChange = {
                            onUiEvent(TipTrackerUiEvent.TimePeriodSelected(it))
                        },
                        center = true,
                    )
                    StatsCard(
                        title = when (content.timePeriod) {
                            "Week" -> "This week"
                            "Month" -> "This month"
                            "Year" -> "This year"
                            else -> "All time"
                        },
                        total = when (content.timePeriod) {
                            "Week" -> content.stats.weeklyTotal.toString()
                            "Month" -> content.stats.monthlyTotal.toString()
                            "Year" -> content.stats.yearlyTotal.toString()
                            else -> content.stats.total.toString()
                        },
                        average = when (content.timePeriod) {
                            "Week" -> content.stats.weeklyAverage.toString()
                            "Month" -> content.stats.monthlyAverage.toString()
                            "Year" -> content.stats.yearlyAverage.toString()
                            else -> content.stats.totalAverage.toString()
                        },
                    )
                }
            }
            // TODO check if the item is still there if highest tip is null because I don't want the spacedBy to be there always
            item {
                content.stats.highestTip?.let { tip ->
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
                content.stats.bestMonth?.let { bestMonth ->
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
}

@Preview(showBackground = true)
@Composable
private fun TipStatisticsScreenPreview() {
    LifeTogetherTheme {
        TipStatisticsScreen(
            uiState = TipTrackerUiState.Content(
                tips = listOf(TipItem(amount = 120f, date = Date())),
                stats = TipTrackerStats(
                    weeklyTotal = 120f,
                    monthlyTotal = 120f,
                    yearlyTotal = 120f,
                    total = 120f,
                    weeklyAverage = 120f,
                    monthlyAverage = 120f,
                    yearlyAverage = 120f,
                    totalAverage = 120f,
                    highestTip = TipItem(amount = 120f, date = Date()),
                    bestMonth = "January 2026" to 120f,
                ),
                groupedTips = mapOf("01. January 2026" to listOf(TipItem(amount = 120f, date = Date()))),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
