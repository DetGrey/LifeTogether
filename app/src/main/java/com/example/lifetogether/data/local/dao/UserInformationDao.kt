package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.util.Constants.USER_TABLE
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface UserInformationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(item: UserEntity)

    @Query("SELECT * FROM $USER_TABLE WHERE uid = :uid LIMIT 1")
    fun getItems(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM $USER_TABLE WHERE uid = :uid LIMIT 1")
    suspend fun getItemOnce(uid: String): UserEntity?

    @Query("SELECT image_data FROM $USER_TABLE WHERE uid = :uid LIMIT 1")
    fun getImageByteArray(uid: String): Flow<ByteArray?>

    @Query("UPDATE $USER_TABLE SET image_data = :imageData, last_updated = :lastUpdated WHERE uid = :uid")
    suspend fun updateImageByteArray(
        uid: String,
        imageData: ByteArray?,
        lastUpdated: Date,
    )

    @Query("UPDATE $USER_TABLE SET image_url = :imageUrl, last_updated = :lastUpdated WHERE uid = :uid")
    suspend fun updateImageUrl(
        uid: String,
        imageUrl: String?,
        lastUpdated: Date,
    )

    @Query("UPDATE $USER_TABLE SET name = :name, last_updated = :lastUpdated WHERE uid = :uid")
    suspend fun updateName(uid: String, name: String, lastUpdated: Date)

}
