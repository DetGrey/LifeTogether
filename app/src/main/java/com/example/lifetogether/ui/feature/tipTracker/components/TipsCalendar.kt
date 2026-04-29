package com.example.lifetogether.ui.feature.tipTracker.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerCalendarDay
import com.example.lifetogether.ui.feature.tipTracker.TipTrackerCalendarState
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun TipsCalendar(
    calendar: TipTrackerCalendarState,
    onPreviousMonthClick: () -> Unit,
    onCurrentMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LifeTogetherTokens.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextDefault(
            text = "< Previous",
            modifier = Modifier
                .weight(1f)
                .clickable { onPreviousMonthClick() },
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
        )
        TextDefault(
            text = "Current month",
            modifier = Modifier
                .weight(1f)
                .clickable { onCurrentMonthClick() },
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        TextDefault(
            text = "Next >",
            modifier = Modifier
                .weight(1f)
                .clickable { onNextMonthClick() },
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
        )
    }

    TextSubHeadingMedium(
        calendar.monthLabel,
        Modifier.fillMaxWidth(),
        MaterialTheme.colorScheme.primary,
        alignCenter = true,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LifeTogetherTokens.spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        TextDefault(
            text = "Total: ${calendar.summary.totalText}",
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(15.dp))
        TextDefault(
            text = "Average: ${calendar.summary.averageText}",
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxWidth()
            .height(calendar.gridHeight)
            .padding(LifeTogetherTokens.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
    ) {
        items(items = calendar.days, key = { it.label }) { day ->
            DayCell(day = day)
        }
    }
}

@Composable
private fun DayCell(day: TipTrackerCalendarDay) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small) //todo do we need both?
            .border(2.dp, MaterialTheme.colorScheme.onBackground, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.aspectRatio(1f)) {
            Text(
                text = day.label,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = LifeTogetherTokens.spacing.xSmall, start = LifeTogetherTokens.spacing.xSmall),
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            day.totalText?.let { totalText ->
                Text(
                    text = totalText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = LifeTogetherTokens.spacing.xSmall)
                        .align(Alignment.Center),
                )
            }
        }
    }
}
