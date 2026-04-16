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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.observer.ObserverUpdatingText
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel
import com.example.lifetogether.domain.observer.ObserverKey

@Composable
fun RecipesScreen(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
) {
    val recipesViewModel: RecipesViewModel = hiltViewModel()
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    val recipes by recipesViewModel.filteredRecipes.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        println("Recipes familyId: ${userInformation?.familyId}")
        userInformation?.familyId?.let { recipesViewModel.setUpRecipes(it) }
    }

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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ObserverUpdatingText(
                        keys = setOf(ObserverKey.RECIPES),
                    )

                    TagOptionRow(
                        options = recipesViewModel.tagsList,
                        selectedOption = recipesViewModel.selectedTag,
                        onSelectedOptionChange = {
                            recipesViewModel.selectedTag = it
                        },
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (recipe in recipes) {
                        RecipeOverview(
                            recipe = recipe,
                            onClick = {
                                appNavigator?.navigateToRecipeDetails(recipe.id)
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
            appNavigator?.navigateToRecipeDetails()
        })
    }
    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (recipesViewModel.showAlertDialog) {
        LaunchedEffect(recipesViewModel.error) {
            recipesViewModel.toggleAlertDialog()
        }
        ErrorAlertDialog(recipesViewModel.error)
    }
}
