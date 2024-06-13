package com.example.lifetogether.ui.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.CountdownRow
import com.example.lifetogether.ui.common.FeatureOverview
import com.example.lifetogether.ui.common.text.TextDisplayLarge
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    appNavigator: AppNavigator? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_picture),
                        contentDescription = "contentDescription", // TODO
                    )

                    TextDisplayLarge(text = "A Life Together")

                    Image(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "contentDescription", // TODO
                    )
                }

                Text(
                    text = "x days together", // TODO
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(shape = RoundedCornerShape(30.dp))
                        .background(color = Color.Gray),
                ) {
                    // TODO add image
                }
            }

            item {
                Text(text = "Important dates:")
                // TODO update events and make clickable
                CountdownRow(event = "Wedding anniversary", daysLeft = "20")
                CountdownRow(event = "Andrés' birthday", daysLeft = "35")
            }

            item {
                FlowRow(
                    maxItemsInEachRow = 2,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) { // TODO add real items
                    FeatureOverview(
                        "Grocery list",
                        10,
                        "Recipe",
                    )
                    FeatureOverview(
                        "Recipes",
                        1,
                        "Recipe",
                    )
                    FeatureOverview(
                        "Memory lane",
                        4,
                        "Recipe",
                        true,
                    )
                    FeatureOverview(
                        "Gallery",
                        10,
                        "Recipe",
                    )
                    FeatureOverview(
                        "Note Corner",
                        43,
                        "Recipe",
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LifeTogetherTheme {
        HomeScreen()
    }
}
