package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.GuideProgressEntity
import com.example.lifetogether.util.Constants.GUIDE_PROGRESS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface GuideProgressDao {
    @Query("SELECT * FROM $GUIDE_PROGRESS_TABLE WHERE family_id = :familyId AND uid = :uid")
    fun getItems(familyId: String, uid: String): Flow<List<GuideProgressEntity>>

    @Query("SELECT * FROM $GUIDE_PROGRESS_TABLE WHERE family_id = :familyId AND uid = :uid AND guide_id = :guideId LIMIT 1")
    fun getItemByGuideIdFlow(
        familyId: String,
        uid: String,
        guideId: String,
    ): Flow<GuideProgressEntity?>

    @Query("SELECT * FROM $GUIDE_PROGRESS_TABLE WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): GuideProgressEntity?

    @Query("SELECT * FROM $GUIDE_PROGRESS_TABLE WHERE family_id = :familyId AND uid = :uid AND pending_sync = 1")
    suspend fun getPendingItems(familyId: String, uid: String): List<GuideProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: GuideProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<GuideProgressEntity>)

    @Query("DELETE FROM $GUIDE_PROGRESS_TABLE WHERE family_id = :familyId AND uid = :uid AND guide_id NOT IN (:guideIds)")
    suspend fun deleteMissingGuides(
        familyId: String,
        uid: String,
        guideIds: List<String>,
    )

    @Query("DELETE FROM $GUIDE_PROGRESS_TABLE WHERE family_id = :familyId AND uid = :uid")
    suspend fun deleteFamilyUserItems(
        familyId: String,
        uid: String,
    )

    @Query("DELETE FROM $GUIDE_PROGRESS_TABLE WHERE family_id = :familyId AND guide_id IN (:guideIds)")
    suspend fun deleteByGuideIds(
        familyId: String,
        guideIds: List<String>,
    )
}
