package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.DefaultsResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.usecase.item.FetchListDefaultsUseCase
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.ToggleItemCompletionUseCase
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class GroceryListViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val toggleItemCompletionUseCase: ToggleItemCompletionUseCase,
    private val fetchListDefaultsUseCase: FetchListDefaultsUseCase,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
) : ViewModel() {
    var isLoading = true

    // TODO
    var groceryCategories: List<Category> by mutableStateOf(
        listOf(
//            Category(
//                emoji = "❓️",
//                name = "Uncategorized",
//            ),
        ),
    )

    // Must be "=" and not "by" else the app with crash
    var categoryExpandedStates: MutableMap<String, MutableState<Boolean>> = mutableMapOf()
    var completedSectionExpanded: Boolean by mutableStateOf(false)

    // TODO
    var groceryList: List<GroceryItem> by mutableStateOf(
        listOf(),
    )

    var newItemText: String by mutableStateOf("")
    var newItemCategory: Category by mutableStateOf(
        Category(
            emoji = "❓️",
            name = "Uncategorized",
        ),
    )

    fun updateNewItemCategory(category: Category?) {
        newItemCategory = category
            ?: Category(
                emoji = "❓️",
                name = "Uncategorized",
            )
        println("New category: $newItemCategory")
    }

    fun getCategoryItems(
        category: Category,
    ): List<GroceryItem> {
        return groceryList.filter { item ->
            if (category.name == "Uncategorized") {
                !item.completed && item.category == null
            } else {
                !item.completed && item.category == category
            }
        }
    }

    fun getCompletedItems(): List<GroceryItem> {
        return groceryList.filter { item -> item.completed }
    }

    private fun updateExpandedStates() {
        // Ensure each category has an expanded state entry
        groceryCategories.forEach { category ->
            if (!categoryExpandedStates.containsKey(category.name)) {
                categoryExpandedStates[category.name] = mutableStateOf(true)
            }
        }

        println("categoryExpandedStates: $categoryExpandedStates")
    }

    fun toggleCategoryExpanded(categoryName: String) {
        categoryExpandedStates[categoryName]?.let { expanded ->
            expanded.value = !expanded.value
        }
    }

    // USE CASES
    fun fetchData(uid: String) {
        viewModelScope.launch {
            val defaultsDeferred = async {
                fetchDefaults(onSuccess = {
                    updateExpandedStates()
                })
            }
            val itemsDeferred = async {
                fetchListItems(uid, onSuccess = {
                    groceryList.forEach { item ->
                        item.category?.let { updateCategories(it) }
                    }
                })
            }
            // Wait for both coroutines to complete
            defaultsDeferred.await()
            itemsDeferred.await()

            // Now that both coroutines are done, set isLoading to false
            isLoading = false
        }
    }

    fun addItemToList(
        uid: String,
        onSuccess: () -> Unit,
    ) {
        if (groceryList.any { it.itemName.lowercase() == newItemText.lowercase() && !it.completed }) {
            // TODO add error popup
            return
        }
        viewModelScope.launch {
            val groceryItem = GroceryItem(
                uid = uid,
                category = newItemCategory,
                itemName = newItemText,
                lastUpdated = Date(System.currentTimeMillis()),
                completed = false,
            )
            val result: ResultListener = saveItemUseCase.invoke(groceryItem, "grocery-list")
            if (result is ResultListener.Success) {
                groceryList = groceryList.plus(groceryItem)
                updateCategories(newItemCategory)
                updateNewItemCategory(null)
                newItemText = ""
                onSuccess()
            } else if (result is ResultListener.Failure) {
                // TODO popup saying the error for 5 sec
            }
        }
    }

    fun toggleItemCompleted(
        oldItem: GroceryItem,
    ) {
        isLoading = true
        val newItem = oldItem.copy(completed = !oldItem.completed, lastUpdated = Date(System.currentTimeMillis()))

        viewModelScope.launch {
            val result: ResultListener = toggleItemCompletionUseCase.invoke(newItem, "grocery-list")
            if (result is ResultListener.Success) {
                groceryList = groceryList.minus(oldItem).plus(newItem)
                isLoading = false
            } else if (result is ResultListener.Failure) {
                // TODO popup saying the error for 5 sec
                isLoading = false
            }
        }
    }

    // PRIVATE FUNCTIONS
    private fun updateCategories(
        newCategory: Category,
    ) {
        if (!groceryCategories.contains(newCategory)) {
            println("adding new category: $newCategory")
            groceryCategories = groceryCategories.plus(newCategory)
            updateExpandedStates()
        }
    }

    private suspend fun fetchDefaults(
        onSuccess: () -> Unit,
    ) {
        val result: DefaultsResultListener = fetchListDefaultsUseCase.invoke("grocery-list")
        if (result is DefaultsResultListener.Success) {
            groceryCategories = hashmapListToCategoryList(result.documentSnapshot)
            onSuccess()
        } else if (result is DefaultsResultListener.Failure) {
            // TODO popup saying the error for 5 sec
        }
    }

    private fun hashmapListToCategoryList(
        documentSnapshot: DocumentSnapshot,
    ): List<Category> {
        val categoriesMapList = documentSnapshot.data?.get("categories") as? List<*>
        return categoriesMapList?.mapNotNull { item ->
            if (item is Map<*, *>) {
                val emoji = item["emoji"]
                val name = item["name"]
                if (emoji is String && name is String) {
                    Category(emoji, name)
                } else {
                    null
                }
            } else {
                null
            }
        } ?: listOf()
    }

    private suspend fun fetchListItems(
        uid: String,
        onSuccess: () -> Unit,
    ) {
        val result: ListItemsResultListener<GroceryItem> = fetchListItemsUseCase.invoke(uid, "grocery-list", GroceryItem::class)
        if (result is ListItemsResultListener.Success) {
            groceryList = result.listItems
            onSuccess()
        } else if (result is ListItemsResultListener.Failure) {
            // TODO popup saying the error for 5 sec
        }
    }
}
