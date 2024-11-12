package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.usecase.item.AddCategoryUseCase
import com.example.lifetogether.domain.usecase.item.DeleteCategoryUseCase
import com.example.lifetogether.domain.usecase.item.FetchCategoriesUseCase
import com.example.lifetogether.domain.usecase.item.ToggleItemCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGroceryCategoriesViewModel @Inject constructor(
    private val toggleItemCompletionUseCase: ToggleItemCompletionUseCase,
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
) : ViewModel() {
    var showDeleteCategoryConfirmationDialog: Boolean by mutableStateOf(false)
    var selectedCategory: Category? by mutableStateOf(null)

    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    var isLoading = true // TODO might need to change to false!!! or mutablestate

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
    var newCategory: String by mutableStateOf("")

    // ---------------------------------------------------------------- ADD CATEGORY
    fun addCategory() {
        if (newCategory.isEmpty() && !newCategory.contains(" ")) {
            return
        }

        val categoryAsList = newCategory.split(" ", limit = 2)
        val category = Category(emoji = categoryAsList[0], name = categoryAsList[1].trim())

        viewModelScope.launch {
            val result: ResultListener = addCategoryUseCase.invoke(category)
            if (result is ResultListener.Success) {
                newCategory = ""
            }
            else if (result is ResultListener.Failure) {
                // TODO popup saying the error for 5 sec
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }
        }
    }

    // ---------------------------------------------------------------- DELETE CATEGORY
    fun deleteCategory() {
//        isLoading = true

        if (selectedCategory == null) {
            return
        }
        viewModelScope.launch {
            val result: ResultListener = deleteCategoryUseCase.invoke(selectedCategory!!)

            if (result is ResultListener.Failure) {
                // TODO popup saying the error for 5 sec
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }

            showDeleteCategoryConfirmationDialog = false
            //isLoading = false
        }
    }

}
