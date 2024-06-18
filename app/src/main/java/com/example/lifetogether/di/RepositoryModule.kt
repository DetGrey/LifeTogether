package com.example.lifetogether.di

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.repository.ListRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRemoteListRepository(
        remoteListRepositoryImpl: RemoteListRepositoryImpl,
    ): ListRepository

    @Binds
    abstract fun bindLocalListRepository(
        localListRepositoryImpl: LocalListRepositoryImpl,
    ): ListRepository
}
