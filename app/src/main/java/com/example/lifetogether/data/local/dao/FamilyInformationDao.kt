package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow

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

    // Query to get family members by familyId (FamilyMemberEntity)
    @Query("SELECT * FROM ${Constants.FAMILY_MEMBERS_TABLE} WHERE family_id = :familyId")
    fun getFamilyMembers(familyId: String): Flow<List<FamilyMemberEntity>>

    // Query to get image bytearray by familyId
    @Query("SELECT image_data FROM ${Constants.FAMILIES_TABLE} WHERE family_id = :familyId LIMIT 1")
    fun getImageByteArray(familyId: String): Flow<ByteArray?>

    // Delete all entries in the family table
    @Query("DELETE FROM ${Constants.FAMILIES_TABLE}")
    fun deleteFamiliesTable()

    // Delete all entries in the family members table
    @Query("DELETE FROM ${Constants.FAMILY_MEMBERS_TABLE}")
    fun deleteFamilyMembersTable()
}
