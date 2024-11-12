package com.example.lifetogether.di

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.data.repository.RemoteAdminRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.repository.AdminRepository
import com.example.lifetogether.domain.repository.ListRepository
import com.example.lifetogether.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRemoteAdminRepository(
        remoteAdminRepositoryImpl: RemoteAdminRepositoryImpl
    ): AdminRepository

    @Binds
    abstract fun bindRemoteListRepository(
        remoteListRepositoryImpl: RemoteListRepositoryImpl,
    ): ListRepository

    @Binds
    abstract fun bindLocalListRepository(
        localListRepositoryImpl: LocalListRepositoryImpl,
    ): ListRepository

    @Binds
    abstract fun bindRemoteUserRepository(
        remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
    ): UserRepository

    @Binds
    abstract fun bindLocalUserRepository(
        localUserRepositoryImpl: LocalUserRepositoryImpl,
    ): UserRepository
}
