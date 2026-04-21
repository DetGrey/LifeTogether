package com.example.lifetogether.ui.feature.groceryList

import com.example.lifetogether.domain.result.toUserMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.CategoryRepository
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

private const val ALERT_DISMISS_DELAY_MS = 3000L
private const val UNCATEGORIZED_NAME = "Uncategorized"
private val UNCATEGORIZED_CATEGORY = Category(
    emoji = "❓️",
    name = UNCATEGORIZED_NAME,
)

data class GroceryListUiState(
    val showConfirmationDialog: Boolean = false,
    val showAlertDialog: Boolean = false,
    val error: String = "",
    val isLoading: Boolean = true,
    val groceryList: List<GroceryItem> = emptyList(),
    val completedItems: List<GroceryItem> = emptyList(),
    val categorizedItems: Map<Category, List<GroceryItem>> = emptyMap(),
    val groceryCategories: List<Category> = emptyList(),
    val categoryExpandedStates: Map<String, Boolean> = emptyMap(),
    val completedSectionExpanded: Boolean = false,
    val expectedTotalPrice: Float? = null,
    val newItemText: String = "",
    val newItemPrice: String = "",
    val newItemCategory: Category = UNCATEGORIZED_CATEGORY,
    val allGrocerySuggestions: List<GrocerySuggestion> = emptyList(),
    val currentGrocerySuggestions: List<GrocerySuggestion> = emptyList(),
)

