package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.session.SessionState
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun signOut(): ResultListener
}
