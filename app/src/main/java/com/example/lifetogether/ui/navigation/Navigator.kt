package com.example.lifetogether.ui.navigation

interface Navigator {
    fun navigateToAdmin()
    fun navigateToAdminGroceryCategories()
    fun navigateToAdminGrocerySuggestions()
    fun navigateToHome()
    fun navigateToProfile()
    fun navigateToSettings()
    fun navigateToGroceryList()
    fun navigateToLogin()
    fun navigateToSignUp()
    fun navigateBack()
    // ... other navigation methods
}
