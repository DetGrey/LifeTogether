package com.example.lifetogether.ui.feature.groceryList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.CategoriesListener
import com.example.lifetogether.domain.listener.GrocerySuggestionsListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.usecase.item.DeleteCompletedItemsUseCase
import com.example.lifetogether.domain.usecase.item.FetchCategoriesUseCase
import com.example.lifetogether.domain.usecase.item.FetchGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.ToggleCompletableItemCompletionUseCase
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.util.Constants
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
    private val saveItemUseCase: SaveItemUseCase,
    private val toggleCompletableItemCompletionUseCase: ToggleCompletableItemCompletionUseCase,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val deleteCompletedItemsUseCase: DeleteCompletedItemsUseCase,
    private val fetchGrocerySuggestionsUseCase: FetchGrocerySuggestionsUseCase,
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

        viewModelScope.launch {
            fetchListItemsUseCase(familyId!!, Constants.GROCERY_TABLE, GroceryItem::class).collect { result ->
                println("fetchListItemsUseCase result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        println("Items found: ${result.listItems}")
                        val groceryItems = result.listItems.filterIsInstance<GroceryItem>()
                        if (groceryItems.isNotEmpty()) {
                            updateUiState { state ->
                                state.copy(groceryList = groceryItems)
                            }
                            updateExpandedStates()
                        } else {
                            println("Error: No GroceryItem instances found in the result")
                        }
                    }

                    is ListItemsResultListener.Failure -> {
                        println("Error: ${result.message}")
                        showError(result.message)
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
        println("GroceryListViewModel before calling fetchCategoriesUseCase")
        viewModelScope.launch {
            fetchCategoriesUseCase().collect { result ->
                println("GroceryListViewModel fetchCategoriesUseCase result: $result")
                when (result) {
                    is CategoriesListener.Success -> {
                        println("GroceryListViewModel categories updated: ${result.listItems}")
                        val categories = result.listItems
                            .filterNot { it.name == "Uncategorized" }
                            .sortedBy { it.name }
                            .let { listOf(UNCATEGORIZED_CATEGORY) + it }
                        updateUiState { state ->
                            state.copy(groceryCategories = categories)
                        }
                        updateExpandedStates()
                    }

                    is CategoriesListener.Failure -> {
                        updateUiState { state ->
                            state.copy(groceryCategories = emptyList())
                        }
                        showError(result.message)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- GROCERY SUGGESTIONS
    private fun fetchGrocerySuggestions() {
        println("GroceryListViewModel before calling fetchGrocerySuggestionsUseCase")
        viewModelScope.launch {
            fetchGrocerySuggestionsUseCase().collect { result ->
                println("GroceryListViewModel fetchGrocerySuggestionsUseCase result: $result")
                when (result) {
                    is GrocerySuggestionsListener.Success -> {
                        println("GroceryListViewModel categories updated: ${result.listItems}")
                        updateUiState { state ->
                            state.copy(
                                allGrocerySuggestions = result.listItems
                                    .sortedBy { it.suggestionName }
                            )
                        }
                    }

                    is GrocerySuggestionsListener.Failure -> {
                        updateUiState { state ->
                            state.copy(allGrocerySuggestions = emptyList())
                        }
                        showError(result.message)
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
            val result: StringResultListener = saveItemUseCase.invoke(groceryItem, Constants.GROCERY_TABLE)
            if (result is StringResultListener.Success) {
                updateUiState { currentState ->
                    currentState.copy(
                        newItemCategory = UNCATEGORIZED_CATEGORY,
                        newItemText = "",
                        newItemPrice = "",
                    )
                }
            } else if (result is StringResultListener.Failure) {
                println("Error: ${result.message}")
                showError(result.message)
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
            val result: ResultListener = toggleCompletableItemCompletionUseCase.invoke(newItem, Constants.GROCERY_TABLE)
            if (result is ResultListener.Success) {
                updateUiState { state ->
                    state.copy(isLoading = false)
                }
            } else if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                showError(result.message)
                updateUiState { state ->
                    state.copy(isLoading = false)
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
            deleteCompletedItemsUseCase.invoke(
                Constants.GROCERY_TABLE,
                idsList = idsToDelete,
            )
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
