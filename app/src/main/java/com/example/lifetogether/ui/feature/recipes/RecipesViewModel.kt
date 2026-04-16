package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
) : ViewModel() {
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

    private var familyId: String? = null

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _filteredRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val filteredRecipes: StateFlow<List<Recipe>> = _filteredRecipes

    // ---------------------------------------------------------------- TAGS AND FILTERS
    private val _tagsList: MutableList<String> = mutableListOf("All", "Simple", "Dinner", "Breakfast", "Dessert", "Pasta", "Rice")
    var tagsList: List<String> by mutableStateOf(_tagsList)
    var selectedTag: String by mutableStateOf("All")

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != null && newFamilyId != familyId) {
                    familyId = newFamilyId
                    setUpRecipes()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                }
            }
        }
    }

    private fun setUpRecipes() {
        val familyId = familyId ?: return
        viewModelScope.launch {
            fetchListItemsUseCase(familyId, Constants.RECIPES_TABLE, Recipe::class).collect { result ->
                println("fetchListItemsUseCase result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        val foundRecipes = result.listItems.filterIsInstance<Recipe>()
                        if (foundRecipes.isNotEmpty()) {
                            _recipes.value = foundRecipes
                            updateTagsList(foundRecipes.map { it.tags })
                        } else {
                            println("Error: No Recipe instances found in the result")
                            error = "No Recipe instances found in the result"
                            showAlertDialog = true
                        }
                    }

                    is ListItemsResultListener.Failure -> {
                        println("Error: ${result.message}")
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(_recipes, snapshotFlow { selectedTag }) { recipes, tag ->
                if (tag == "All") {
                    recipes
                } else {
                    recipes.filter { recipe ->
                        recipe.tags.any { recipeTag ->
                            recipeTag.equals(tag, ignoreCase = true)
                        }
                    }
                }
            }.collect { filteredList ->
                _filteredRecipes.value = filteredList.sortedBy { it.itemName.lowercase() }
            }
        }
    }

    private fun updateTagsList(
        list: List<List<String>>,
    ) {
        for (tags in list) {
            for (tag in tags) {
                // Capitalize the first letter and make the rest lowercase
                val normalizedTag = tag.replaceFirstChar { it.uppercase() }

                // Check if the normalized tag is already in the list, ignoring case
                if (_tagsList.none { it.equals(normalizedTag, ignoreCase = true) }) {
                    _tagsList.add(normalizedTag)
                }
            }
        }
        tagsList = _tagsList.toList()
    }
}
