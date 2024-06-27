package com.example.lifetogether.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.ListCountEntity
import com.example.lifetogether.data.model.UserEntity

@Database(
    entities = [GroceryListEntity::class, ListCountEntity::class, CategoryEntity::class, UserEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groceryListDao(): GroceryListDao
    abstract fun userInformationDao(): UserInformationDao
    abstract fun categoriesDao(): CategoriesDao
    // Add other DAOs here
}
