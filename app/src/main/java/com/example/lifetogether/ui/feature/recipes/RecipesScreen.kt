package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AuthViewModel

@Composable
fun RecipesScreen(
    appNavigator: AppNavigator? = null,
    authViewModel: AuthViewModel? = null,
) {
    val recipesViewModel: RecipesViewModel = hiltViewModel()

    val userInformationState by authViewModel?.userInformation!!.collectAsState()

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
                        appNavigator?.navigateBack()
                    },
                    text = "Recipes",
                )
            }

            item {
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            for (tag in listOf("All", "Simple", "Dinner", "Breakfast", "Dessert")) {
                                TagOption(
                                    tag = tag,
                                    selectedTag = "All",
                                    onClick = { }, // TODO
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
            }

            item {
                // TODO add the recipe list
                RecipeOverview(
                    recipe = EXAMPLE_RECIPE,
                    onClick = {
                        appNavigator?.navigateToRecipeDetails(EXAMPLE_RECIPE.id)
                    },
                )
            }
        }
    }

    AddButton(onClick = {
        appNavigator?.navigateToRecipeDetails()
    })
}

@Preview(showBackground = true)
@Composable
fun RecipesScreenPreview() {
    LifeTogetherTheme {
        RecipesScreen()
    }
}
