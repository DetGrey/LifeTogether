package com.example.lifetogether.di

import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.FirebaseStorageDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideFirebaseAuthDataSource(firestoreDataSource: FirestoreDataSource): FirebaseAuthDataSource {
        return FirebaseAuthDataSource(firestoreDataSource)
    }

    @Provides
    @Singleton
    fun provideFirestoreDataSource(): FirestoreDataSource = FirestoreDataSource()

    @Provides
    @Singleton
    fun provideFirebaseStorageDataSource(): FirebaseStorageDataSource = FirebaseStorageDataSource()
}
