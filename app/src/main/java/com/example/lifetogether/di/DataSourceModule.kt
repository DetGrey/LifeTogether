package com.example.lifetogether.di

import com.example.lifetogether.data.remote.CloudflareR2StorageDataSource
import com.example.lifetogether.data.remote.FamilyFirestoreDataSource
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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
    fun provideFirebaseAuthDataSource(
        familyFirestoreDataSource: FamilyFirestoreDataSource,
    ): FirebaseAuthDataSource {
        return FirebaseAuthDataSource(familyFirestoreDataSource)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

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
