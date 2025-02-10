package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.util.Constants.RECIPES_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipesDao {
    @Query("SELECT * FROM $RECIPES_TABLE")
    fun getAll(): List<RecipeEntity>

    @Query("SELECT * FROM $RECIPES_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM $RECIPES_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getRecipeById(familyId: String, id: String): RecipeEntity?

    @Query("SELECT image_data FROM $RECIPES_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getImageByteArray(familyId: String, id: String): Flow<ByteArray?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<RecipeEntity>)

    @Query("DELETE FROM $RECIPES_TABLE")
    fun deleteTable()

    @Query("DELETE FROM $RECIPES_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)
}
