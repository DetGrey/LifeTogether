package com.example.lifetogether.ui.common.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.sync.SyncCoordinator
import com.example.lifetogether.domain.usecase.image.ObserveImageStateUseCase
import dagger.hilt.android.EntryPointAccessors

@Composable
private fun rememberAppSingletonEntryPoint(): AppSingletonEntryPoint {
    val context = LocalContext.current
    val appContext = context.applicationContext

    return remember(appContext) {
        EntryPointAccessors.fromApplication(
            appContext,
            AppSingletonEntryPoint::class.java,
        )
    }
}

@Composable
fun rememberSessionRepository(): SessionRepository {
    return rememberAppSingletonEntryPoint().sessionRepository()
}

@Composable
fun rememberSyncCoordinator(): SyncCoordinator {
    return rememberAppSingletonEntryPoint().syncCoordinator()
}

@Composable
fun rememberObserveImageStateUseCase(): ObserveImageStateUseCase {
    return rememberAppSingletonEntryPoint().observeImageStateUseCase()
}