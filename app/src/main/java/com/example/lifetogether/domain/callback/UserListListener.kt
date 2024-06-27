package com.example.lifetogether.domain.callback

import com.example.lifetogether.domain.model.UserInformation

sealed class UserListListener {
    data class Success(val userInformationList: List<UserInformation>) : UserListListener()
    data class Failure(val message: String) : UserListListener()
}

// TODO THIS IS A TEMP CLASS THAT SHOULD BE DELETED AGAIN
