package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalUserRepositoryImpl @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
    private val localDataSource: LocalDataSource,
) : UserRepository {

    fun getUserInformation(uid: String): Flow<AuthResultListener> {
        return localDataSource.getUserInformation(uid).map { user ->
            try {
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

    override suspend fun login(
        user: User,
    ): AuthResultListener {
        val loginResult = remoteUserRepositoryImpl.login(user)
        return loginResult
//        when (loginResult) {
//            is AuthResultListener.Success -> {
//                firestoreDataSource.getUserInformation(currentUserUid)
//            }
//            is AuthResultListener.Failure -> {
//
//            }
//        }
//        TODO("Not yet implemented")
    }

    override suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): AuthResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun logout(): ResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun changeName(uid: String, newName: String): ResultListener {
        TODO("Not yet implemented")
    }
}
