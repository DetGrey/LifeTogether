package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val fetchListItemsUseCase: FetchListItemsUseCase,
) : ViewModel() {
    val tagsList: List<String> = listOf("All", "Simple", "Dinner", "Breakfast", "Dessert", "Pasta", "Rice")

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
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    fun setUpRecipes(addedFamilyId: String) {
        if (!familyIdIsSet) {
            println("RecipesViewModel setting familyId")
            familyId = addedFamilyId
            familyIdIsSet = true
        }

        viewModelScope.launch {
            fetchListItemsUseCase(familyId!!, Constants.RECIPES_TABLE, Recipe::class).collect { result ->
                println("fetchListItemsUseCase result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        // Filter and map the result.listItems to only include GroceryItem instances
                        val foundRecipes = result.listItems.filterIsInstance<Recipe>()
                        if (foundRecipes.isNotEmpty()) {
                            println("_recipe old value: ${_recipes.value}")
                            _recipes.value = foundRecipes
                            println("recipe new value: ${this@RecipesViewModel.recipes.value}")
                        } else {
                            println("Error: No Recipe instances found in the result")
                            error = "No Recipe instances found in the result"
                            showAlertDialog = true
                        }
                    }

                    is ListItemsResultListener.Failure -> {
                        // Handle failure, e.g., show an error message
                        println("Error: ${result.message}")
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }
}
