package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.CategoriesListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.usecase.item.AddCategoryUseCase
import com.example.lifetogether.domain.usecase.item.DeleteCategoryUseCase
import com.example.lifetogether.domain.usecase.item.FetchCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGroceryCategoriesViewModel @Inject constructor(
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

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    fun setUpCategories() {
        fetchCategories()
    }

    // ---------------------------------------------------------------- CATEGORIES
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
            } else if (result is ResultListener.Failure) {
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
            // isLoading = false
        }
    }
}
