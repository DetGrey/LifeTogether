package com.example.lifetogether.ui.navigation

interface Navigator {
    fun navigate(route: AppRoute)
    fun navigateBack()
}
