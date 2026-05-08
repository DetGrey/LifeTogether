package com.example.lifetogether.ui.feature.lists.listDetails.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MealPlannerListSection(
    entries: List<MealPlanEntry>,
    onEntryClick: (MealPlanEntry) -> Unit,
) {
    MealPlannerWeekPager(
        entries = entries,
        onEntryClick = onEntryClick,
    )
}

@Composable
private fun MealPlannerWeekPager(
    entries: List<MealPlanEntry>,
    onEntryClick: (MealPlanEntry) -> Unit,
) {
    val parsedEntries = remember(entries) {
        entries.mapNotNull { entry ->
            parseMealDate(entry.date)?.let { it to entry }
        }
    }
    val entryByDate = remember(parsedEntries) { parsedEntries.toMap() }
    val today = remember { LocalDate.now() }
    val currentWeekStart = remember(today) { startOfWeek(today) }
    val weekOffsets = remember(parsedEntries, currentWeekStart) {
        parsedEntries.map { pair -> java.time.temporal.ChronoUnit.WEEKS.between(currentWeekStart, startOfWeek(pair.first)).toInt() }
    }
    val minOffset = remember(weekOffsets) { minOf(weekOffsets.minOrNull() ?: 0, -8) }
    val maxOffset = remember(weekOffsets) { maxOf(weekOffsets.maxOrNull() ?: 0, 8) }
    val pageCount = (maxOffset - minOffset + 1).coerceAtLeast(1)
    val currentWeekPage = -minOffset
    val pagerState = rememberPagerState(initialPage = currentWeekPage) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextDefault(text = "Week view")
            SecondaryButton(
                text = "Current week",
                onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(currentWeekPage) }
                },
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val weekStart = currentWeekStart.plusWeeks((page - currentWeekPage).toLong())
            val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
            ) { //todo can have multiple meals per day and make sure to order correctly as per when you eat which meal
                items(weekDays) { day ->
                    val dayEntry = entryByDate[day]
                    val label = "${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${day.dayOfMonth}"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.large,
                            )
                            .padding(LifeTogetherTokens.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)
                    ) {
                        if (dayEntry == null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextDefault(text = label)
                                TextLabel(text = "No meal planned")
                            }
                        } else {
                            TextDefault(text = label)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.large,
                                    )
                                    .clickable(
                                        onClick = { onEntryClick(dayEntry) },
                                    )
                                    .padding(LifeTogetherTokens.spacing.medium),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        TextHeadingMedium(
                                            text = dayEntry.itemName,
                                            maxLines = 1,
                                        )
                                        TextLabel(
                                            text = dayEntry.mealType.displayName,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                    TextDefault(
                                        text = "TODO prep time",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseMealDate(value: String): LocalDate? {
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun startOfWeek(date: LocalDate): LocalDate {
    var d = date
    while (d.dayOfWeek != DayOfWeek.MONDAY) {
        d = d.minusDays(1)
    }
    return d
}