package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.FamilyInformationDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLocalDataSource @Inject constructor(
    private val userInformationDao: UserInformationDao,
    private val familyInformationDao: FamilyInformationDao,
) {
    suspend fun updateUserInformation(
        userInformation: UserInformation,
        byteArray: ByteArray? = null,
    ) {
        val existingUser = userInformationDao.getItemOnce(userInformation.uid)
        val existingImageUrl = existingUser?.imageUrl
        val existingImageData = existingUser?.imageData
        val imageData = byteArray ?: if (existingImageUrl == userInformation.imageUrl) {
            existingImageData
        } else {
            null
        }
        val userEntity = UserEntity(
            uid = userInformation.uid,
            email = userInformation.email,
            name = userInformation.name,
            birthday = userInformation.birthday,
            familyId = userInformation.familyId,
            imageData = imageData,
            imageUrl = userInformation.imageUrl,
        )
        userInformationDao.updateItems(userEntity)
    }

    fun observeFamilyInformation(familyId: String): Flow<FamilyEntity> = familyInformationDao.getFamilyInfo(familyId)

    suspend fun getProfileOnce(uid: String): UserEntity? = userInformationDao.getItemOnce(uid)

    fun observeFamilyMembers(familyId: String): Flow<List<FamilyMemberEntity>> = familyInformationDao.observeFamilyMembers(familyId)

    fun observeProfileImageByteArray(uid: String): Flow<ByteArray?> = userInformationDao.observeImageByteArray(uid)

    suspend fun updateProfileImageByteArray(
        uid: String,
        imageData: ByteArray?,
    ) {
        userInformationDao.updateImageByteArray(uid, imageData)
    }

    suspend fun updateProfileImageUrl(
        uid: String,
        imageUrl: String?,
    ) {
        userInformationDao.updateImageUrl(uid, imageUrl)
    }

    fun observeFamilyImageByteArray(familyId: String): Flow<ByteArray?> = familyInformationDao.observeImageByteArray(familyId)

    suspend fun getFamilyOnce(familyId: String): FamilyEntity? = familyInformationDao.getFamilyOnce(familyId)

    suspend fun updateFamilyImageByteArray(
        familyId: String,
        imageData: ByteArray?,
    ) {
        familyInformationDao.updateImageByteArray(familyId, imageData)
    }

    suspend fun updateFamilyImageUrl(
        familyId: String,
        imageUrl: String?,
    ) {
        familyInformationDao.updateImageUrl(familyId, imageUrl)
    }

    suspend fun updateFamilyTogetherSince(
        familyId: String,
        togetherSince: Date?,
    ) {
        familyInformationDao.updateTogetherSince(familyId, togetherSince)
    }

    suspend fun updateFamilyInformation(
        familyInformation: FamilyInformation,
        byteArray: ByteArray? = null,
    ) {
        val existingFamily = familyInformationDao.getFamilyOnce(familyInformation.familyId)
        val existingImageUrl = existingFamily?.imageUrl
        val existingImageData = existingFamily?.imageData
        val imageData = byteArray ?: if (existingImageUrl == familyInformation.imageUrl) {
            existingImageData
        } else {
            null
        }
        val familyEntity = FamilyEntity(
            familyId = familyInformation.familyId,
            imageData = imageData,
            imageUrl = familyInformation.imageUrl,
            togetherSince = familyInformation.togetherSince,
        )
        familyInformationDao.updateFamily(familyEntity)

        val familyMembers = familyInformation.members.map {
            FamilyMemberEntity(
                uid = it.uid,
                familyId = familyInformation.familyId,
                name = it.name,
            )
        }
        familyInformationDao.updateFamilyMembers(familyMembers)
    }
}
