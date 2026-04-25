package com.example.lifetogether.ui.feature.groceryList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.groceryListNotificationOptions
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.notification.SendNotificationUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.navigation.NotificationDestination
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

private const val UNCATEGORIZED_NAME = "Uncategorized"
private val UNCATEGORIZED_CATEGORY = Category(
    emoji = "❓️",
    name = UNCATEGORIZED_NAME,
)

data class GroceryListUiState(
    val showConfirmationDialog: Boolean = false,
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
    private val groceryRepository: GroceryRepository,
    private val sendNotificationUseCase: SendNotificationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GroceryListUiState())
    val uiState: StateFlow<GroceryListUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

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

    fun onEvent(event: GroceryListUiEvent) {
        when (event) {
            is GroceryListUiEvent.CategoryExpandedClicked -> toggleCategoryExpanded(event.categoryName)
            GroceryListUiEvent.CompletedSectionExpandedClicked -> toggleCompletedSectionExpanded()
            is GroceryListUiEvent.ItemCompletedToggled -> toggleItemCompleted(event.item)
            is GroceryListUiEvent.NotificationClicked -> sendGroceryNotification(event.item)
            GroceryListUiEvent.DeleteCompletedClicked -> showDeleteCompletedConfirmation()
            GroceryListUiEvent.DismissDeleteCompletedConfirmation -> dismissDeleteCompletedConfirmation()
            GroceryListUiEvent.ConfirmDeleteCompletedConfirmation -> deleteCompletedItems()
            is GroceryListUiEvent.NewItemTextChanged -> onNewItemTextChange(event.value)
            is GroceryListUiEvent.NewItemPriceChanged -> onNewItemPriceChange(event.value)
            is GroceryListUiEvent.NewItemCategoryChanged -> updateNewItemCategory(event.value)
            GroceryListUiEvent.AddItemClicked -> addItemToList()
            is GroceryListUiEvent.SuggestionClicked -> {
                applySuggestion(event.suggestion)
                addItemToList()
            }
        }
    }

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    private fun setUpGroceryList() {
        observeCategories()
        observeGrocerySuggestions()

        val familyId = familyId ?: return

        viewModelScope.launch {
            groceryRepository.observeGroceryItems(familyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val groceryItems = result.data
                        updateUiState { state ->
                            state.copy(
                                groceryList = groceryItems,
                                isLoading = false,
                            )
                        }
                    }

                    is Result.Failure -> {
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun updateCategorizedItems(list: List<GroceryItem>): Map<Category, List<GroceryItem>> {
        val categorizedMap = list
            .filter { !it.completed }
            .groupBy { item ->
                item.category?.takeIf { it.name != UNCATEGORIZED_NAME } ?: UNCATEGORIZED_CATEGORY
            }
            .mapValues { entry ->
                entry.value.sortedBy { it.itemName }
            }
            .toSortedMap(compareBy { it.name })
        return categorizedMap
    }

    // ---------------------------------------------------------------- CATEGORIES
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
    private fun observeGrocerySuggestions() {
        viewModelScope.launch {
            groceryRepository.observeGrocerySuggestions().collect { result ->
                when (result) {
                    is Result.Success -> {
                        updateUiState { state ->
                            state.copy(
                                allGrocerySuggestions = result.data.sortedBy { it.suggestionName },
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

    private fun toggleCategoryExpanded(categoryName: String) {
        updateUiState { state ->
            val currentStates = state.categoryExpandedStates.toMutableMap()
            val currentState = currentStates[categoryName] ?: true
            currentStates[categoryName] = !currentState
            state.copy(categoryExpandedStates = currentStates)
        }
    }

    private fun toggleCompletedSectionExpanded() {
        updateUiState { state ->
            state.copy(completedSectionExpanded = !state.completedSectionExpanded)
        }
    }

    private fun showDeleteCompletedConfirmation() {
        updateUiState { state ->
            state.copy(showConfirmationDialog = true)
        }
    }

    private fun dismissDeleteCompletedConfirmation() {
        updateUiState { state ->
            state.copy(showConfirmationDialog = false)
        }
    }

    // ---------------------------------------------------------------- NEW ITEM
    private fun onNewItemTextChange(value: String) {
        updateUiState { state ->
            state.copy(newItemText = value)
        }
    }

    private fun onNewItemPriceChange(value: String) {
        updateUiState { state ->
            state.copy(newItemPrice = value)
        }
    }

    private fun updateNewItemCategory(category: Category?) {
        updateUiState { state ->
            state.copy(newItemCategory = category ?: UNCATEGORIZED_CATEGORY)
        }
    }

    private fun applySuggestion(suggestion: GrocerySuggestion) {
        updateUiState { state ->
            state.copy(
                newItemText = suggestion.suggestionName,
                newItemCategory = suggestion.category ?: UNCATEGORIZED_CATEGORY,
                newItemPrice = suggestion.approxPrice?.toString() ?: "",
            )
        }
    }

    // ---------------------------------------------------------------- ADD NEW ITEM
    private fun addItemToList() {
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
                approxPrice = price,
            )
        }
        if (groceryItem == null) {
            showError("Please connect to a family first")
            return
        }

        viewModelScope.launch {
            when (val result = groceryRepository.saveGroceryItem(groceryItem)) {
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
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    // ---------------------------------------------------------------- TOGGLE ITEM COMPLETION
    private fun toggleItemCompleted(oldItem: GroceryItem) {
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
                    showError(result.error.toUserMessage())
                    updateUiState { state ->
                        state.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private fun sendGroceryNotification(item: GroceryItem) {
        val option = groceryListNotificationOptions(item.itemName, item.category?.emoji ?: "")

        viewModelScope.launch {
            sendNotificationUseCase(
                familyId = item.familyId,
                title = option.title,
                message = option.message,
                channelId = Constants.GROCERY_LIST_CHANNEL,
                destination = NotificationDestination.Grocery,
            )
        }
    }

    // ---------------------------------------------------------------- DELETE COMPLETED ITEMS
    private fun deleteCompletedItems() {
        val completedItems = _uiState.value.completedItems
        if (completedItems.isEmpty()) {
            dismissDeleteCompletedConfirmation()
            return
        }
        val idsToDelete = completedItems.mapNotNull { it.id }

        viewModelScope.launch {
            when (val result = groceryRepository.deleteGroceryItems(itemIds = idsToDelete)) {
                is Result.Success -> dismissDeleteCompletedConfirmation()
                is Result.Failure -> {
                    dismissDeleteCompletedConfirmation()
                    showError(result.error.toUserMessage())
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
            expectedTotalPrice = expectedTotalPrice,
        )
    }
}
