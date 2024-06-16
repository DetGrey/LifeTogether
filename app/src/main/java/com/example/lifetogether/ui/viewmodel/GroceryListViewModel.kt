package com.example.lifetogether.ui.viewmodel

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
    var groceryCategories: List<Category>? by mutableStateOf(
        listOf(
            Category(
                "🍎",
                "Fruits and vegetables",
            ),
        ),
    )

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
                checked = false,
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
                checked = false,
            ),
        ),
    )

    var newItemText: String by mutableStateOf("")
    var newItemCategory: Category? by mutableStateOf(null)

    fun getCategoryItems(
        category: Category,
    ): List<GroceryItem> {
        return groceryList.filter { item ->
            !item.checked && item.category == category
        }
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
                checked = false,
            )

            val saveItemUseCase = SaveItemUseCase()
            val result = saveItemUseCase.invoke(groceryItem)
            if (result is ResultListener.Success) {
                groceryList = groceryList.plus(groceryItem)
                if (groceryCategories?.contains(newItemCategory) == false) {
                    updateCategories()
                }
                newItemCategory = null
                newItemText = ""
            } else if (result is ResultListener.Failure) {
                // TODO popup saying the error for 5 sec
            }
        }
    }

    fun deleteItem(item: GroceryItem) {
    }

    private fun updateCategories() {
        groceryCategories = if (groceryCategories != null) {
            groceryCategories!!.plus(newItemCategory as Category)
        } else {
            listOf(newItemCategory as Category)
        }
    }
}
