package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.sync.SyncKey

@Composable
fun RecipesScreen(
    uiState: RecipesUiState,
    onUiEvent: (RecipesUiEvent) -> Unit,
    onNavigationEvent: (RecipesNavigationEvent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp),
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
                        onNavigationEvent(RecipesNavigationEvent.NavigateBack)
                    },
                    text = "Recipes",
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SyncUpdatingText(
                        keys = setOf(SyncKey.RECIPES),
                    )

                    TagOptionRow(
                        options = uiState.tagsList,
                        selectedOption = uiState.selectedTag,
                        onSelectedOptionChange = {
                            onUiEvent(RecipesUiEvent.TagSelected(it))
                        },
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (recipe in uiState.recipes) {
                        RecipeOverview(
                            recipe = recipe,
                            onClick = {
                                onNavigationEvent(RecipesNavigationEvent.NavigateToRecipeDetails(recipe.id))
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 30.dp, end = 30.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        AddButton(onClick = {
            onNavigationEvent(RecipesNavigationEvent.NavigateToRecipeDetails())
        })
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipesScreenPreview() {
    LifeTogetherTheme {
        RecipesScreen(
            uiState = RecipesUiState(
                recipes = listOf(
                    Recipe(
                        id = "1",
                        itemName = "Tomato Soup",
                        preparationTimeMin = 25,
                        tags = listOf("Dinner", "Soup"),
                    ),
                ),
                tagsList = listOf("All", "Dinner", "Soup"),
                selectedTag = "All",
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
