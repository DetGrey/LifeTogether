package com.example.lifetogether.ui.common.sync

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.domain.sync.SyncState
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

@Composable
fun SyncUpdatingText(
    keys: Set<SyncKey>,
) {
    val syncCoordinator = rememberSyncCoordinator()

    val syncStates by syncCoordinator.syncStates.collectAsStateWithLifecycle()
    val activeKeys by syncCoordinator.activeSyncKeys.collectAsStateWithLifecycle()
    val hasSyncedOnce by syncCoordinator.hasSyncedOnce.collectAsStateWithLifecycle()

    val activeKeysAwaitingFirstSuccess = keys.filter { key ->
        key in activeKeys &&
            syncStates[key] == SyncState.UPDATING &&
            hasSyncedOnce[key] != true
    }

    if (activeKeysAwaitingFirstSuccess.isNotEmpty()) {
        Text(
            text = "Updating...",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }
}
