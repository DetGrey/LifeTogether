package com.example.lifetogether.di

import com.example.lifetogether.data.remote.CloudflareR2StorageDataSource
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.repository.StorageRepository
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
    fun provideStorageRepository(
        cloudflareR2StorageDataSource: CloudflareR2StorageDataSource,
    ): StorageRepository = cloudflareR2StorageDataSource
    
    // OPTION 2: Use Firebase Storage (old implementation)
    // Uncomment this and comment out the R2 implementation above to switch back to Firebase
    // @Provides
    // @Singleton
    // fun provideStorageRepository(
    //     firebaseStorageDataSource: FirebaseStorageDataSource,
    // ): StorageRepository = firebaseStorageDataSource
}
