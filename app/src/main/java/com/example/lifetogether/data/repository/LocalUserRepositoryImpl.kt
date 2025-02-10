package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.FamilyInformationResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalUserRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
) : UserRepository {
    fun getUserInformation(uid: String): Flow<AuthResultListener> {
        return localDataSource.getUserInformation(uid).map { user ->
            try {
                println("LocalUserRepositoryImpl getUserInformation user: $user")

                AuthResultListener.Success(
                    UserInformation(
                        uid = user.uid,
                        email = user.email,
                        name = user.name,
                        birthday = user.birthday,
                        familyId = user.familyId,
                    ),
                )
            } catch (e: Exception) {
                AuthResultListener.Failure(e.message ?: "Unknown error")
            }
        }
    }

    fun getFamilyInformation(familyId: String): Flow<FamilyInformationResultListener> {
        // Get family information (without members)
        val familyInformationFlow: Flow<FamilyInformationResultListener> = localDataSource.getFamilyInformation(familyId).map { user ->
            try {
                println("LocalUserRepositoryImpl getFamilyInformation user: $user")

                // Initial FamilyInformation without members
                FamilyInformationResultListener.Success(
                    FamilyInformation(
                        familyId = user.familyId,
                    ),
                )
            } catch (e: Exception) {
                FamilyInformationResultListener.Failure(e.message ?: "Unknown error")
            }
        }

        // Get family members
        val familyMembersFlow = localDataSource.getFamilyMembers(familyId).map { list ->
            list.map { familyMember ->
                try {
                    println("LocalUserRepositoryImpl getFamilyInformation familyMember: $familyMember")

                    FamilyMember(
                        uid = familyMember.uid,
                        name = familyMember.name,
                    )
                } catch (e: Exception) {
                    println("Error fetching family members: ${e.message}")
                    emptyList<FamilyMember>()
                }
            }
        }

        // Combine both flows
        return familyInformationFlow.combine(familyMembersFlow) { familyInfo, familyMembers ->
            when (familyInfo) {
                is FamilyInformationResultListener.Success -> {
                    // Add the family members to the family information
                    val validMembers = familyMembers.filterIsInstance<FamilyMember>()
                    val updatedFamilyInfo = familyInfo.familyInformation.copy(members = validMembers)
                    FamilyInformationResultListener.Success(updatedFamilyInfo)
                }
                is FamilyInformationResultListener.Failure -> {
                    familyInfo
                }
            }
        }
    }

    fun removeSavedUserInformation(): ResultListener {
        return localDataSource.clearUserInformationTables()
    }

    override fun logout(uid: String, familyId: String?): ResultListener {
        TODO("Not yet implemented")
    }
}
