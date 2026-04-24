package com.example.lifetogether.ui.feature.recipes

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.toMutableRecipe
import com.example.lifetogether.domain.logic.toRecipe
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.MutableRecipe
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
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
    private val sessionRepository: SessionRepository,
    private val recipeRepository: RecipeRepository,
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

    // ---------------------------------------------------------------- editMode
    var editMode: Boolean by mutableStateOf(false)

    fun toggleEditMode() {
        if (editMode) {
            updateRecipeFlow(_originalRecipe.value.toRecipe())
        }
        editMode = !editMode
    }

    // ---------------------------------------------------------------- Family Id
    private val _familyId = MutableStateFlow<String?>(null)
    val familyId: StateFlow<String?> = _familyId.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                _familyId.value = (state as? SessionState.Authenticated)?.user?.familyId
            }
        }
    }

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    fun setUp(recipeId: String?) {
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
    private val _originalRecipe = MutableStateFlow(MutableRecipe())
    private val _recipe = MutableStateFlow(MutableRecipe())
    val recipe: StateFlow<MutableRecipe> = _recipe.asStateFlow()

    private fun updateRecipeFlow(recipe: Recipe) {
        _originalRecipe.value = recipe.toMutableRecipe()
        _recipe.value = recipe.toMutableRecipe()
        preparationTimeMin = recipe.preparationTimeMin.toString()
        servings = recipe.servings.toString()
        tags = recipe.tags.joinToString(" ")
        ingredientsByServings()
    }

    private fun fetchRecipe(recipeId: String) {
        val familyIdValue = _familyId.value ?: run {
            error = "Not connected to a family"
            showAlertDialog = true
            return
        }
        viewModelScope.launch {
            recipeRepository.observeRecipeById(familyIdValue, recipeId).collect { result ->
                when (result) {
                    is Result.Success -> updateRecipeFlow(result.data)
                    is Result.Failure -> {
                        error = result.error
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    fun recipeAddNewItemToList(item: Completable) {
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
        ingredientsByServings = recipe.value.ingredients.map { it.copy(amount = it.amount * multiplier) }
    }

    fun saveRecipe(recipeId: String?, onSuccess: () -> Unit) {
        if (servings.isNotEmpty()) _recipe.value.servings = servings.toInt()
        if (preparationTimeMin.isNotEmpty()) _recipe.value.preparationTimeMin = preparationTimeMin.toInt()
        if (tags.isNotEmpty()) _recipe.value.tags = tags.lowercase().split(" ")

        if (recipe.value.itemName.isEmpty()) {
            error = "Please write some text first"
            showAlertDialog = true
            return
        }

        val newRecipe = _familyId.value?.let {
            Recipe(
                id = recipeId,
                familyId = it,
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
            if (newRecipe.id.isNullOrEmpty()) {
                when (val result = recipeRepository.saveRecipe(newRecipe)) {
                    is Result.Success -> onSuccess()
                    is Result.Failure -> {
                        error = result.error
                        showAlertDialog = true
                    }
                }
            } else {
                when (val result = recipeRepository.updateRecipe(newRecipe)) {
                    is Result.Success -> onSuccess()
                    is Result.Failure -> {
                        error = result.error
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    fun deleteRecipe(recipeId: String, onSuccess: () -> Unit) {
        if (recipeId.isEmpty()) {
            error = "Recipe not saved - no id"
            showAlertDialog = true
            return
        }

        viewModelScope.launch {
            when (val result = recipeRepository.deleteRecipe(recipeId)) {
                is Result.Success -> onSuccess()
                is Result.Failure -> {
                    error = result.error
                    showAlertDialog = true
                }
            }
            showConfirmationDialog = false
        }
    }
}
