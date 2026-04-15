package com.example.lifetogether.ui.common.observer

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.domain.observer.ObserverSyncState
import com.example.lifetogether.ui.viewmodel.RootCoordinatorViewModel

@Composable
fun FeatureObserverLifecycleBinding(
    keys: Set<ObserverKey>,
) {
    if (keys.isEmpty()) return

    val activity = LocalActivity.current as? ComponentActivity ?: return
    val rootCoordinator: RootCoordinatorViewModel = hiltViewModel(activity)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, rootCoordinator, keys) {
        fun acquireAll() = keys.forEach { rootCoordinator.acquireObserver(it) }
        fun releaseAll() = keys.forEach { rootCoordinator.releaseObserver(it) }

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
fun ObserverUpdatingText(
    keys: Set<ObserverKey>,
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val rootCoordinator: RootCoordinatorViewModel = hiltViewModel(activity)

    val syncStates by rootCoordinator.observerSyncStates.collectAsState()
    val activeKeys by rootCoordinator.activeObserverKeys.collectAsState()
    val hasSyncedOnce by rootCoordinator.observerHasSyncedOnce.collectAsState()

    val activeKeysAwaitingFirstSuccess = keys.filter { key ->
        key in activeKeys &&
            syncStates[key] == ObserverSyncState.UPDATING &&
            hasSyncedOnce[key] != true
    }

    if (activeKeysAwaitingFirstSuccess.isNotEmpty()) {
        Text(
            text = "Updating...",
            modifier = modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }
}
