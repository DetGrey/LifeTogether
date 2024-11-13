package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.util.Constants.GROCERY_SUGGESTIONS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface GrocerySuggestionsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<GrocerySuggestionEntity>)

    @Delete
    suspend fun deleteItems(items: List<GrocerySuggestionEntity>)

    @Query("SELECT * FROM $GROCERY_SUGGESTIONS_TABLE")
    fun getItems(): Flow<List<GrocerySuggestionEntity>>
}
