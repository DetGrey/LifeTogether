package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.FamilyInformationDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLocalDataSource @Inject constructor(
    private val userInformationDao: UserInformationDao,
    private val familyInformationDao: FamilyInformationDao,
) {
    fun getUserInformation(uid: String): Flow<UserEntity?> = userInformationDao.getItems(uid)

    suspend fun updateUserInformation(
        userInformation: UserInformation,
        byteArray: ByteArray? = null,
    ) {
        var userEntity = UserEntity(
            uid = userInformation.uid ?: "",
            email = userInformation.email,
            name = userInformation.name,
            birthday = userInformation.birthday,
            familyId = userInformation.familyId,
        )
        if (byteArray != null) {
            userEntity = userEntity.copy(imageData = byteArray)
        }
        userInformationDao.updateItems(userEntity)
    }

    suspend fun userHasProfileImage(uid: String): Boolean = (userInformationDao.hasImageData(uid) ?: 0) == 1

    fun getFamilyInformation(familyId: String): Flow<FamilyEntity> = familyInformationDao.getFamilyInfo(familyId)

    fun getFamilyMembers(familyId: String): Flow<List<FamilyMemberEntity>> = familyInformationDao.getFamilyMembers(familyId)

    fun getProfileImageByteArray(uid: String): Flow<ByteArray?> = userInformationDao.getImageByteArray(uid)

    fun getFamilyImageByteArray(familyId: String): Flow<ByteArray?> = familyInformationDao.getImageByteArray(familyId)

    suspend fun updateFamilyInformation(
        familyInformation: FamilyInformation,
        byteArray: ByteArray? = null,
    ) {
        var familyEntity = FamilyEntity(
            familyId = familyInformation.familyId ?: "",
        )
        if (byteArray != null) {
            familyEntity = familyEntity.copy(imageData = byteArray)
        }
        familyInformationDao.updateFamily(familyEntity)

        val familyMembers = familyInformation.members?.map {
            FamilyMemberEntity(
                uid = it.uid ?: "",
                familyId = familyInformation.familyId,
                name = it.name,
            )
        } ?: emptyList()
        familyInformationDao.updateFamilyMembers(familyMembers)
    }

    suspend fun familyHasImage(familyId: String): Boolean = (familyInformationDao.hasImageData(familyId) ?: 0) == 1
}
