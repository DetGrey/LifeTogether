package com.example.lifetogether.domain.model.session

import com.example.lifetogether.domain.model.UserInformation

sealed interface SessionState {
    data object Loading : SessionState

    data object Unauthenticated : SessionState

    data class Authenticated(
        val user: UserInformation,
    ) : SessionState
}

val SessionState.authenticatedUserOrNull: UserInformation?
    get() = (this as? SessionState.Authenticated)?.user
