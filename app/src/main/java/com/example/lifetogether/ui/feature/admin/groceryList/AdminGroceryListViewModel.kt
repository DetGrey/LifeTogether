package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.usecase.item.DeleteCompletedItemsUseCase
import com.example.lifetogether.domain.usecase.item.FetchCategoriesUseCase
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.ToggleCompletableItemCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGroceryListViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val toggleCompletableItemCompletionUseCase: ToggleCompletableItemCompletionUseCase,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val deleteCompletedItemsUseCase: DeleteCompletedItemsUseCase,
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

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    fun setUpGroceryList() {
        fetchCategories()
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
                        updateExpandedStates()
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
            updateExpandedStates()
        }
    }

    // ---------------------------------------------------------------- EXPANDED STATES
    private val _categoryExpandedStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val categoryExpandedStates: StateFlow<Map<String, Boolean>> = _categoryExpandedStates.asStateFlow()

    var completedSectionExpanded: Boolean by mutableStateOf(false)

    private fun updateExpandedStates() {
        // Ensure each category has an expanded state entry
        val currentStates = _categoryExpandedStates.value.toMutableMap()
        groceryCategories.value.forEach { category ->
            currentStates.putIfAbsent(category.name, true)
        }
        _categoryExpandedStates.value = currentStates
//        println("GroceryListViewModel updateExpandedStates() categories: ${groceryCategories.value}")
//        println("GroceryListViewModel updateExpandedStates() categoryExpandedStates: $currentStates")
    }

    fun toggleCategoryExpanded(categoryName: String) {
        val currentStates = _categoryExpandedStates.value.toMutableMap()
        val currentState = currentStates[categoryName] ?: true
        currentStates[categoryName] = !currentState
        _categoryExpandedStates.value = currentStates
    }

    // ---------------------------------------------------------------- NEW ITEM
    var newItemText: String by mutableStateOf("")
    var newItemCategory: Category by mutableStateOf(uncategorizedCategory)

    fun updateNewItemCategory(category: Category?) {
        newItemCategory = category
            ?: uncategorizedCategory
        println("New category: $newItemCategory")
    }
}
