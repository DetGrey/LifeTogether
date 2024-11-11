package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.usecase.item.DeleteCompletedItemsUseCase
import com.example.lifetogether.domain.usecase.item.FetchCategoriesUseCase
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.ToggleItemCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class GroceryListViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val toggleItemCompletionUseCase: ToggleItemCompletionUseCase,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val deleteCompletedItemsUseCase: DeleteCompletedItemsUseCase,
) : ViewModel() {
    var showConfirmationDialog: Boolean by mutableStateOf(false)

    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(5000)
            showAlertDialog = false
            error = ""
        }
    }

    var isLoading = true // TODO might need to change to false!!! or mutablestate

    // ---------------------------------------------------------------- UID
    private var familyIdIsSet = false
    var familyId: String? = null

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    private val _groceryList = MutableStateFlow<List<GroceryItem>>(emptyList())
    val groceryList: StateFlow<List<GroceryItem>> = _groceryList.asStateFlow()

    fun setUpGroceryList(addedFamilyId: String) {
        fetchCategories()

        if (!familyIdIsSet) {
            println("GroceryListViewModel setting UID")
            familyId = addedFamilyId
            // Use the UID here (e.g., fetch grocery list items)
            viewModelScope.launch {
                fetchListItemsUseCase(familyId!!, "grocery-list", GroceryItem::class).collect { result ->
                    println("fetchListItemsUseCase result: $result")
                    when (result) {
                        is ListItemsResultListener.Success -> {
                            println("_groceryList old value: ${_groceryList.value}")
                            _groceryList.value = result.listItems
                            println("groceryList new value: ${groceryList.value}")

//                            result.listItems.forEach { item ->
//                                updateCategories(item.category ?: uncategorizedCategory)
//                            }
                            updateExpandedStates()
                        }

                        is ListItemsResultListener.Failure -> {
                            // Handle failure, e.g., show an error message
                            error = result.message
                            showAlertDialog = true
                        }
                    }
                }
            }
            familyIdIsSet = true
        }
    }

    // ---------------------------------------------------------------- CATEGORIZED LISTS
    val completedItems: StateFlow<List<GroceryItem>>
        get() = groceryList
            .map { list ->
                list.filter { it.completed }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // StateFlow for categorized items (excluding completed items)
    val categorizedItems: StateFlow<Map<Category, List<GroceryItem>>>
        get() = groceryList
            .map { list ->
                updateCategorizedItems(list)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private fun updateCategorizedItems(list: List<GroceryItem>): Map<Category, List<GroceryItem>> {
//        println("GroceryListViewModel updateCategorizedItems() initial list: $list")

        // Logic to categorize items and post value to _categorizedItems
        val categorizedMap = list
            .filter { !it.completed }
            .groupBy { item ->
                item.category?.takeIf { it.name != "Uncategorized" } ?: uncategorizedCategory
            }
//        println("GroceryListViewModel updateCategorizedItems() categorizedMap: $categorizedMap")
        return categorizedMap
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

    // ---------------------------------------------------------------- ADD NEW ITEM
    // USE CASES
    fun addItemToList(
        onSuccess: () -> Unit,
    ) {
        println("GroceryListViewModel addItemToList()")

        if (newItemText.isEmpty()) {
            error = "Please write some text first"
            showAlertDialog = true
            return
        }

        val groceryItem = familyId?.let {
            GroceryItem(
                familyId = it,
                category = newItemCategory,
                itemName = newItemText,
                lastUpdated = Date(System.currentTimeMillis()),
                completed = false,
            )
        }
        if (groceryItem == null) {
            error = "Please connect to a family first"
            showAlertDialog = true
            return
        }

        viewModelScope.launch {
            val result: ResultListener = saveItemUseCase.invoke(groceryItem, "grocery-list")
            if (result is ResultListener.Success) {
//                updateCategories(newItemCategory)
                updateNewItemCategory(null)
                newItemText = ""
                onSuccess()
            } else if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                // TODO popup saying the error for 5 sec
                error = result.message
                showAlertDialog = true
            }
        }
    }

    // ---------------------------------------------------------------- TOGGLE ITEM COMPLETION
    fun toggleItemCompleted(
        oldItem: GroceryItem,
    ) {
        isLoading = true
        val newItem = oldItem.copy(completed = !oldItem.completed, lastUpdated = Date(System.currentTimeMillis()))

        viewModelScope.launch {
            val result: ResultListener = toggleItemCompletionUseCase.invoke(newItem, "grocery-list")
            if (result is ResultListener.Success) {
//                groceryList = groceryList.minus(oldItem).plus(newItem)
                isLoading = false
            } else if (result is ResultListener.Failure) {
                // TODO popup saying the error for 5 sec
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
                isLoading = false
            }
        }
    }

    // ---------------------------------------------------------------- DELETE COMPLETED ITEMS
    fun deleteCompletedItems() {
        if (completedItems.value.isEmpty()) {
            return
        }
        val items = completedItems.value.filter { it.id != null }

        viewModelScope.launch {
            val result = deleteCompletedItemsUseCase.invoke(
                "grocery-list",
                items = items,
            )
            when (result) {
                is ResultListener.Success -> showConfirmationDialog = false
                is ResultListener.Failure -> showConfirmationDialog = false
            }
        }
    }
}
