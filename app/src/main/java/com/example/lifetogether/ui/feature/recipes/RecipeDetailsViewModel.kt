package com.example.lifetogether.ui.feature.recipes

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ItemResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.logic.toMutableRecipe
import com.example.lifetogether.domain.logic.toRecipe
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.MutableRecipe
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.usecase.item.DeleteItemUseCase
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.UpdateItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@SuppressLint("MutableCollectionMutableState")
@HiltViewModel
class RecipeDetailsViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
) : ViewModel() {
    var showConfirmationDialog: Boolean by mutableStateOf(false)

    // ---------------------------------------------------------------- ERROR
    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    // ---------------------------------------------------------------- editMode
    var editMode: Boolean by mutableStateOf(false)

    fun toggleEditMode() {
        if (editMode) {
            updateRecipeFlow(_originalRecipe.value.toRecipe())
        }
        editMode = !editMode
    }

    // ---------------------------------------------------------------- Family Id
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
        } else {
            editMode = true
        }
    }

    // ---------------------------------------------------------------- EXPANDED STATES
    var expandedStates by mutableStateOf(mutableMapOf("ingredients" to true, "instructions" to true))

    fun toggleExpandedStates(name: String) {
        val currentState = expandedStates[name] ?: true
        expandedStates = expandedStates.toMutableMap().apply { put(name, !currentState) }
    }

    // ---------------------------------------------------------------- RECIPE
    private val _originalRecipe = MutableStateFlow<MutableRecipe>(MutableRecipe())
    private val _recipe = MutableStateFlow<MutableRecipe>(MutableRecipe())
    val recipe: StateFlow<MutableRecipe> = _recipe.asStateFlow()

    private fun updateRecipeFlow(recipe: Recipe) {
        println("_recipe old value: ${_recipe.value}")
        _originalRecipe.value = recipe.toMutableRecipe()
        _recipe.value = recipe.toMutableRecipe()
        println("recipe new value: ${this@RecipeDetailsViewModel.recipe.value}")
        preparationTimeMin = recipe.preparationTimeMin.toString()
        servings = recipe.servings.toString()
        tags = recipe.tags.joinToString(" ")
        ingredientsByServings()
    }

    private fun fetchRecipe(
        recipeId: String,
    ) {
        viewModelScope.launch {
            fetchItemByIdUseCase.invoke(familyId!!, recipeId, Constants.RECIPES_TABLE, Recipe::class).collect { result ->
                println("fetchItemByIdUseCase result: $result")
                when (result) {
                    is ItemResultListener.Success -> {
                        if (result.item is Recipe) {
                            updateRecipeFlow(result.item)
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

    fun recipeAddNewItemToList(item: Completable) {
        println("Trying to add to list: $item")
        when (item) {
            is Ingredient -> {
                val updatedIngredients = _recipe.value.ingredients.toMutableList()
                updatedIngredients.add(item)
                val newRecipe = _recipe.value.toRecipe().copy(ingredients = updatedIngredients)
                _recipe.value = newRecipe.toMutableRecipe()
            }
            is Instruction -> {
                val updatedInstructions = _recipe.value.instructions.toMutableList()
                updatedInstructions.add(item)
                val newRecipe = _recipe.value.toRecipe().copy(instructions = updatedInstructions)
                _recipe.value = newRecipe.toMutableRecipe()
            }
        }
    }

    // ---------------------------------------------------------------- SERVINGS + TAGS ETC
    var preparationTimeMin: String by mutableStateOf("")
    var servings: String by mutableStateOf("")
    var servingsExpanded: Boolean by mutableStateOf(false)
    var tags: String by mutableStateOf("")
    var ingredientsByServings: List<Ingredient> by mutableStateOf(recipe.value.ingredients)

    fun ingredientsByServings() {
        val multiplier = servings.toDouble() / recipe.value.servings.toDouble()
        println("multiplier: $multiplier")
        println("list: ${recipe.value.ingredients.map { it.copy(amount = it.amount * multiplier) }}")
        ingredientsByServings = recipe.value.ingredients.map { it.copy(amount = it.amount * multiplier) }
    }

    // ---------------------------------------------------------------- USE CASES
    // USE CASES
    fun saveRecipe(
        recipeId: String?,
        onSuccess: () -> Unit,
    ) {
        println("RecipeDetailsViewModel saveRecipe()")

        if (servings.isNotEmpty()) {
            _recipe.value.servings = servings.toInt()
        }
        if (preparationTimeMin.isNotEmpty()) {
            _recipe.value.preparationTimeMin = preparationTimeMin.toInt()
        }
        if (tags.isNotEmpty()) {
            _recipe.value.tags = tags.lowercase().split(" ")
        }

        if (recipe.value.itemName.isEmpty()) {
            error = "Please write some text first"
            showAlertDialog = true
            return
        }

        val newRecipe = familyId?.let {
            Recipe(
                id = recipeId,
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

        if (newRecipe == null) {
            error = "Please connect to a family first"
            showAlertDialog = true
            return
        }

        viewModelScope.launch {
            println("newRecipe id: ${newRecipe.id}")
            if (newRecipe.id.isNullOrEmpty()) {
                println("Chosen use case: Recipe is saved as a new one")
                val result: StringResultListener = saveItemUseCase.invoke(newRecipe, Constants.RECIPES_TABLE)

                if (result is StringResultListener.Success) {
                    onSuccess()
                } else if (result is StringResultListener.Failure) {
                    println("Error: ${result.message}")
                    error = result.message
                    showAlertDialog = true
                }
            } else {
                println("Chosen use case: Recipe is updated in old")
                val result: ResultListener = updateItemUseCase.invoke(newRecipe, Constants.RECIPES_TABLE)

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

    fun deleteRecipe(
        recipeId: String,
        onSuccess: () -> Unit,
    ) {
        println("RecipeDetailsViewModel deleteRecipe()")
        println("recipeId: $recipeId")

        if (recipeId.isEmpty()) {
            error = "Recipe not saved - no id"
            showAlertDialog = true
            return
        }

        viewModelScope.launch {
            val result: ResultListener = deleteItemUseCase.invoke(recipeId, Constants.RECIPES_TABLE)

            if (result is ResultListener.Success) {
                onSuccess()
            } else if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }
            showConfirmationDialog = false
        }
    }
}
