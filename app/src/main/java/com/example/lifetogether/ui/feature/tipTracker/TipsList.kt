package com.example.lifetogether.ui.feature.tipTracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.ui.common.text.TextHeadingLarge

@Composable
fun TipsList(
    groupedTips: Map<String, List<TipItem>>,
    onDeleteClick: (TipItem) -> Unit,
) {
    groupedTips.forEach { (dateString, tipsForDate) ->
        Spacer(modifier = Modifier.height(30.dp))
        TextHeadingLarge(dateString) // Show the date heading
        Spacer(modifier = Modifier.height(10.dp))

        tipsForDate.forEach { tipItem ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(20))
                    .background(Color.White)
                    .padding(horizontal = 15.dp, vertical = 5.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${tipItem.amount} ${tipItem.currency}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .aspectRatio(1f)
                                .clickable { onDeleteClick(tipItem) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_trashcan_black),
                                contentDescription = "Delete icon",
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
