package com.example.lifetogether.ui.common.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.di.rememberSyncCoordinator

@Composable
fun FeatureSyncLifecycleBinding(
    keys: Set<SyncKey>,
) {
    if (keys.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    val syncCoordinator = rememberSyncCoordinator()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, syncCoordinator, coroutineScope, keys) {
        fun acquireAll() = keys.forEach { key ->
            syncCoordinator.acquireSynchronizer(scope = coroutineScope, key = key)
        }

        fun releaseAll() = keys.forEach(syncCoordinator::releaseSynchronizer)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> acquireAll()
                Lifecycle.Event.ON_STOP -> releaseAll()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            acquireAll()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            releaseAll()
        }
    }
}
