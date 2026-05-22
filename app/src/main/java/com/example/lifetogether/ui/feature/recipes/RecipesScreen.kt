package com.example.lifetogether.ui.feature.recipes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.feature.recipes.components.RecipeCardV2
import com.example.lifetogether.ui.feature.recipes.components.RecipeSearchField
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun RecipesScreen(
    uiState: RecipesUiState,
    onUiEvent: (RecipesUiEvent) -> Unit,
    onNavigationEvent: (RecipesNavigationEvent) -> Unit,
) {
    val contentState = uiState as? RecipesUiState.Content
    val isLoading = uiState is RecipesUiState.Loading
    val searchFocusRequester = remember { FocusRequester() }
    val isSearchActive = contentState?.isSearchActive == true
    val searchQuery = contentState?.searchQuery.orEmpty()

    BackHandler(enabled = isSearchActive) {
        if (searchQuery.isNotBlank()) {
            onUiEvent(RecipesUiEvent.SearchClearClicked)
        } else {
            onUiEvent(RecipesUiEvent.SearchCancelClicked)
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(RecipesNavigationEvent.NavigateBack)
                },
                text = "Recipes",
                titleContent = if (isSearchActive) {
                    {
                        RecipeSearchField(
                            value = searchQuery,
                            onValueChange = { onUiEvent(RecipesUiEvent.SearchQueryChanged(it)) },
                            focusRequester = searchFocusRequester,
                        )
                    }
                } else {
                    null
                },
                rightAppIcon = contentState?.let {
                    AppIcon(
                        resId = if (isSearchActive) R.drawable.ic_close else R.drawable.ic_search,
                        description = if (isSearchActive) "close search icon" else "search icon",
                    )
                },
                onRightClick = contentState?.let {
                    {
                        when {
                            !isSearchActive -> onUiEvent(RecipesUiEvent.SearchIconClicked)
                            searchQuery.isNotBlank() -> onUiEvent(RecipesUiEvent.SearchClearClicked)
                            else -> onUiEvent(RecipesUiEvent.SearchCancelClicked)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isLoading) {
                AddButton(
                    onClick = {
                        onNavigationEvent(RecipesNavigationEvent.NavigateToCreateRecipe)
                    }
                )
            }
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = isLoading,
            label = "recipes_loading_content",
            loadingContent = {
                Skeletons.ListDetail(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = contentState ?: return@AnimatedLoadingContent
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(LifeTogetherTokens.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.large),
            ) {
                item {
                    TagOptionRow(
                        options = content.tagsList,
                        selectedOption = content.selectedTag,
                        onSelectedOptionChange = {
                            onUiEvent(RecipesUiEvent.TagSelected(it))
                        },
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                    ) {
                        for (recipe in content.recipes) {
                            RecipeCardV2(
                                recipe = recipe,
                                onClick = {
                                    onNavigationEvent(
                                        RecipesNavigationEvent.NavigateToRecipeDetails(recipe.id)
                                    )
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.bottomInsetMedium))
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
            uiState = RecipesUiState.Content(
                recipes = listOf(
                    Recipe(
                        id = "1",
                        familyId = "family-1",
                        itemName = "Tomato Soup",
                        description = "A simple soup.",
                        ingredients = emptyList(),
                        instructions = emptyList(),
                        preparationTimeMin = 25,
                        favourite = false,
                        servings = 2,
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
@Preview(showBackground = true)
@Composable
private fun RecipesScreenLoadingPreview() {
    LifeTogetherTheme {
        RecipesScreen(
            uiState = RecipesUiState.Loading,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true, name = "Recipes screen searching", heightDp = 1200)
@Composable
private fun RecipesScreenSearchingPreview() {
    LifeTogetherTheme {
        RecipesScreen(
            uiState = RecipesUiState.Content(
                recipes = listOf(
                    Recipe(
                        id = "1",
                        familyId = "family-1",
                        itemName = "Tomato Soup",
                        description = "A simple soup.",
                        ingredients = emptyList(),
                        instructions = emptyList(),
                        preparationTimeMin = 25,
                        favourite = false,
                        servings = 2,
                        tags = listOf("Dinner", "Soup"),
                    ),
                ),
                tagsList = listOf("All", "Dinner", "Soup"),
                selectedTag = "All",
                searchQuery = "soup",
                isSearchActive = true,
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
