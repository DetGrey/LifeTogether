package com.example.lifetogether.di

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
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
    fun provideFirebaseAuthDataSource(): FirebaseAuthDataSource = FirebaseAuthDataSource()

    @Provides
    @Singleton
    fun provideFirestoreDataSource(
        localDataSource: LocalDataSource,
    ): FirestoreDataSource = FirestoreDataSource(localDataSource)
}
