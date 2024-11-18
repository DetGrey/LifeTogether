package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.toggleCompleted
import com.example.lifetogether.ui.common.AddNewString
import com.example.lifetogether.ui.common.CompletableCategoryList
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.text.EditableTextField
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import java.util.Date

@Composable
fun RecipeDetailsScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    recipeId: String? = null,
) {
    val recipeDetailsViewModel: RecipeDetailsViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val recipe by recipeDetailsViewModel.recipe.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        println("GroceryList familyId: ${userInformation?.familyId}")
        userInformation?.familyId?.let { recipeDetailsViewModel.setUpRecipeDetails(it, recipeId) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp, top = 10.dp)
                            .height(40.dp)
                            .aspectRatio(1f)
                            .clickable {
                                appNavigator?.navigateBack()
                            }
                            .align(Alignment.TopStart),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = "back arrow icon",
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp, top = 10.dp)
                            .height(40.dp)
                            .aspectRatio(1f)
                            .clickable(
                                enabled = if (recipeDetailsViewModel.editMode) true else if (recipeId != null) true else false,
                            ) {
                                if (recipeDetailsViewModel.editMode) {
                                    recipeDetailsViewModel.saveRecipe()
                                } else if (recipeId != null) {
                                    // TODO Delete recipe
                                }
                            }
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (recipeDetailsViewModel.editMode) {
                            Text("Save") // TODO make an icon maybe???
                        } else if (recipeId != null) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_trashcan),
                                contentDescription = "trashcan icon",
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp, end = 40.dp)
                            .align(Alignment.BottomStart),
                    ) {
                        EditableTextField(
                            text = recipe.itemName,
                            onTextChange = { recipe.itemName = it },
                            label = "Recipe name",
                            isEditable = recipeDetailsViewModel.editMode,
                            textStyle = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (!recipeDetailsViewModel.editMode) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 5.dp)
                                .height(40.dp)
                                .aspectRatio(1f)
                                .clickable {
                                    recipeDetailsViewModel.editMode = true
                                }
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_edit_black),
                                contentDescription = "edit icon",
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                EditableTextField(
                    text = recipe.description,
                    onTextChange = { recipe.description = it },
                    label = "Description",
                    isEditable = recipeDetailsViewModel.editMode,
                    textStyle = MaterialTheme.typography.bodyLarge,
                )

                Column {
                    recipeDetailsViewModel.expandedStates["ingredients"]?.let { expanded ->
                        CompletableCategoryList(
                            category = Category(
                                "🍎",
                                "Ingredients",
                            ),
                            itemList = recipe.ingredients,
                            expanded = expanded,
                            onClick = {
                                println("expanded before $expanded")
                                recipeDetailsViewModel.toggleExpandedStates("ingredients")
                                println("expanded after $expanded")
                            },
                            onCompleteToggle = {
                                recipe.ingredients = recipe.ingredients.toggleCompleted(it.itemName)
                            },
                        )
                    }

                    if (recipeDetailsViewModel.editMode) {
                        AddNewIngredient(
                            onAddClick = {
                                recipeDetailsViewModel.recipeAddNewItemToList(it)
                            },
                        )
                    }
                }
                Column {
                    recipeDetailsViewModel.expandedStates["instructions"]?.let { expanded ->
                        CompletableCategoryList(
                            category = Category(
                                "✔️",
                                "Instructions",
                            ),
                            itemList = recipe.instructions,
                            expanded = expanded,
                            onClick = {
                                recipeDetailsViewModel.toggleExpandedStates("instructions")
                            },
                            onCompleteToggle = {
                                recipe.instructions =
                                    recipe.instructions.toggleCompleted(it.itemName)
                            },
                        )

                        if (recipeDetailsViewModel.editMode) {
                            AddNewString(
                                label = "Add new instruction",
                                onAddClick = {
                                    recipeDetailsViewModel.recipeAddNewItemToList(Instruction(itemName = it))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(30.dp))

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (recipeDetailsViewModel.showAlertDialog) {
        ErrorAlertDialog(recipeDetailsViewModel.error)
        recipeDetailsViewModel.toggleAlertDialog()
    }
}

@Preview(showBackground = true)
@Composable
fun RecipeDetailsScreenPreview() {
    LifeTogetherTheme {
        RecipeDetailsScreen(recipeId = EXAMPLE_RECIPE.id)
    }
}

val EXAMPLE_RECIPE = Recipe(
    id = "1",
    familyId = "family123",
    itemName = "Spaghetti Bolognese",
    lastUpdated = Date(),
    description = "A classic Italian pasta dish with a rich, savory sauce.",
    ingredients = listOf(
        Ingredient(amount = 200.0, measureType = MeasureType.GRAM, itemName = "spaghetti"),
        Ingredient(amount = 100.0, measureType = MeasureType.GRAM, itemName = "ground beef"),
        Ingredient(amount = 1.0, measureType = MeasureType.CAN, itemName = "tomato sauce"),
        Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "onion, diced"),
        Ingredient(amount = 2.0, measureType = MeasureType.CLOVE, itemName = "garlic, minced"),
        Ingredient(amount = 0.0, measureType = MeasureType.PINCH, itemName = "Salt and pepper to taste"),
        Ingredient(amount = 0.0, measureType = MeasureType.SLICE, itemName = "Grated Parmesan cheese"),
    ),
    instructions = listOf(
        Instruction("Cook the spaghetti according to the package instructions."),
        Instruction("In a separate pan, sauté the onion and garlic until translucent."),
        Instruction("Add the ground beef to the pan and cook until browned."),
        Instruction("Pour in the tomato sauce and let it simmer for 15 minutes."),
        Instruction("Season with salt and pepper."),
        Instruction("Serve the sauce over the spaghetti and top with grated Parmesan cheese."),
    ),
    preparationTimeMin = 30,
    favourite = true,
    servings = 2,
    tags = listOf("Dinner", "Pasta", "Italian"),
)
