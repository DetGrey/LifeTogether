package com.example.lifetogether.ui.feature.home

import android.graphics.Bitmap
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.UserInformation

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Unauthenticated(
        val content: HomeContent,
    ) : HomeUiState

    data class Authenticated(
        val userInformation: UserInformation,
        val content: HomeContent,
    ) : HomeUiState
}

data class HomeContent(
    val statusCard: HomeStatusCard = HomeStatusCard.None,
    val bitmap: Bitmap? = null,
    val sections: List<HomeSection> = emptyList(),
)

sealed interface HomeStatusCard {
    data object None : HomeStatusCard

    data class Message(
        val text: String,
    ) : HomeStatusCard
}

data class HomeSection(
    val title: String? = null,
    val maxItemsInEachRow: Int,
    val items: List<HomeSectionItem>,
)

sealed interface HomeSectionItem {
    data class Tile(val tile: HomeTile) : HomeSectionItem
    data object Break : HomeSectionItem
}

sealed interface HomeTile {
    val title: String
    val icon: Icon
    val requiresFamilyAccess: Boolean
    val requiresAdminAccess: Boolean

    data object GroceryList : HomeTile {
        override val title: String = "Grocery list"
        override val icon: Icon = Icon(com.example.lifetogether.R.drawable.ic_groceries, "groceries basket icon")
        override val requiresFamilyAccess: Boolean = true
        override val requiresAdminAccess: Boolean = false
    }

    data object Recipes : HomeTile {
        override val title: String = "Recipes"
        override val icon: Icon = Icon(com.example.lifetogether.R.drawable.ic_recipes, "recipes chef hat icon")
        override val requiresFamilyAccess: Boolean = true
        override val requiresAdminAccess: Boolean = false
    }

    data object Guides : HomeTile {
        override val title: String = "Guides"
        override val icon: Icon = Icon(com.example.lifetogether.R.drawable.ic_guide, "guides icon")
        override val requiresFamilyAccess: Boolean = true
        override val requiresAdminAccess: Boolean = false
    }

    data object Gallery : HomeTile {
        override val title: String = "Gallery"
        override val icon: Icon = Icon(com.example.lifetogether.R.drawable.ic_gallery, "image gallery icon")
        override val requiresFamilyAccess: Boolean = true
        override val requiresAdminAccess: Boolean = false
    }

    data object TipTracker : HomeTile {
        override val title: String = "Tip Tracker"
        override val icon: Icon = Icon(com.example.lifetogether.R.drawable.ic_tip, "money tip icon")
        override val requiresFamilyAccess: Boolean = true
        override val requiresAdminAccess: Boolean = false
    }

    data object Lists : HomeTile {
        override val title: String = "Lists"
        override val icon: Icon = Icon(com.example.lifetogether.R.drawable.ic_guide, "lists icon")
        override val requiresFamilyAccess: Boolean = true
        override val requiresAdminAccess: Boolean = false
    }

    data object AdminGroceryCategories : HomeTile {
        override val title: String = "Grocery categories"
        override val icon: Icon = Icon(com.example.lifetogether.R.drawable.ic_groceries, "groceries basket icon")
        override val requiresFamilyAccess: Boolean = true
        override val requiresAdminAccess: Boolean = true
    }

    data object AdminGrocerySuggestions : HomeTile {
        override val title: String = "Grocery suggestions"
        override val icon: Icon = Icon(com.example.lifetogether.R.drawable.ic_groceries, "groceries basket icon")
        override val requiresFamilyAccess: Boolean = true
        override val requiresAdminAccess: Boolean = true
    }
}
