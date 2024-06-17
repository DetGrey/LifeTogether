package com.example.lifetogether.domain.callback

import com.google.firebase.firestore.DocumentSnapshot

sealed class DefaultsResultListener {
    data class Success(val documentSnapshot: DocumentSnapshot) : DefaultsResultListener()
    data class Failure(val message: String) : DefaultsResultListener()
}
