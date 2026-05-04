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
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class GroceryListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val groceryRepository: GroceryRepository,
    private val sendNotificationUseCase: SendNotificationUseCase,
) : ViewModel() {
    private val _interactionState = MutableStateFlow(
        GroceryListUiState.Content(
            groceryList = emptyList(),
            completedItems = emptyList(),
            categorizedItems = emptyMap(),
            groceryCategories = emptyList(),
            categoryExpandedStates = emptyMap(),
            expectedTotalPrice = null,
            allGrocerySuggestions = emptyList(),
            currentGrocerySuggestions = emptyList(),
        ),
    )

    val uiState: StateFlow<GroceryListUiState> = combine(
        groceryItemsState(),
        categoriesState(),
        suggestionsState(),
        _interactionState,
    ) { groceryItems, categories, suggestions, interactionState ->
        if (
            groceryItems is LoadableData.Loading ||
            categories is LoadableData.Loading ||
            suggestions is LoadableData.Loading
        ) {
            GroceryListUiState.Loading
        } else {
            val groceryList = (groceryItems as LoadableData.Ready<List<GroceryItem>>).value
            val groceryCategories = (categories as LoadableData.Ready<List<Category>>).value
            val allSuggestions = (suggestions as LoadableData.Ready<List<GrocerySuggestion>>).value

            enrichDerivedState(
                ensureExpandedStates(
                    interactionState.copy(
                        groceryList = groceryList,
                        groceryCategories = groceryCategories,
                        allGrocerySuggestions = allSuggestions,
                    ),
                ),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroceryListUiState.Loading,
    )

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun groceryItemsState(): Flow<LoadableData<List<GroceryItem>>> {
        return sessionRepository.sessionState
            .map { state ->
                (state as? SessionState.Authenticated)?.user?.familyId
            }
            .distinctUntilChanged()
            .flatMapLatest { familyId ->
                if (familyId.isNullOrBlank()) {
                    flowOf<LoadableData<List<GroceryItem>>>(LoadableData.Loading)
                } else {
                    flow<LoadableData<List<GroceryItem>>> {
                        emit(LoadableData.Loading)
                        groceryRepository.observeGroceryItems(familyId).collect { result ->
                            emit(
                                when (result) {
                                    is Result.Success -> LoadableData.Ready(result.data)
                                    is Result.Failure -> {
                                        showError(result.error.toUserMessage())
                                        LoadableData.Ready(emptyList())
                                    }
                                },
                            )
                        }
                    }
                }
            }
    }

    private fun categoriesState(): Flow<LoadableData<List<Category>>> {
        return flow {
            emit(LoadableData.Loading)
            groceryRepository.observeCategories().collect { result ->
                emit(
                    when (result) {
                        is Result.Success -> {
                            val categories = result.data
                                .filterNot { it.name == UNCATEGORIZED_CATEGORY_NAME }
                                .sortedBy { it.name }
                                .let { listOf(UNCATEGORIZED_CATEGORY) + it }
                            LoadableData.Ready(categories)
                        }

                        is Result.Failure -> {
                            showError(result.error.toUserMessage())
                            LoadableData.Ready(emptyList())
                        }
                    },
                )
            }
        }
    }

    private fun suggestionsState(): Flow<LoadableData<List<GrocerySuggestion>>> {
        return flow {
            emit(LoadableData.Loading)
            groceryRepository.observeGrocerySuggestions().collect { result ->
                emit(
                    when (result) {
                        is Result.Success -> LoadableData.Ready(result.data.sortedBy { it.suggestionName })
                        is Result.Failure -> {
                            showError(result.error.toUserMessage())
                            LoadableData.Ready(emptyList())
                        }
                    },
                )
            }
        }
    }

    private fun toggleCategoryExpanded(categoryName: String) {
        updateContentState { state ->
            val currentStates = state.categoryExpandedStates.toMutableMap()
            val currentState = currentStates[categoryName] ?: true
            currentStates[categoryName] = !currentState
            state.copy(categoryExpandedStates = currentStates)
        }
    }

    private fun toggleCompletedSectionExpanded() {
        updateContentState { state ->
            state.copy(completedSectionExpanded = !state.completedSectionExpanded)
        }
    }

    private fun showDeleteCompletedConfirmation() {
        updateContentState { state ->
            state.copy(showConfirmationDialog = true)
        }
    }

    private fun dismissDeleteCompletedConfirmation() {
        updateContentState { state ->
            state.copy(showConfirmationDialog = false)
        }
    }

    private fun onNewItemTextChange(value: String) {
        updateContentState { state ->
            state.copy(newItemText = value)
        }
    }

    private fun onNewItemPriceChange(value: String) {
        updateContentState { state ->
            state.copy(newItemPrice = value)
        }
    }

    private fun updateNewItemCategory(category: Category?) {
        updateContentState { state ->
            state.copy(newItemCategory = category ?: UNCATEGORIZED_CATEGORY)
        }
    }

    private fun applySuggestion(suggestion: GrocerySuggestion) {
        updateContentState { state ->
            state.copy(
                newItemText = suggestion.suggestionName,
                newItemCategory = suggestion.category ?: UNCATEGORIZED_CATEGORY,
                newItemPrice = suggestion.approxPrice?.toString() ?: "",
            )
        }
    }

    private fun addItemToList() {
        val state = currentContentState()

        if (state.newItemText.isEmpty()) {
            showError("Please write some text first")
            return
        }

        val price = state.newItemPrice.toFloatOrNull()
        val familyId = currentFamilyId()

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
                    updateContentState { currentState ->
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

    private fun toggleItemCompleted(oldItem: GroceryItem) {
        val newItem = oldItem.copy(
            completed = !oldItem.completed,
            lastUpdated = Date(System.currentTimeMillis()),
        )

        viewModelScope.launch {
            when (val result = groceryRepository.toggleGroceryItemBought(newItem)) {
                is Result.Success -> Unit
                is Result.Failure -> showError(result.error.toUserMessage())
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

    private fun deleteCompletedItems() {
        val completedItems = currentContentState().completedItems
        if (completedItems.isEmpty()) {
            dismissDeleteCompletedConfirmation()
            return
        }
        val idsToDelete = completedItems.map { it.id }

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

    private fun updateContentState(transform: (GroceryListUiState.Content) -> GroceryListUiState.Content) {
        _interactionState.update(transform)
    }

    private fun currentContentState(): GroceryListUiState.Content {
        return _interactionState.value
    }

    private fun currentFamilyId(): String? {
        return (sessionRepository.sessionState.value as? SessionState.Authenticated)?.user?.familyId
    }

    private fun ensureExpandedStates(state: GroceryListUiState.Content): GroceryListUiState.Content {
        val currentStates = state.categoryExpandedStates.toMutableMap()
        state.groceryCategories.forEach { category ->
            currentStates.putIfAbsent(category.name.trim(), true)
        }
        return state.copy(categoryExpandedStates = currentStates)
    }

    private fun enrichDerivedState(state: GroceryListUiState.Content): GroceryListUiState.Content {
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

    private fun updateCategorizedItems(list: List<GroceryItem>): Map<Category, List<GroceryItem>> {
        val categorizedMap = list
            .filter { !it.completed }
            .groupBy { item ->
                item.category?.takeIf { it.name != UNCATEGORIZED_CATEGORY_NAME } ?: UNCATEGORIZED_CATEGORY
            }
            .mapValues { entry ->
                entry.value.sortedBy { it.itemName }
            }
            .toSortedMap(compareBy { it.name })
        return categorizedMap
    }

    private sealed interface LoadableData<out T> {
        data object Loading : LoadableData<Nothing>
        data class Ready<T>(val value: T) : LoadableData<T>
    }
}
