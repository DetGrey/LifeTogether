package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun RecipesScreen(
    uiState: RecipesUiState,
    onUiEvent: (RecipesUiEvent) -> Unit,
    onNavigationEvent: (RecipesNavigationEvent) -> Unit,
) {
    Scaffold(
        topBar = {
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
        },
        floatingActionButton = {
            AddButton(onClick = {
                onNavigationEvent(RecipesNavigationEvent.NavigateToCreateRecipe)
            })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LifeTogetherTokens.spacing.small)
                .padding(bottom = LifeTogetherTokens.spacing.bottomInsetMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)) {
                    SyncUpdatingText(keys = setOf(SyncKey.RECIPES))

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
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    for (recipe in uiState.recipes) {
                        RecipeCard(
                            recipe = recipe,
                            onClick = {
                                recipe.id?.let { recipeId ->
                                    onNavigationEvent(
                                        RecipesNavigationEvent.NavigateToRecipeDetails(recipeId)
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
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
