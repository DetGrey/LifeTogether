package com.example.lifetogether.ui.feature.admin.groceryList

import com.example.lifetogether.domain.result.toUserMessage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminGroceryCategoriesViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository,
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
        observeCategories()
    }

    // ---------------------------------------------------------------- CATEGORIES
    private val _groceryCategories = MutableStateFlow<List<Category>>(emptyList())
    val groceryCategories: StateFlow<List<Category>> = _groceryCategories.asStateFlow()

    private fun observeCategories() {
        viewModelScope.launch {
            groceryRepository.observeCategories().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _groceryCategories.value = result.data
                            .filterNot { it.name == "Uncategorized" }
                            .sortedBy { it.name }
                            .let { listOf(Category("❓️", "Uncategorized")) + it }
                    }

                    is Result.Failure -> {
                        _groceryCategories.value = emptyList()
                        error = result.error.toUserMessage()
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
            val result = groceryRepository.addCategory(category)
            if (result is Result.Success) {
                newCategory = ""
            } else if (result is Result.Failure) {
                println("Error: ${result.error.toUserMessage()}")
                error = result.error.toUserMessage()
                showAlertDialog = true
            }
        }
    }

    // ---------------------------------------------------------------- DELETE CATEGORY
    fun deleteCategory() {
        val category = selectedCategory ?: return
        viewModelScope.launch {
            val result = groceryRepository.deleteCategory(category)

            if (result is Result.Failure) {
                println("Error: ${result.error.toUserMessage()}")
                error = result.error.toUserMessage()
                showAlertDialog = true
            }

            showDeleteCategoryConfirmationDialog = false
        }
    }
}
