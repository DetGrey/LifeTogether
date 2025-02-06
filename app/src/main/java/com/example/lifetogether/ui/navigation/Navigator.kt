package com.example.lifetogether.ui.navigation

interface Navigator {
    fun navigateToAdmin()
    fun navigateToAdminGroceryCategories()
    fun navigateToAdminGrocerySuggestions()
    fun navigateToHome()
    fun navigateToProfile()
    fun navigateToFamily()
    fun navigateToSettings()
    fun navigateToLogin()
    fun navigateToSignUp()
    fun navigateBack()
    fun navigateToGroceryList()
    fun navigateToRecipes()
    fun navigateToRecipeDetails(recipeId: String? = null)
}
