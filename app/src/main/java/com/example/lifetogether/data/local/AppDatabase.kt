package com.example.lifetogether.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.ListCountEntity

@Database(entities = [GroceryListEntity::class, ListCountEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groceryListDao(): GroceryListDao
    // Add other DAOs here
}
