package com.example.lifetogether.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.model.ListCountEntity
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.UserEntity

@Database(
    entities = [
        GroceryListEntity::class,
        GrocerySuggestionEntity::class,
        ListCountEntity::class,
        CategoryEntity::class,
        UserEntity::class,
        RecipeEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groceryListDao(): GroceryListDao
    abstract fun grocerySuggestionsDao(): GrocerySuggestionsDao
    abstract fun userInformationDao(): UserInformationDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun recipesDao(): RecipesDao
    // Add other DAOs here
}
