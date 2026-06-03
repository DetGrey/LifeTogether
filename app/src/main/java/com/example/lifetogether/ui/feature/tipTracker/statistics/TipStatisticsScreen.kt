package com.example.lifetogether.ui.feature.tipTracker.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingLarge
import com.example.lifetogether.ui.feature.tipTracker.TipPeriodComparison
import com.example.lifetogether.ui.feature.tipTracker.TipStatisticsPeriod
import com.example.lifetogether.ui.feature.tipTracker.TipTotalSummary
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerCalendarState
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerNavigationEvent
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerStats
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerUiEvent
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerUiState
import com.example.lifetogether.ui.feature.tipTracker.formatTipTotal
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@Composable
fun TipStatisticsScreen(
    uiState: TipTrackerUiState,
    onUiEvent: (TipTrackerUiEvent) -> Unit,
    onNavigationEvent: (TipTrackerNavigationEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(TipTrackerNavigationEvent.NavigateBack)
                },
                text = "Tip Statistics",
            )
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is TipTrackerUiState.Loading,
            label = "tip_statistics_loading",
            loadingContent = {
                Skeletons.GridCollection(
                    modifier = Modifier.fillMaxSize(),
                )
            },
        ) {
            val content = uiState as? TipTrackerUiState.Content ?: return@AnimatedLoadingContent

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = LifeTogetherTokens.spacing.medium),
                contentPadding = PaddingValues(
                    top = LifeTogetherTokens.spacing.medium,
                    bottom = LifeTogetherTokens.spacing.bottomInsetLarge,
                ),
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
            ) {
                if (content.tips.isEmpty()) {
                    item {
                        Text(
                            text = "No tips yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    item {
                        TagOptionRow(
                            options = TipStatisticsPeriod.entries.map { it.displayName },
                            selectedOption = content.timePeriod.displayName,
                            onSelectedOptionChange = {
                                val selectedPeriod = TipStatisticsPeriod.entries.firstOrNull { period ->
                                    period.displayName == it
                                } ?: return@TagOptionRow
                                onUiEvent(TipTrackerUiEvent.TimePeriodSelected(selectedPeriod))
                            },
                        )
                    }

                    item {
                        PeriodSummarySection(
                            title = content.timePeriod.summaryTitle(),
                            total = content.stats.totalFor(content.timePeriod),
                            average = content.stats.averageFor(content.timePeriod),
                            comparison = content.stats.periodComparisons[content.timePeriod],
                            period = content.timePeriod,
                        )
                    }

                    item {
                        TipTotalsSection(
                            title = "Top months",
                            items = content.stats.topMonths,
                            valueSuffix = " kr.",
                        )
                    }

                    item {
                        TipTotalsSection(
                            title = "Top days",
                            items = content.stats.topDays,
                            valueSuffix = " kr.",
                        )
                    }

                    item {
                        TipTotalsSection(
                            title = "Best weekdays",
                            items = content.stats.bestWeekdays,
                            valueSuffix = " kr. avg",
                        )
                    }

                    item {
                        MonthlyTrendSection(
                            items = content.stats.monthlyTrend,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSummarySection(
    title: String,
    total: Float,
    average: Float,
    comparison: TipPeriodComparison?,
    period: TipStatisticsPeriod,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
    ) {
        TextHeadingLarge(
            text = title,
            textAlign = TextAlign.Start
        )
        Text(
            text = formatTipTotal(total),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        comparison?.let {
            Row {
                TextDefault(
                    text = "${it.differenceText()} vs previous ${period.comparisonLabel()}",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Row {
            TextDefault(
                text = "Average tip: ",
                color = MaterialTheme.colorScheme.onBackground,
            )
            TextDefault(
                text = formatTipTotal(average),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun TipTotalsSection(
    title: String,
    items: List<TipTotalSummary>,
    valueSuffix: String = "",
) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        SectionTitle(title)

        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.xSmall))

        items.forEachIndexed { index, item ->
            MedalStatisticRow(
                rank = index + 1,
                label = item.label,
                value = "${formatTipTotal(item.total)}$valueSuffix",
            )
        }
    }
}

@Composable
private fun MonthlyTrendSection(items: List<TipTotalSummary>) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        SectionTitle("Monthly trend")
        items.forEach { item ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
            ) {
                StatisticRow(
                    label = item.label,
                    value = "${formatTipTotal(item.total)} kr.",
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { calculateTrendProgress(item.total, items) },
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        TextHeadingLarge(
            text = title,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun MedalStatisticRow(
    rank: Int,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = LifeTogetherTokens.spacing.medium)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankMedal(rank)
        Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.small))
        TextDefault(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.medium))
        TextDefault(
            text = value,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun RankMedal(rank: Int) {
    Box(
        modifier = Modifier
            .size(LifeTogetherTokens.sizing.iconLarge)
            .clip(CircleShape)
            .background(
                color = when (rank) {
                    1 -> MaterialTheme.colorScheme.secondaryContainer
                    2 -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.ordinalLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = when (rank) {
                1 -> MaterialTheme.colorScheme.onSecondaryContainer
                2 -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onTertiaryContainer
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun StatisticRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

private fun Int.ordinalLabel(): String {
    return when (this) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "${this}th"
    }
}

private fun TipTrackerStats.totalFor(period: TipStatisticsPeriod): Float {
    return when (period) {
        TipStatisticsPeriod.WEEK -> weeklyTotal
        TipStatisticsPeriod.MONTH -> monthlyTotal
        TipStatisticsPeriod.YEAR -> yearlyTotal
        TipStatisticsPeriod.ALL -> total
    }
}

private fun TipTrackerStats.averageFor(period: TipStatisticsPeriod): Float {
    return when (period) {
        TipStatisticsPeriod.WEEK -> weeklyAverage
        TipStatisticsPeriod.MONTH -> monthlyAverage
        TipStatisticsPeriod.YEAR -> yearlyAverage
        TipStatisticsPeriod.ALL -> totalAverage
    }
}

private fun TipStatisticsPeriod.summaryTitle(): String {
    return when (this) {
        TipStatisticsPeriod.WEEK -> "This week"
        TipStatisticsPeriod.MONTH -> "This month"
        TipStatisticsPeriod.YEAR -> "This year"
        TipStatisticsPeriod.ALL -> "All time"
    }
}

private fun TipStatisticsPeriod.comparisonLabel(): String {
    return when (this) {
        TipStatisticsPeriod.WEEK -> "week"
        TipStatisticsPeriod.MONTH -> "month"
        TipStatisticsPeriod.YEAR -> "year"
        TipStatisticsPeriod.ALL -> ""
    }
}

private fun TipPeriodComparison.differenceText(): String {
    val sign = if (difference > 0f) "+" else ""
    val percentText = differencePercent?.let { percent ->
        val percentSign = if (percent > 0f) "+" else ""
        " ($percentSign${formatTipTotal(percent)}%)"
    } ?: ""

    return "$sign${formatTipTotal(difference)}$percentText"
}

private fun calculateTrendProgress(
    total: Float,
    items: List<TipTotalSummary>,
): Float {
    val maxTotal = items.maxOfOrNull { it.total } ?: return 0f
    if (maxTotal <= 0f) return 0f

    return total / maxTotal
}

@Preview(showBackground = true)
@Composable
private fun TipStatisticsScreenPreview() {
    LifeTogetherTheme {
        TipStatisticsScreen(
            uiState = TipTrackerUiState.Content(
                tips = listOf(
                    TipItem(
                        id = "tip-1",
                        familyId = "family-1",
                        itemName = "Tip",
                        amount = 120f,
                        date = Date(),
                    ),
                ),
                stats = TipTrackerStats(
                    weeklyTotal = 120f,
                    monthlyTotal = 120f,
                    yearlyTotal = 120f,
                    total = 120f,
                    weeklyAverage = 120f,
                    monthlyAverage = 120f,
                    yearlyAverage = 120f,
                    totalAverage = 120f,
                    topMonths = listOf(
                        TipTotalSummary("January 2026", 120f),
                        TipTotalSummary("February 2026", 90f),
                        TipTotalSummary("March 2026", 70f),
                    ),
                    topDays = listOf(
                        TipTotalSummary("1. January 2026", 120f),
                        TipTotalSummary("2. January 2026", 90f),
                        TipTotalSummary("3. January 2026", 70f),
                    ),
                    bestWeekdays = listOf(
                        TipTotalSummary("Friday", 120f),
                        TipTotalSummary("Saturday", 90f),
                        TipTotalSummary("Thursday", 70f),
                    ),
                    monthlyTrend = listOf(
                        TipTotalSummary("October 2025", 40f),
                        TipTotalSummary("November 2025", 55f),
                        TipTotalSummary("December 2025", 75f),
                        TipTotalSummary("January 2026", 120f),
                        TipTotalSummary("February 2026", 90f),
                        TipTotalSummary("March 2026", 70f),
                    ),
                    periodComparisons = mapOf(
                        TipStatisticsPeriod.WEEK to TipPeriodComparison(
                            currentTotal = 120f,
                            previousTotal = 90f,
                            difference = 30f,
                            differencePercent = 33.33f,
                        ),
                    ),
                ),
                calendar = TipTrackerCalendarState(),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
