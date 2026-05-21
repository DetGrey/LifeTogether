package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface FamilyInformationDao {

    // Insert or update family information (FamilyEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFamily(family: FamilyEntity)

    // Insert or update family members (FamilyMemberEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFamilyMembers(members: List<FamilyMemberEntity>)

    // Query to get family details by familyId (FamilyEntity)
    @Query("SELECT * FROM ${Constants.FAMILIES_TABLE} WHERE family_id = :familyId LIMIT 1")
    fun getFamilyInfo(familyId: String): Flow<FamilyEntity>

    @Query("SELECT * FROM ${Constants.FAMILIES_TABLE} WHERE family_id = :familyId LIMIT 1")
    suspend fun getFamilyOnce(familyId: String): FamilyEntity?

    // Query to get family members by familyId (FamilyMemberEntity)
    @Query("SELECT * FROM ${Constants.FAMILY_MEMBERS_TABLE} WHERE family_id = :familyId")
    fun observeFamilyMembers(familyId: String): Flow<List<FamilyMemberEntity>>

    // Query to get image bytearray by familyId
    @Query("SELECT image_data FROM ${Constants.FAMILIES_TABLE} WHERE family_id = :familyId LIMIT 1")
    fun observeImageByteArray(familyId: String): Flow<ByteArray?>

    @Query("UPDATE ${Constants.FAMILIES_TABLE} SET image_data = :imageData WHERE family_id = :familyId")
    suspend fun updateImageByteArray(
        familyId: String,
        imageData: ByteArray?,
    )

    @Query("UPDATE ${Constants.FAMILIES_TABLE} SET image_url = :imageUrl WHERE family_id = :familyId")
    suspend fun updateImageUrl(
        familyId: String,
        imageUrl: String?,
    )

    @Query("UPDATE ${Constants.FAMILIES_TABLE} SET together_since = :togetherSince WHERE family_id = :familyId")
    suspend fun updateTogetherSince(
        familyId: String,
        togetherSince: Date?,
    )

}
