package com.example.lifetogether.ui.feature.tipTracker

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium

@Composable
fun TipsCalendar(
    groupedTips: Map<String, List<TipItem>>,
) {
    val viewModel: TipsCalendarViewModel = hiltViewModel()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(25.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextDefault(
            text = "< Previous",
            modifier = Modifier.clickable { viewModel.selectPreviousMonth() }.weight(1f),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.End,
        )
        TextDefault(
            text = "Current month",
            modifier = Modifier.clickable { viewModel.selectCurrentMonth() }.weight(1f),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        TextDefault(
            text = "Next >",
            modifier = Modifier.clickable { viewModel.selectNextMonth() }.weight(1f),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Start,
        )
    }

    TextSubHeadingMedium(
        viewModel.getCurrentMonthYearDisplay(),
        Modifier.fillMaxWidth(),
        MaterialTheme.colorScheme.primary,
        alignCenter = true
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val monthlyStats = viewModel.getMonthlySummary(groupedTips)
        TextDefault(
            text = "Total: ${monthlyStats.total}",
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(15.dp))
        TextDefault(
            text = "Average: ${monthlyStats.average}",
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxWidth()
            .height(viewModel.getGridHeight())
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = viewModel.days, key = { it }) { day ->
            val date = viewModel.getDate(day)
            val dayOfWeek = viewModel.getDayOfWeekLabel(date)
            val tipTotal = viewModel.getTipTotal(viewModel.getDateKey(date), groupedTips)

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color(0xFF007A7A), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.aspectRatio(1f)) {
                    Text(
                        text = "$day $dayOfWeek",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 2.dp, start = 4.dp),
                        fontSize = 9.sp,
                    )

                    if (tipTotal > 0f) {
                        Text(
                            text = viewModel.formatTipTotal(tipTotal),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}
