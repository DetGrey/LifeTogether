package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class LocalUserRepositoryImpl @Inject constructor(
    private val remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
) : UserRepository {
    private val _userInformation = MutableStateFlow<UserInformation?>(null)
    val userInformation: StateFlow<UserInformation?> = _userInformation.asStateFlow()

    suspend fun getCurrentUser() {
        // Fetch user information and update the state flow
        return remoteUserRepositoryImpl.getCurrentUser().collect { authResult ->
            when (authResult) {
                is AuthResultListener.Success -> {
                    // Update the state flow with the user information
                    _userInformation.value = authResult.userInformation
                }
                is AuthResultListener.Failure -> {
                    // Handle failure, e.g., log the error message
                    // Optionally, you can set _userInformation.value to null or keep the last known value
                    _userInformation.value = null
                }
            }
        }
    }

    override suspend fun login(
        user: User,
    ): AuthResultListener {
        TODO("Not yet implemented")
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
