package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
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

    fun getImageByteArray(uid: String): Flow<ByteArrayResultListener> {
        println("LocalUserRepositoryImpl getImageByteArray")
        return localDataSource.getImageByteArray(uid).map { byteArray ->
            try {
                if (byteArray != null) {
                    ByteArrayResultListener.Success(byteArray)
                } else {
                    ByteArrayResultListener.Failure("No ByteArray found")
                }
            } catch (e: Exception) {
                ByteArrayResultListener.Failure(e.message ?: "Unknown error")
            }
        }
    }

    fun removeSavedUserInformation(): ResultListener {
        return localDataSource.clearUserInformationTables()
    }

    override fun logout(): ResultListener {
        TODO("Not yet implemented")
    }
}
