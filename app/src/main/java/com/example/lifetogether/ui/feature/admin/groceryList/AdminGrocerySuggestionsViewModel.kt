package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.GrocerySuggestionsListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.usecase.item.AddCategoryUseCase
import com.example.lifetogether.domain.usecase.item.DeleteGrocerySuggestionUseCase
import com.example.lifetogether.domain.usecase.item.FetchCategoriesUseCase
import com.example.lifetogether.domain.usecase.item.FetchGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.item.SaveGrocerySuggestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGrocerySuggestionsViewModel @Inject constructor(
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val fetchGrocerySuggestionsUseCase: FetchGrocerySuggestionsUseCase,
    private val deleteGrocerySuggestionUseCase: DeleteGrocerySuggestionUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val saveGrocerySuggestionUseCase: SaveGrocerySuggestionUseCase,
) : ViewModel() {
    var showDeleteCategoryConfirmationDialog: Boolean by mutableStateOf(false)
    var selectedSuggestion: GrocerySuggestion? by mutableStateOf(null)

    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    fun setUpGrocerySuggestions() {
        viewModelScope.launch {
            fetchCategories()
            fetchGrocerySuggestions()
        }
    }

    // ---------------------------------------------------------------- CATEGORIES
    private val uncategorizedCategory: Category = Category(
        emoji = "❓️",
        name = "Uncategorized",
    )

    private val _groceryCategories = MutableStateFlow<List<Category>>(emptyList())
    val groceryCategories: StateFlow<List<Category>> = _groceryCategories.asStateFlow()

    private fun fetchCategories() {
        println("GroceryListViewModel before calling fetchCategoriesUseCase")
        viewModelScope.launch {
            fetchCategoriesUseCase().collect { result ->
                println("GroceryListViewModel fetchCategoriesUseCase result: $result")
                when (result) {
                    is CategoriesListener.Success -> {
                        println("GroceryListViewModel categories updated: ${result.listItems}")
                        _groceryCategories.value = result.listItems
                            .filterNot { it.name == "Uncategorized" }
                            .sortedBy { it.name }
                            .let { listOf(Category("❓️", "Uncategorized")) + it }
                    }

                    is CategoriesListener.Failure -> {
                        _groceryCategories.value = emptyList()
                        // Handle failure, e.g., show an error message
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    private fun updateCategories(newCategory: Category) {
        if (!groceryCategories.value.contains(newCategory)) {
            println("adding new category: $newCategory")
            _groceryCategories.value = _groceryCategories.value
                .filterNot { it.name == "Uncategorized" }
                .plus(newCategory)
                .sortedBy { it.name }
                .let { listOf(Category("❓️", "Uncategorized")) + it }
        }
    }

    // ---------------------------------------------------------------- GROCERY SUGGESTIONS
    private val _grocerySuggestions = MutableStateFlow<List<GrocerySuggestion>>(emptyList())
    val grocerySuggestions: StateFlow<List<GrocerySuggestion>> = _grocerySuggestions.asStateFlow()

    private fun fetchGrocerySuggestions() {
        println("GroceryListViewModel before calling fetchGrocerySuggestionsUseCase")
        viewModelScope.launch {
            fetchGrocerySuggestionsUseCase().collect { result ->
                println("GroceryListViewModel fetchGrocerySuggestionsUseCase result: $result")
                when (result) {
                    is GrocerySuggestionsListener.Success -> {
                        println("GroceryListViewModel categories updated: ${result.listItems}")
                        _grocerySuggestions.value = result.listItems
                            .sortedBy { it.category?.name }
                    }

                    is GrocerySuggestionsListener.Failure -> {
                        _grocerySuggestions.value = emptyList()
                        // Handle failure, e.g., show an error message
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- NEW ITEM
    var newSuggestionText: String by mutableStateOf("")
    var newSuggestionCategory: Category by mutableStateOf(uncategorizedCategory)

    fun updateNewSuggestionCategory(category: Category?) {
        newSuggestionCategory = category
            ?: uncategorizedCategory
        println("New category: $newSuggestionCategory")
    }

    // ---------------------------------------------------------------- ADD CATEGORY
    fun addNewGrocerySuggestion() {
        if (newSuggestionText.isEmpty()) {
            error = "Please enter a suggestion first"
            showAlertDialog = true
            return
        }

        val grocerySuggestion = GrocerySuggestion(
            category = newSuggestionCategory,
            suggestionName = newSuggestionText,
        )

        viewModelScope.launch {
            val result: ResultListener = saveGrocerySuggestionUseCase.invoke(grocerySuggestion)
            if (result is ResultListener.Success) {
                updateNewSuggestionCategory(null)
                newSuggestionText = ""
            } else if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }
        }
    }

    // ---------------------------------------------------------------- DELETE CATEGORY
    fun deleteCategory() {
        if (selectedSuggestion == null) {
            return
        }
        viewModelScope.launch {
            val result: ResultListener = deleteGrocerySuggestionUseCase.invoke(selectedSuggestion!!)

            if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }

            showDeleteCategoryConfirmationDialog = false
        }
    }
}