@HiltViewModel
class GroceryListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val categoryRepository: CategoryRepository,
    private val groceryRepository: GroceryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GroceryListUiState())
    val uiState: StateFlow<GroceryListUiState> = _uiState.asStateFlow()

    private var familyId: String? = null

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != null && newFamilyId != familyId) {
                    familyId = newFamilyId
                    setUpGroceryList()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                }
            }
        }
    }

    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(ALERT_DISMISS_DELAY_MS)
            updateUiState { state ->
                state.copy(
                    showAlertDialog = false,
                    error = "",
                )
            }
        }
    }

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    private fun setUpGroceryList() {
        fetchCategories()
        fetchGrocerySuggestions()

        val familyId = familyId ?: return //todo maybe not the best way

        viewModelScope.launch {
            groceryRepository.observeGroceryItems(familyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        println("Items found: ${result.data}")
                        val groceryItems = result.data
                        if (groceryItems.isNotEmpty()) {
                            updateUiState { state ->
                                state.copy(groceryList = groceryItems)
                            }
                            updateExpandedStates()
                        } else {
                            println("Error: No GroceryItem instances found in the result")
                        }
                    }

                    is Result.Failure -> {
                        println("Error: ${result.error.toUserMessage()}")
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun updateCategorizedItems(list: List<GroceryItem>): Map<Category, List<GroceryItem>> {
        println("GroceryListViewModel updateCategorizedItems() initial list: $list")

        val categorizedMap = list
            .filter { !it.completed }
            .groupBy { item ->
                item.category?.takeIf { it.name != UNCATEGORIZED_NAME } ?: UNCATEGORIZED_CATEGORY
            }
            .mapValues { entry ->
                entry.value.sortedBy { it.itemName }
            }
            .toSortedMap(compareBy { it.name })
        println("GroceryListViewModel updateCategorizedItems() categorizedMap: $categorizedMap")
        return categorizedMap
    }

    // ---------------------------------------------------------------- CATEGORIES
    private fun fetchCategories() {
        println("GroceryListViewModel before calling getCategories")
        viewModelScope.launch {
            categoryRepository.getCategories().collect { result ->
                println("GroceryListViewModel getCategories result: $result")
                when (result) {
                    is Result.Success -> {
                        println("GroceryListViewModel categories updated: ${result.data}")
                        val categories = result.data
                            .filterNot { it.name == "Uncategorized" }
                            .sortedBy { it.name }
                            .let { listOf(UNCATEGORIZED_CATEGORY) + it }
                        updateUiState { state ->
                            state.copy(groceryCategories = categories)
                        }
                        updateExpandedStates()
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

    // ---------------------------------------------------------------- GROCERY SUGGESTIONS
    private fun fetchGrocerySuggestions() {
        println("GroceryListViewModel before calling getGrocerySuggestions")
        viewModelScope.launch {
            groceryRepository.getGrocerySuggestions().collect { result ->
                println("GroceryListViewModel getGrocerySuggestions result: $result")
                when (result) {
                    is Result.Success -> {
                        println("GroceryListViewModel categories updated: ${result.data}")
                        updateUiState { state ->
                            state.copy(
                                allGrocerySuggestions = result.data
                                    .sortedBy { it.suggestionName }
                            )
                        }
                    }

                    is Result.Failure -> {
                        updateUiState { state ->
                            state.copy(allGrocerySuggestions = emptyList())
                        }
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- EXPANDED STATES
    private fun updateExpandedStates() {
        updateUiState { state ->
            val currentStates = state.categoryExpandedStates.toMutableMap()
            state.groceryCategories.forEach { category ->
                currentStates.putIfAbsent(category.name.trim(), true)
            }
            state.copy(categoryExpandedStates = currentStates)
        }
    }

    fun toggleCategoryExpanded(categoryName: String) {
        updateUiState { state ->
            val currentStates = state.categoryExpandedStates.toMutableMap()
            val currentState = currentStates[categoryName] ?: true
            currentStates[categoryName] = !currentState
            state.copy(categoryExpandedStates = currentStates)
        }
    }

    fun toggleCompletedSectionExpanded() {
        updateUiState { state ->
            state.copy(completedSectionExpanded = !state.completedSectionExpanded)
        }
    }

    fun showDeleteCompletedConfirmation() {
        updateUiState { state ->
            state.copy(showConfirmationDialog = true)
        }
    }

    fun dismissDeleteCompletedConfirmation() {
        updateUiState { state ->
            state.copy(showConfirmationDialog = false)
        }
    }

    // ---------------------------------------------------------------- NEW ITEM
    fun onNewItemTextChange(value: String) {
        updateUiState { state ->
            state.copy(newItemText = value)
        }
    }

    fun onNewItemPriceChange(value: String) {
        updateUiState { state ->
            state.copy(newItemPrice = value)
        }
    }

    fun updateNewItemCategory(category: Category?) {
        updateUiState { state ->
            state.copy(newItemCategory = category ?: UNCATEGORIZED_CATEGORY)
        }
    }

    fun applySuggestion(suggestion: GrocerySuggestion) {
        updateUiState { state ->
            state.copy(
                newItemText = suggestion.suggestionName,
                newItemCategory = suggestion.category ?: UNCATEGORIZED_CATEGORY,
                newItemPrice = suggestion.approxPrice?.toString() ?: "",
            )
        }
    }

    // ---------------------------------------------------------------- ADD NEW ITEM
    fun addItemToList() {
        println("GroceryListViewModel addItemToList()")
        val state = _uiState.value

        if (state.newItemText.isEmpty()) {
            showError("Please write some text first")
            return
        }

        val price = state.newItemPrice.toFloatOrNull()

        val groceryItem = familyId?.let {
            GroceryItem(
                familyId = it,
                category = state.newItemCategory,
                itemName = state.newItemText,
                lastUpdated = Date(System.currentTimeMillis()),
                completed = false,
                approxPrice = price
            )
        }
        if (groceryItem == null) {
            showError("Please connect to a family first")
            return
        }

        viewModelScope.launch {
            when (val result = groceryRepository.saveItem(groceryItem)) {
                is Result.Success -> {
                    updateUiState { currentState ->
                        currentState.copy(
                            newItemCategory = UNCATEGORIZED_CATEGORY,
                            newItemText = "",
                            newItemPrice = "",
                        )
                    }
                }
                is Result.Failure -> {
                    println("Error: ${result.error.toUserMessage()}")
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    // ---------------------------------------------------------------- TOGGLE ITEM COMPLETION
    fun toggleItemCompleted(
        oldItem: GroceryItem,
    ) {
        updateUiState { state ->
            state.copy(isLoading = true)
        }
        val newItem = oldItem.copy(completed = !oldItem.completed, lastUpdated = Date(System.currentTimeMillis()))

        viewModelScope.launch {
            when (val result = groceryRepository.toggleGroceryItemBought(newItem)) {
                is Result.Success -> {
                    updateUiState { state ->
                        state.copy(isLoading = false)
                    }
                }
                is Result.Failure -> {
                    println("Error: ${result.error.toUserMessage()}")
                    showError(result.error.toUserMessage())
                    updateUiState { state ->
                        state.copy(isLoading = false)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- DELETE COMPLETED ITEMS
    fun deleteCompletedItems() {
        val completedItems = _uiState.value.completedItems
        if (completedItems.isEmpty()) {
            return
        }
        val idsToDelete = completedItems.mapNotNull { it.id }

        viewModelScope.launch {
            groceryRepository.deleteGroceryItems(itemIds = idsToDelete)
            //todo handle errors
            dismissDeleteCompletedConfirmation()
        }
    }

    private fun showError(message: String) {
        updateUiState { state ->
            state.copy(
                error = message,
                showAlertDialog = true,
            )
        }
    }

    private fun updateUiState(transform: (GroceryListUiState) -> GroceryListUiState) {
        _uiState.update { currentState ->
            enrichDerivedState(transform(currentState))
        }
    }

    private fun enrichDerivedState(state: GroceryListUiState): GroceryListUiState {
        val completedItems = state.groceryList.filter { item ->
            item.completed
        }
        val categorizedItems = updateCategorizedItems(state.groceryList)
        val currentSuggestions = if (state.newItemText.isNotEmpty()) {
            state.allGrocerySuggestions.filter { suggestion ->
                suggestion.suggestionName.startsWith(state.newItemText, ignoreCase = true)
            }.take(5)
        } else {
            emptyList()
        }
        val expectedTotalPrice = state.groceryList
            .filter { !it.completed }
            .mapNotNull { it.approxPrice }
            .takeIf { it.isNotEmpty() }
            ?.sum()

        return state.copy(
            completedItems = completedItems,
            categorizedItems = categorizedItems,
            currentGrocerySuggestions = currentSuggestions,
            expectedTotalPrice = expectedTotalPrice
        )
    }
}
