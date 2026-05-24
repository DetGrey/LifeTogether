package com.example.lifetogether.ui.navigation

sealed interface NotificationDestination {
    val key: String
    fun toRouteChain(): List<AppRoute>

    data object Grocery : NotificationDestination {
        override val key = "grocery"
        override fun toRouteChain() = listOf(GroceryListNavRoute)
    }

    data class MealPlan(val id: String) : NotificationDestination {
        override val key = "$MEAL_PLAN_PREFIX$id"
        override fun toRouteChain() = listOf(
            MealPlannerNavRoute,
            MealPlanDetailsNavRoute(mealPlanId = id)
        )
    }

    companion object {
        private const val MEAL_PLAN_PREFIX = "meal_plan:"

        fun fromKey(key: String): NotificationDestination? = when {
            key == Grocery.key -> Grocery
            key.startsWith(MEAL_PLAN_PREFIX) -> MealPlan(key.removePrefix(MEAL_PLAN_PREFIX))
            else -> null
        }
    }
}
