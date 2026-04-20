package com.example.lifetogether.domain.listener

import com.google.firebase.firestore.DocumentSnapshot

sealed class DefaultsResultListener {
    data class Success(val documentSnapshot: DocumentSnapshot) : DefaultsResultListener()
    data class Failure(val message: String) : DefaultsResultListener()
}
