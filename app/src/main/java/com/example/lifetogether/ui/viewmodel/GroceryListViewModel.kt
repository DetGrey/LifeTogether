package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.ToggleItemCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {
    var isLoading = true // TODO might need to change to false!!! or mutablestate

    // ---------------------------------------------------------------- UID
    private var familyIdIsSet = false
    var familyId: String? = null

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    private val _groceryList = MutableStateFlow<List<GroceryItem>>(emptyList())
    val groceryList: StateFlow<List<GroceryItem>> = _groceryList.asStateFlow()

    fun setUpGroceryList(addedFamilyId: String) {
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

                            result.listItems.forEach { item ->
                                updateCategories(item.category ?: uncategorizedCategory)
                            }
                            updateExpandedStates()
                        }

                        is ListItemsResultListener.Failure -> {
                            // Handle failure, e.g., show an error message
                        }
                    }
                }
            }
            familyIdIsSet = true
        }
    }

    // ---------------------------------------------------------------- CATEGORY LISTS
    val completedItems: StateFlow<List<GroceryItem>>
        get() = groceryList
            .map { list ->
                list.filter { it.completed }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // StateFlow for categorized items (excluding completed items)
    val categorizedItems: StateFlow<Map<Category, List<GroceryItem>>>
        get() = groceryList
            .map { list ->
                updateCategorizedItems(list)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    private fun updateCategories(newCategory: Category) {
        if (!groceryCategories.value.contains(newCategory)) {
            println("adding new category: $newCategory")
            _groceryCategories.value += newCategory
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
//        if (groceryList.any { it.itemName.lowercase() == newItemText.lowercase() && !it.completed }) {
//            // TODO add error popup
//            return
//        }

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
            return
        }

        viewModelScope.launch {
            val result: ResultListener = saveItemUseCase.invoke(groceryItem, "grocery-list")
            if (result is ResultListener.Success) {
//                groceryList = groceryList.plus(groceryItem)
                updateCategories(newItemCategory)
                updateNewItemCategory(null)
                newItemText = ""
                onSuccess()
            } else if (result is ResultListener.Failure) {
                // TODO popup saying the error for 5 sec
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
                isLoading = false
            }
        }
    }

    // PRIVATE FUNCTIONS
//    private suspend fun fetchDefaults(
//        onSuccess: () -> Unit,
//    ) {
//        val result: DefaultsResultListener = fetchListDefaultsUseCase.invoke("grocery-list")
//        if (result is DefaultsResultListener.Success) {
// //            groceryCategories = hashmapListToCategoryList(result.documentSnapshot)
//            onSuccess()
//        } else if (result is DefaultsResultListener.Failure) {
//            // TODO popup saying the error for 5 sec
//        }
//    }

//    private fun hashmapListToCategoryList(
//        documentSnapshot: DocumentSnapshot,
//    ): List<Category> {
//        val categoriesMapList = documentSnapshot.data?.get("categories") as? List<*>
//        return categoriesMapList?.mapNotNull { item ->
//            if (item is Map<*, *>) {
//                val emoji = item["emoji"]
//                val name = item["name"]
//                if (emoji is String && name is String) {
//                    Category(emoji, name)
//                } else {
//                    null
//                }
//            } else {
//                null
//            }
//        } ?: listOf()
//    }
}
