package com.example.lifetogether.data.model

sealed class Entity {
    data class GroceryList(val entity: GroceryListEntity) : Entity()
    data class Recipe(val entity: RecipeEntity) : Entity()
}
