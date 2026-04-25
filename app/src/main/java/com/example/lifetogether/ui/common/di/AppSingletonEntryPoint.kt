package com.example.lifetogether.ui.common.di

import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.sync.SyncCoordinator
import com.example.lifetogether.domain.usecase.image.ObserveImageStateUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppSingletonEntryPoint {
    fun sessionRepository(): SessionRepository
    fun syncCoordinator(): SyncCoordinator
    fun observeImageStateUseCase(): ObserveImageStateUseCase
}