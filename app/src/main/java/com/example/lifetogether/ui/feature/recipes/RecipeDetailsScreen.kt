package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.toggleCompleted
import com.example.lifetogether.ui.common.add.AddNewString
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.ui.common.image.ImageUploadDialog
import com.example.lifetogether.ui.common.list.CompletableCategoryList
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.textfield.EditableTextField
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import com.example.lifetogether.ui.viewmodel.ImageViewModel
import java.util.Date

@Composable
fun RecipeDetailsScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    recipeId: String? = null,
) {
    val recipeDetailsViewModel: RecipeDetailsViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val recipe by recipeDetailsViewModel.recipe.collectAsState()
    val bitmap by imageViewModel.bitmap.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        println("recipeDetails screen familyId: ${userInformation?.familyId}")
        userInformation?.familyId?.let { familyId ->
            recipeDetailsViewModel.setUpRecipeDetails(familyId, recipeId)

            recipeId?.let { recipeId ->
                imageViewModel.collectImageFlow(
                    imageType = ImageType.RecipeImage(familyId, recipeId),
                    onError = {
                        recipeDetailsViewModel.error = it
                        recipeDetailsViewModel.showAlertDialog = true
                    },
                )
            }
        }
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
                    if (bitmap != null) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize(),
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "recipe image",
                            contentScale = ContentScale.Crop,
                            alpha = 0.7f,
                        )
                    }

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
                            .height(if (!recipeDetailsViewModel.editMode && recipeId != null) 40.dp else 50.dp)
                            .aspectRatio(1f)
                            .clickable(
                                enabled = if (recipeDetailsViewModel.editMode) true else if (recipeId != null) true else false,
                            ) {
                                if (recipeDetailsViewModel.editMode) {
                                    imageViewModel.showImageUploadDialog = true
                                } else if (recipeId != null) {
                                    recipeDetailsViewModel.showConfirmationDialog = true
                                }
                            }
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (recipeDetailsViewModel.editMode) {
                            Text(
                                text = if (bitmap != null) "Change image" else "Add image",
                                textAlign = TextAlign.Right,
                            )
                        } else if (recipeId != null) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_trashcan_black),
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

                    Box(
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                            .height(40.dp)
                            .aspectRatio(1f)
                            .clickable {
                                recipeDetailsViewModel.toggleEditMode()
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

                if (recipeDetailsViewModel.editMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextDefault("Preparation time in minutes: ")
                        EditableTextField(
                            text = recipeDetailsViewModel.preparationTimeMin,
                            onTextChange = { recipeDetailsViewModel.preparationTimeMin = it },
                            label = "E.g. 30",
                            isEditable = recipeDetailsViewModel.editMode,
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(if (recipeDetailsViewModel.editMode) 1f else 0.45f)
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextDefault("Servings: ")
                        if (recipeDetailsViewModel.editMode) {
                            EditableTextField(
                                text = recipeDetailsViewModel.servings,
                                onTextChange = {
                                    recipeDetailsViewModel.servings = it
                                },
                                label = "E.g. 2",
                                isEditable = recipeDetailsViewModel.editMode,
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            )
                        } else {
                            Dropdown(
                                selectedValue = recipeDetailsViewModel.servings,
                                expanded = recipeDetailsViewModel.servingsExpanded,
                                onExpandedChange = { recipeDetailsViewModel.servingsExpanded = it },
                                options = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "15", "20", "30", "40"),
                                label = null,
                                onValueChangedEvent = {
                                    recipeDetailsViewModel.servings = it
                                    recipeDetailsViewModel.ingredientsByServings()
                                },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        TextDefault("Tags:")
                        if (recipeDetailsViewModel.editMode) {
                            EditableTextField(
                                text = recipeDetailsViewModel.tags,
                                onTextChange = { recipeDetailsViewModel.tags = it },
                                label = "E.g. \"dinner pasta\"",
                                isEditable = recipeDetailsViewModel.editMode,
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                                capitalization = false,
                            )
                        } else {
                            for (tag in recipe.tags) {
                                TagOption(
                                    tag = tag,
                                    selectedTag = tag,
                                )
                            }
                        }
                    }
                }

                if (recipeDetailsViewModel.editMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Column {
                    recipeDetailsViewModel.expandedStates["ingredients"]?.let { expanded ->
                        CompletableCategoryList(
                            category = Category(
                                "🍎",
                                "Ingredients",
                            ),
                            itemList = if (recipeDetailsViewModel.editMode) recipe.ingredients else recipeDetailsViewModel.ingredientsByServings,
                            expanded = expanded,
                            onClick = {
                                println("expanded before $expanded")
                                recipeDetailsViewModel.toggleExpandedStates("ingredients")
                                println("expanded after $expanded")
                            },
                            onCompleteToggle = {
                                if (recipeDetailsViewModel.editMode) {
                                    recipe.ingredients = recipe.ingredients.toggleCompleted(it.itemName)
                                } else {
                                    recipeDetailsViewModel.ingredientsByServings = recipeDetailsViewModel.ingredientsByServings.toggleCompleted(it.itemName)
                                }
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

                if (recipeDetailsViewModel.editMode) {
                    Spacer(modifier = Modifier.height(10.dp))
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

                if (recipeDetailsViewModel.editMode) {
                    Button(
                        onClick = {
                            recipeDetailsViewModel.saveRecipe(
                                recipeId,
                                onSuccess = {
                                    appNavigator?.navigateBack()
                                },
                            )
                        },
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(30.dp))

    // ---------------------------------------------------------------- CONFIRM DELETION OF RECIPE
    if (recipeDetailsViewModel.showConfirmationDialog) {
        if (recipeId != null) {
            ConfirmationDialog(
                onDismiss = { recipeDetailsViewModel.showConfirmationDialog = false },
                onConfirm = {
                    recipeDetailsViewModel.deleteRecipe(
                        recipeId,
                        onSuccess = {
                            appNavigator?.navigateBack()
                        },
                    )
                },
                dialogTitle = "Delete recipe",
                dialogMessage = "Are you sure you want to the recipe?",
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Delete",
            )
        } else {
            recipeDetailsViewModel.showConfirmationDialog = false
        }
    }

    // ---------------------------------------------------------------- IMAGE UPLOAD DIALOG
    if (imageViewModel.showImageUploadDialog && userInformation != null) {
        userInformation!!.familyId?.let { familyId ->
            recipeId?.let { recipeId ->
                ImageUploadDialog(
                    onDismiss = { imageViewModel.showImageUploadDialog = false },
                    onConfirm = { imageViewModel.showImageUploadDialog = false },
                    dialogTitle = "Upload recipe image",
                    dialogMessage = "Select an image for your recipe",
                    imageType = ImageType.RecipeImage(familyId, recipeId),
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Upload image",
                )
            }
        }
    }

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
