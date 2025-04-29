package com.example.lifetogether.data.model

sealed class Entity {
    data class GroceryList(val entity: GroceryListEntity) : Entity()
    data class Recipe(val entity: RecipeEntity) : Entity()
    data class Album(val entity: AlbumEntity) : Entity()
    data class GalleryImage(val entity: GalleryImageEntity) : Entity()
}
