package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.util.Constants.CATEGORY_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<CategoryEntity>)

    @Delete
    suspend fun deleteItems(items: List<CategoryEntity>)

    @Query("SELECT * FROM $CATEGORY_TABLE")
    fun getItems(): Flow<List<CategoryEntity>>
}
