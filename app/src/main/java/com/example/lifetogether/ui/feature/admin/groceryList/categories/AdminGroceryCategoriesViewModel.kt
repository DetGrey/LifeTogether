package com.example.lifetogether.ui.feature.admin.groceryList.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val UNCATEGORIZED_NAME = "Uncategorized"
private val UNCATEGORIZED_CATEGORY = Category(
    emoji = "❓️",
    name = UNCATEGORIZED_NAME,
)

@HiltViewModel
class AdminGroceryCategoriesViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminGroceryCategoriesUiState())
    val uiState: StateFlow<AdminGroceryCategoriesUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    init {
        observeCategories()
    }

    fun onEvent(event: AdminGroceryCategoriesUiEvent) {
        when (event) {
            is AdminGroceryCategoriesUiEvent.NewCategoryChanged -> updateUiState {
                it.copy(newCategory = event.value)
            }
            AdminGroceryCategoriesUiEvent.AddCategoryClicked -> addCategory()
            is AdminGroceryCategoriesUiEvent.DeleteCategoryClicked -> showDeleteConfirmation(event.category)
            AdminGroceryCategoriesUiEvent.DismissDeleteCategoryConfirmation -> dismissDeleteConfirmation()
            AdminGroceryCategoriesUiEvent.ConfirmDeleteCategory -> deleteCategory()
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            groceryRepository.observeCategories().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val categories = result.data
                            .filterNot { it.name == UNCATEGORIZED_NAME }
                            .sortedBy { it.name }
                            .let { listOf(UNCATEGORIZED_CATEGORY) + it }
                        updateUiState { state ->
                            state.copy(groceryCategories = categories)
                        }
                    }

                    is Result.Failure -> {
                        updateUiState { state ->
                            state.copy(groceryCategories = emptyList())
                        }
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmation(category: Category) {
        updateUiState {
            it.copy(
                selectedCategory = category,
                showDeleteCategoryConfirmationDialog = true,
            )
        }
    }

    private fun dismissDeleteConfirmation() {
        updateUiState {
            it.copy(
                showDeleteCategoryConfirmationDialog = false,
                selectedCategory = null,
            )
        }
    }

    private fun addCategory() {
        val newCategory = _uiState.value.newCategory
        if (newCategory.isEmpty() && !newCategory.contains(" ")) {
            return
        }

        val categoryAsList = newCategory.split(" ", limit = 2)
        if (categoryAsList.size < 2) {
            showError("Please enter both an emoji and a category name")
            return
        }
        val category = Category(emoji = categoryAsList[0], name = categoryAsList[1].trim())

        viewModelScope.launch {
            when (val result = groceryRepository.addCategory(category)) {
                is Result.Success -> updateUiState { state ->
                    state.copy(newCategory = "")
                }

                is Result.Failure -> {
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun deleteCategory() {
        val category = _uiState.value.selectedCategory ?: return
        viewModelScope.launch {
            when (val result = groceryRepository.deleteCategory(category)) {
                is Result.Success -> dismissDeleteConfirmation()
                is Result.Failure -> {
                    showError(result.error.toUserMessage())
                    dismissDeleteConfirmation()
                }
            }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                ),
            )
        }
    }

    private fun updateUiState(transform: (AdminGroceryCategoriesUiState) -> AdminGroceryCategoriesUiState) {
        _uiState.update(transform)
    }
}
