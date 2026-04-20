package com.example.lifetogether.di

import com.example.lifetogether.data.remote.CloudflareR2StorageDataSource
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
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
    fun provideStorageDataSource(
        cloudflareR2StorageDataSource: CloudflareR2StorageDataSource,
    ): StorageDataSource = cloudflareR2StorageDataSource
    
    // OPTION 2: Use Firebase Storage (old implementation)
    // Uncomment this and comment out the R2 implementation above to switch back to Firebase
    // @Provides
    // @Singleton
    // fun provideStorageDataSource(
    //     firebaseStorageDataSource: FirebaseStorageDataSource,
    // ): StorageDataSource = firebaseStorageDataSource
}
