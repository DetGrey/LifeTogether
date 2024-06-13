package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.User

interface UserRepository {
    fun login(user: User)
    fun signUp(user: User)
}
