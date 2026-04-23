package com.example.lifetogether.ui.navigation

sealed interface NotificationDestination {
    val key: String
    fun toAppRoute(): AppRoute

    data object Grocery : NotificationDestination {
        override val key = "grocery"
        override fun toAppRoute() = GroceryListNavRoute
    }

    companion object {
        fun fromKey(key: String): NotificationDestination? = when (key) {
            Grocery.key -> Grocery
            else -> null
        }
    }
}
