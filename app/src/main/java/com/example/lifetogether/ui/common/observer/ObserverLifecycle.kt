package com.example.lifetogether.ui.common.observer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.domain.observer.ObserverSyncState
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun FeatureObserverLifecycleBinding(
    appSessionViewModel: AppSessionViewModel,
    keys: Set<ObserverKey>,
    uid: String? = null,
    familyId: String? = null,
) {
    if (keys.isEmpty()) return

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appSessionViewModel, keys, uid, familyId) {
        fun acquireAll() {
            keys.forEach { key ->
                appSessionViewModel.acquireObserver(
                    key = key,
                    uid = uid,
                    familyId = familyId,
                )
            }
        }

        fun releaseAll() {
            keys.forEach { key ->
                appSessionViewModel.releaseObserver(key)
            }
        }

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
    appSessionViewModel: AppSessionViewModel,
    keys: Set<ObserverKey>,
    modifier: Modifier = Modifier,
) {
    val syncStates by appSessionViewModel.observerSyncStates.collectAsState()
    val activeKeys by appSessionViewModel.activeObserverKeys.collectAsState()
    val hasSyncedOnce by appSessionViewModel.observerHasSyncedOnce.collectAsState()

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
