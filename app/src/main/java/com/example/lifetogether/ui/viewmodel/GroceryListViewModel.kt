package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.usecase.SaveItemUseCase
import kotlinx.coroutines.launch
import java.util.Date

class GroceryListViewModel : ViewModel() {
    // TODO
    var groceryCategories: List<Category> by mutableStateOf(
        listOf(
            Category(
                emoji = "✔️",
                name = "Uncategorized",
            ),
            Category( // TODO remove later
                "🍎",
                "Fruits and vegetables",
            ),
        ),
    )

    // Must be "=" and not "by" else the app with crash
    var categoryExpandedStates: MutableMap<String, MutableState<Boolean>> = mutableMapOf("Uncategorized" to mutableStateOf(true))
    var completedSectionExpanded: Boolean by mutableStateOf(false)

    // TODO
    var groceryList: List<GroceryItem> by mutableStateOf(
        listOf(
            GroceryItem(
                uid = "dsuaihfao",
                username = "Ane",
                category = Category(
                    "🍎",
                    "Fruits and vegetables",
                ),
                itemName = "Bananas",
                lastUpdated = Date(System.currentTimeMillis()),
                completed = false,
            ),
            GroceryItem(
                uid = "dsuaihfao",
                username = "Ane",
                category = Category(
                    "🍎",
                    "Fruits and vegetables",
                ),
                itemName = "Potatoes",
                lastUpdated = Date(System.currentTimeMillis()),
                completed = false,
            ),
            GroceryItem(
                uid = "dsuaihfao",
                username = "Ane",
                category = null,
                itemName = "Pineapple",
                lastUpdated = Date(System.currentTimeMillis()),
                completed = false,
            ),
            GroceryItem(
                uid = "dsuaihfao",
                username = "Ane",
                category = null,
                itemName = "Garlic",
                lastUpdated = Date(System.currentTimeMillis()),
                completed = true,
            ),
        ),
    )

    var newItemText: String by mutableStateOf("")
    var newItemCategory: Category? by mutableStateOf(null)

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

    fun addItemToList(
        uid: String,
        username: String,
    ) {
        viewModelScope.launch {
            val groceryItem = GroceryItem(
                uid = uid,
                username = username,
                category = newItemCategory,
                itemName = newItemText,
                lastUpdated = Date(System.currentTimeMillis()),
                completed = false,
            )

            val saveItemUseCase = SaveItemUseCase()
            val result = saveItemUseCase.invoke(groceryItem)
            if (result is ResultListener.Success) {
                groceryList = groceryList.plus(groceryItem)
                if (!groceryCategories.contains(newItemCategory)) {
                    updateCategories()
                }
                newItemCategory = null
                newItemText = ""
            } else if (result is ResultListener.Failure) {
                // TODO popup saying the error for 5 sec
            }
        }
    }

    fun updateCategories() {
        // Assuming newItemCategory is not null when this function is called
        val newCategory = newItemCategory ?: return

        // Update the list of categories
        groceryCategories = groceryCategories.plus(newCategory).sortedBy { if (it.name == "Uncategorized") 1 else 0 }

        updateExpandedStates()
    }

    fun updateExpandedStates() {
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
}
