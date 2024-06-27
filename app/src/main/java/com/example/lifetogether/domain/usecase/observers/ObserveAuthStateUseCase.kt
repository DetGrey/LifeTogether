package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(): Flow<AuthResultListener> {
        println("ObserveAuthStateUseCase invoked")
        return firebaseAuthDataSource.authStateListener()
    }
}
