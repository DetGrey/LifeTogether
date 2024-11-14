package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ItemResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.recipe.MutableRecipe
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.recipe.toMutableRecipe
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class RecipeDetailsViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
) : ViewModel() {
    var showConfirmationDialog: Boolean by mutableStateOf(false)

    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    // ---------------------------------------------------------------- UID
    private var familyIdIsSet = false
    var familyId: String? = null

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    fun setUpRecipeDetails(addedFamilyId: String, recipeId: String?) {
        if (!familyIdIsSet) {
            println("RecipeDetailsViewModel setting UID")
            familyId = addedFamilyId
            familyIdIsSet = true
        }

        if (recipeId is String) {
            fetchRecipe(recipeId)
        }
    }

    // ---------------------------------------------------------------- EXPANDED STATES
    var expandedStates: MutableMap<String, Boolean> = mutableMapOf("ingredients" to true, "instructions" to true)

    fun toggleCategoryExpanded(name: String) {
        val currentState = expandedStates[name] ?: true
        expandedStates[name] = !currentState
    }

    // ---------------------------------------------------------------- RECIPE
    private val _recipe = MutableStateFlow<MutableRecipe>(MutableRecipe())
    val recipe: StateFlow<MutableRecipe> = _recipe.asStateFlow()
//    var recipe: MutableRecipe = MutableRecipe()

    private fun fetchRecipe(
        recipeId: String,
    ) {
        viewModelScope.launch {
            fetchItemByIdUseCase(familyId!!, recipeId, Constants.GROCERY_TABLE, Recipe::class).collect { result ->
                println("fetchItemByIdUseCase result: $result")
                when (result) {
                    is ItemResultListener.Success -> {
                        // Filter and map the result.listItems to only include GroceryItem instances
                        if (result.item is Recipe) {
                            println("_groceryList old value: ${_recipe.value}")
                            _recipe.value = result.item.toMutableRecipe()
                            println("groceryList new value: ${this@RecipeDetailsViewModel.recipe.value}")
                        } else {
                            println("Error: No recipe found")
                            error = "No recipe found"
                            showAlertDialog = true
                        }
                    }

                    is ItemResultListener.Failure -> {
                        // Handle failure, e.g., show an error message
                        println("Error: ${result.message}")
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- ADD NEW ITEM
    // USE CASES
    fun addRecipeToList(
        onSuccess: () -> Unit,
    ) {
        println("GroceryListViewModel addItemToList()")

        if (recipe.value.itemName.isEmpty()) { // TODO add more checks
            error = "Please write some text first"
            showAlertDialog = true
            return
        }

        val recipe = familyId?.let {
            Recipe(
                familyId = familyId!!,
                itemName = recipe.value.itemName,
                lastUpdated = Date(),
                description = recipe.value.description,
                ingredients = recipe.value.ingredients,
                instructions = recipe.value.instructions,
                preparationTimeMin = recipe.value.preparationTimeMin,
                favourite = recipe.value.favourite,
                servings = recipe.value.servings,
                tags = recipe.value.tags,
            )
        }
        if (recipe == null) {
            error = "Please connect to a family first"
            showAlertDialog = true
            return
        }
        ""
        viewModelScope.launch {
            val result: ResultListener = saveItemUseCase.invoke(recipe, Constants.RECIPES_TABLE)
            if (result is ResultListener.Success) {
                onSuccess()
            } else if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }
        }
    }
}
