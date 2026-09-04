package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun signOut(): Result<Unit, AppError>
}
