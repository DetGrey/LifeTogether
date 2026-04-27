package com.example.lifetogether.ui.feature.tipTracker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun TipsList(
    groupedTips: Map<String, List<TipItem>>,
    onDeleteClick: (TipItem) -> Unit,
) {
    groupedTips.forEach { (dateString, tipsForDate) ->
        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.large))

        TextSubHeadingMedium(dateString, color = MaterialTheme.colorScheme.primary) // Show the date heading

        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = LifeTogetherTokens.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tipsForDate) { tipItem ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.large
                        )
                        .padding(horizontal = LifeTogetherTokens.spacing.small, vertical = LifeTogetherTokens.spacing.xSmall),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${tipItem.amount} ${tipItem.currency}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Row(
                            modifier = Modifier.wrapContentWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(25.dp)
                                    .aspectRatio(1f)
                                    .clickable { onDeleteClick(tipItem) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_trashcan),
                                    contentDescription = "Delete icon",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))
    }
}
