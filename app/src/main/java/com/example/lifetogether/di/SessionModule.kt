package com.example.lifetogether.di

import com.example.lifetogether.data.repository.SessionRepositoryImpl
import com.example.lifetogether.data.repository.LocalUserRepositoryImpl
import com.example.lifetogether.data.repository.RemoteUserRepositoryImpl
import com.example.lifetogether.domain.repository.SessionLocalUserRepository
import com.example.lifetogether.domain.repository.SessionRemoteUserRepository
import com.example.lifetogether.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionScopeModule {
    @Provides
    @Singleton
    @AppScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        sessionRepositoryImpl: SessionRepositoryImpl,
    ): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSessionRemoteUserRepository(
        remoteUserRepositoryImpl: RemoteUserRepositoryImpl,
    ): SessionRemoteUserRepository

    @Binds
    @Singleton
    abstract fun bindSessionLocalUserRepository(
        localUserRepositoryImpl: LocalUserRepositoryImpl,
    ): SessionLocalUserRepository
}
