package com.example.lifetogether.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.LoveButton
import com.example.lifetogether.ui.common.text.TextDisplayLarge
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigationEvent: (HomeNavigationEvent) -> Unit,
) {
    val content = when (uiState) {
        HomeUiState.Loading -> null
        is HomeUiState.Unauthenticated -> uiState.content
        is HomeUiState.Authenticated -> uiState.content
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp)
                .padding(bottom = 60.dp)
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(
                        resId = R.drawable.ic_profile_picture,
                        description = "profile picture icon",
                    ),
                    onLeftClick = {
                        onNavigationEvent(HomeNavigationEvent.ProfileClicked)
                    },
                    text = "Life Together",
                    rightIcon = Icon(
                        resId = R.drawable.ic_settings,
                        description = "settings icon",
                    ),
                    onRightClick = {
                        onNavigationEvent(HomeNavigationEvent.SettingsClicked)
                    },
                    subText = "x days together",
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(shape = RoundedCornerShape(20))
                        .background(color = MaterialTheme.colorScheme.onBackground),
                ) {
                    val bitmap = content?.bitmap
                    if (bitmap != null) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "family image",
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            item {
                when (val statusCard = content?.statusCard) {
                    is HomeStatusCard.Message -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(75.dp)
                                .clip(shape = RoundedCornerShape(20))
                                .background(MaterialTheme.colorScheme.tertiary)
                                .clickable {
                                    onNavigationEvent(HomeNavigationEvent.StatusCardClicked)
                                }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = statusCard.text)
                        }
                    }

                    HomeStatusCard.None, null -> Unit
                }
            }

            content?.sections?.forEach { section ->
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (section.title != null) {
                            TextDisplayLarge(section.title)
                        }

                        FlowRow(
                            maxItemsInEachRow = section.maxItemsInEachRow,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            section.items.forEach { item ->
                                when (item) {
                                    is HomeSectionItem.Tile -> FeatureOverview(
                                        title = item.tile.title,
                                        onClick = {
                                            onNavigationEvent(HomeNavigationEvent.TileClicked(item.tile))
                                        },
                                        icon = item.tile.icon,
                                    )

                                    HomeSectionItem.Break -> Spacer(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(0.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LoveButton()
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LifeTogetherTheme {
        HomeScreen(
            uiState = HomeUiState.Authenticated(
                userInformation = UserInformation(
                    uid = "1",
                    name = "Alex",
                    familyId = "family-123",
                ),
                content = HomeContent(
                    statusCard = HomeStatusCard.None,
                    sections = listOf(
                        HomeSection(
                            maxItemsInEachRow = 3,
                            items = listOf(
                                HomeSectionItem.Tile(HomeTile.GroceryList),
                                HomeSectionItem.Tile(HomeTile.Recipes),
                                HomeSectionItem.Break,
                                HomeSectionItem.Tile(HomeTile.Guides),
                                HomeSectionItem.Tile(HomeTile.Gallery),
                                HomeSectionItem.Tile(HomeTile.TipTracker),
                                HomeSectionItem.Tile(HomeTile.Lists),
                            ),
                        ),
                        HomeSection(
                            title = "Admin features",
                            maxItemsInEachRow = 2,
                            items = listOf(
                                HomeSectionItem.Tile(HomeTile.AdminGroceryCategories),
                                HomeSectionItem.Tile(HomeTile.AdminGrocerySuggestions),
                            ),
                        ),
                    ),
                ),
            ),
            onNavigationEvent = {},
        )
    }
}
