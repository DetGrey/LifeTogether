package com.example.lifetogether.data.model

sealed class Entity {
    data class GroceryList(val entity: GroceryListEntity) : Entity()
    data class Recipe(val entity: RecipeEntity) : Entity()
    data class Album(val entity: AlbumEntity) : Entity()
    data class GalleryMedia(val entity: GalleryMediaEntity) : Entity()
    data class Tip(val entity: TipEntity) : Entity()
    data class Guide(val entity: GuideEntity) : Entity()
    data class UserList(val entity: UserListEntity) : Entity()
    data class RoutineListEntry(val entity: RoutineListEntryEntity) : Entity()
}
