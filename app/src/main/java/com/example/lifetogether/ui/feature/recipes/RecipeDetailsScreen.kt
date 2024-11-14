package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.lifetogether.ui.common.CompletableCategoryList
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AuthViewModel
import java.util.Date

@Composable
fun RecipeDetailsScreen(
    appNavigator: AppNavigator? = null,
    authViewModel: AuthViewModel? = null,
    recipeId: String?,
) {
    val recipeDetailsViewModel: RecipesViewModel = hiltViewModel() // TODO

    val userInformationState by authViewModel?.userInformation!!.collectAsState()

    val recipes = listOf<Recipe>(EXAMPLE_RECIPE)
    val recipe = recipes.find { it.id == recipeId } ?: Recipe()

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

                    Text(
                        text = "Top Right",
                        modifier = Modifier
                            .padding(end = 10.dp, top = 10.dp)
                            .align(Alignment.TopEnd),
                    )

                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp, end = 40.dp)
                            .align(Alignment.BottomStart),
                    ) {
                        Text(
                            text = recipe.itemName.ifEmpty { "Item name" }, // TODO make empty string
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                            .height(40.dp)
                            .aspectRatio(1f)
                            .clickable {
                                // TODO go to edit mode and hide when in edit mode
                                // TODO add a save button???
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

        item {
            Column(
                modifier = Modifier
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                Text(
                    text = recipe.description.ifEmpty { "A classic Italian pasta dish with a rich, savory sauce with more more more more." }, // TODO Make empty
                    style = MaterialTheme.typography.bodyLarge,
                )

                CompletableCategoryList(
                    category = Category(
                        "🍎",
                        "Ingredients",
                    ),
                    itemList = EXAMPLE_RECIPE.ingredients,
                    true,
                    onClick = {},
                    onCompleteToggle = {},
                )

                CompletableCategoryList(
                    category = Category(
                        "✔️",
                        "Instructions",
                    ),
                    itemList = EXAMPLE_RECIPE.instructions,
                    true,
                    onClick = {},
                    onCompleteToggle = {},
                )
            }
        }
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
