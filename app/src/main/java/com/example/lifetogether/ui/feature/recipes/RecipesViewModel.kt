package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.GrocerySuggestionsListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.GrocerySuggestion
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.usecase.item.DeleteCompletedItemsUseCase
import com.example.lifetogether.domain.usecase.item.FetchCategoriesUseCase
import com.example.lifetogether.domain.usecase.item.FetchGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.ToggleCompletableItemCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val toggleCompletableItemCompletionUseCase: ToggleCompletableItemCompletionUseCase,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val deleteCompletedItemsUseCase: DeleteCompletedItemsUseCase,
    private val fetchGrocerySuggestionsUseCase: FetchGrocerySuggestionsUseCase,
) : ViewModel() {
    var showConfirmationDialog: Boolean by mutableStateOf(false)

    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    // ---------------------------------------------------------------- UID
    private var familyIdIsSet = false
    var familyId: String? = null

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    fun setUpRecipes(addedFamilyId: String) {
        fetchCategories()
        fetchGrocerySuggestions()

        if (!familyIdIsSet) {
            println("GroceryListViewModel setting UID")
            familyId = addedFamilyId
            // Use the UID here (e.g., fetch grocery list items)
            viewModelScope.launch {
                fetchListItemsUseCase(familyId!!, "grocery-list", GroceryItem::class).collect { result ->
                    println("fetchListItemsUseCase result: $result")
                    when (result) {
                        is ListItemsResultListener.Success -> {
                            // Filter and map the result.listItems to only include GroceryItem instances
                            val foundRecipes = result.listItems.filterIsInstance<Recipe>()
                            if (foundRecipes.isNotEmpty()) {
                                println("_groceryList old value: ${_recipes.value}")
                                _recipes.value = foundRecipes
                                println("groceryList new value: ${this@RecipesViewModel.recipes.value}")

                                updateExpandedStates()
                            } else {
                                println("Error: No GroceryItem instances found in the result")
                                error = "No GroceryItem instances found in the result"
                                showAlertDialog = true
                            }
                        }

                        is ListItemsResultListener.Failure -> {
                            // Handle failure, e.g., show an error message
                            println("Error: ${result.message}")
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

    private fun updateCategorizedItems(list: List<GroceryItem>): Map<Category, List<GroceryItem>> {
        println("GroceryListViewModel updateCategorizedItems() initial list: $list")

        // Logic to categorize items and post value to _categorizedItems
        val categorizedMap = list
            .filter { !it.completed }
            .groupBy { item ->
                item.category?.takeIf { it.name != "Uncategorized" } ?: uncategorizedCategory
            }
            .mapValues { entry ->
                entry.value.sortedBy { it.itemName } // Sorting items by name
            }
            .toSortedMap(compareBy { it.name }) // Sorting categories by name
        println("GroceryListViewModel updateCategorizedItems() categorizedMap: $categorizedMap")
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

    // ---------------------------------------------------------------- GROCERY SUGGESTIONS
    private val _grocerySuggestions = MutableStateFlow<List<GrocerySuggestion>>(emptyList())
    private val grocerySuggestions: StateFlow<List<GrocerySuggestion>> = _grocerySuggestions.asStateFlow()

    private fun fetchGrocerySuggestions() {
        println("GroceryListViewModel before calling fetchGrocerySuggestionsUseCase")
        viewModelScope.launch {
            fetchGrocerySuggestionsUseCase().collect { result ->
                println("GroceryListViewModel fetchGrocerySuggestionsUseCase result: $result")
                when (result) {
                    is GrocerySuggestionsListener.Success -> {
                        println("GroceryListViewModel categories updated: ${result.listItems}")
                        _grocerySuggestions.value = result.listItems
                            .sortedBy { it.suggestionName }
                    }

                    is GrocerySuggestionsListener.Failure -> {
                        _grocerySuggestions.value = emptyList()
                        // Handle failure, e.g., show an error message
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    val currentGrocerySuggestions = derivedStateOf {
        if (newItemText.isNotEmpty()) {
            grocerySuggestions.value.filter { suggestion ->
                suggestion.suggestionName.startsWith(newItemText, ignoreCase = true)
            }.take(5)
        } else {
            emptyList()
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
            currentStates.putIfAbsent(category.name.trim(), true)
        }
        _categoryExpandedStates.value = currentStates
//        println("GroceryListViewModel updateExpandedStates()  categories: ${groceryCategories.value}")
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
        newItemCategory = category ?: uncategorizedCategory
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
                error = result.message
                showAlertDialog = true
            }
        }
    }

    // ---------------------------------------------------------------- TOGGLE ITEM COMPLETION
    fun toggleItemCompleted(
        oldItem: GroceryItem,
    ) {
        val newItem = oldItem.copy(completed = !oldItem.completed, lastUpdated = Date(System.currentTimeMillis()))

        viewModelScope.launch {
            val result: ResultListener = toggleCompletableItemCompletionUseCase.invoke(newItem, "grocery-list")
            if (result is ResultListener.Success) {
//                groceryList = groceryList.minus(oldItem).plus(newItem)
            } else if (result is ResultListener.Failure) {
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }
        }
    }
}
