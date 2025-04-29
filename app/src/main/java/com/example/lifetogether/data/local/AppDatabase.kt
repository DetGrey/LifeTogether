package com.example.lifetogether.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lifetogether.data.local.dao.AlbumsDao
import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.FamilyInformationDao
import com.example.lifetogether.data.local.dao.GalleryImagesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.model.ListCountEntity
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.data.model.GalleryImageEntity

@Database(
    entities = [
        GroceryListEntity::class,
        GrocerySuggestionEntity::class,
        ListCountEntity::class,
        CategoryEntity::class,
        UserEntity::class,
        FamilyEntity::class,
        FamilyMemberEntity::class,
        RecipeEntity::class,
        AlbumEntity::class,
        GalleryImageEntity::class,
    ],
    version = 15,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groceryListDao(): GroceryListDao
    abstract fun grocerySuggestionsDao(): GrocerySuggestionsDao
    abstract fun userInformationDao(): UserInformationDao
    abstract fun familyInformationDao(): FamilyInformationDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun recipesDao(): RecipesDao
    abstract fun albumsDao(): AlbumsDao
    abstract fun galleryImagesDao(): GalleryImagesDao
    // Add other DAOs here
}
