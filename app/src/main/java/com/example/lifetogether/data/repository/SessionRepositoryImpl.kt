package com.example.lifetogether.data.repository

import android.util.Log
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.di.AppScope
import com.example.lifetogether.domain.listener.AuthResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.model.session.authenticatedUserOrNull
import com.example.lifetogether.domain.repository.SessionLocalUserRepository
import com.example.lifetogether.domain.repository.SessionRemoteUserRepository
import com.example.lifetogether.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    @param:AppScope private val appScope: CoroutineScope,
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val sessionRemoteUserRepository: SessionRemoteUserRepository,
    private val sessionLocalUserRepository: SessionLocalUserRepository,
) : SessionRepository {
    companion object {
        private const val TAG = "SessionRepository"
    }

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        appScope.launch {
            firebaseAuthDataSource.authStateListener().collectLatest { authState ->
                when (authState) {
                    is AuthResultListener.Success -> {
                        val uid = authState.userInformation.uid
                        if (uid.isNullOrBlank()) {
                            _sessionState.value = SessionState.Unauthenticated
                            return@collectLatest
                        }

                        _sessionState.value = SessionState.Loading
                        sessionRemoteUserRepository.observeUserInformation(uid).collect { result ->
                            when (result) {
                                is AuthResultListener.Success -> {
                                    _sessionState.value = SessionState.Authenticated(result.userInformation)
                                }

                                is AuthResultListener.Failure -> {
                                    Log.w(TAG, "User information lookup failed for uid=$uid: ${result.message}")
                                    _sessionState.value = SessionState.Unauthenticated
                                }
                            }
                        }
                    }

                    is AuthResultListener.Failure -> {
                        _sessionState.value = SessionState.Unauthenticated
                    }
                }
            }
        }
    }

    override suspend fun signOut(): ResultListener {
        val currentUser = resolveUserForSignOut()
            ?: return ResultListener.Failure("No authenticated user available for sign out")

        val uid = currentUser.uid
            ?: return ResultListener.Failure("No authenticated user available for sign out")

        return when (val remoteResult = sessionRemoteUserRepository.logout(uid, currentUser.familyId)) {
            is ResultListener.Failure -> remoteResult
            is ResultListener.Success -> {
                _sessionState.value = SessionState.Unauthenticated
                when (val localResult = sessionLocalUserRepository.removeSavedUserInformation()) {
                    is ResultListener.Failure -> {
                        Log.e(TAG, "Local session cleanup failed after remote logout: ${localResult.message}")
                        ResultListener.Failure(localResult.message)
                    }

                    is ResultListener.Success -> ResultListener.Success
                }
            }
        }
    }

    private suspend fun resolveUserForSignOut(): UserInformation? {
        sessionState.value.authenticatedUserOrNull?.let { return it }

        val currentUid = firebaseAuthDataSource.currentUserUid() ?: return null
        return when (val result = sessionRemoteUserRepository.fetchUserInformation(currentUid)) {
            is AuthResultListener.Success -> result.userInformation
            is AuthResultListener.Failure -> {
                Log.w(TAG, "Fallback user fetch for sign out failed for uid=$currentUid: ${result.message}")
                UserInformation(uid = currentUid)
            }
        }
    }
}
