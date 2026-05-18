package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.di.AppScope
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.model.session.authenticatedUserOrNull
import com.example.lifetogether.domain.repository.SessionUserRepository
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
    private val sessionUserRepository: SessionUserRepository,
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
                    is Result.Success -> {
                        val uid = authState.data
                        if (uid.isBlank()) {
                            _sessionState.value = SessionState.Unauthenticated
                            return@collectLatest
                        }

                        _sessionState.value = SessionState.Loading
                        sessionUserRepository.observeUserInformation(uid).collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    _sessionState.value = SessionState.Authenticated(result.data)
                                }

                                is Result.Failure -> {
                                    Log.w(TAG, "User information lookup failed for uid=$uid: ${result.error}")
                                    _sessionState.value = SessionState.Unauthenticated
                                }
                            }
                        }
                    }

                    is Result.Failure -> {
                        _sessionState.value = SessionState.Unauthenticated
                    }
                }
            }
        }
    }

    override suspend fun signOut(): Result<Unit, AppError> {
        val signOutTarget = resolveSignOutTarget()
            ?: return Result.Failure(AppErrors.authentication("No authenticated user available for sign out"))

        return when (val remoteResult = sessionUserRepository.logout(signOutTarget.uid, signOutTarget.familyId)) {
            is Result.Failure -> remoteResult
            is Result.Success -> {
                _sessionState.value = SessionState.Unauthenticated
                when (val localResult = sessionUserRepository.deleteSavedUserInformation()) {
                    is Result.Failure -> {
                        Log.e(TAG, "Local session cleanup failed after remote logout: ${localResult.error}")
                        Result.Failure(localResult.error)
                    }

                    is Result.Success -> Result.Success(Unit)
                }
            }
        }
    }

    private data class SignOutTarget(
        val uid: String,
        val familyId: String?,
    )

    private suspend fun resolveSignOutTarget(): SignOutTarget? {
        sessionState.value.authenticatedUserOrNull?.let {
            return SignOutTarget(it.uid, it.familyId)
        }

        val currentUid = firebaseAuthDataSource.currentUserUid() ?: return null
        return when (val result = sessionUserRepository.fetchUserInformation(currentUid)) {
            is Result.Success -> SignOutTarget(result.data.uid, result.data.familyId)
            is Result.Failure -> {
                Log.w(TAG, "Fallback user fetch for sign out failed for uid=$currentUid: ${result.error}")
                SignOutTarget(currentUid, null)
            }
        }
    }
}
