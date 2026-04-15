package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.listener.ResultListener

interface SessionLocalUserRepository {
    fun removeSavedUserInformation(): ResultListener
}
