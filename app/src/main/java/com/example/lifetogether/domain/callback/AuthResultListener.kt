package com.example.lifetogether.domain.callback

import com.example.lifetogether.domain.model.UserInformation

sealed class AuthResultListener {
    data class Success(val userInformation: UserInformation) : AuthResultListener()
    data class Failure(val message: String) : AuthResultListener()
}
