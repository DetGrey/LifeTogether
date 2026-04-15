package com.example.lifetogether.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.observer.ObserverCoordinator
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.domain.observer.ObserverSyncState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.usecase.guides.SyncPendingGuideProgressUseCase
import com.example.lifetogether.domain.usecase.user.StoreFcmTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootCoordinatorViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val observerCoordinator: ObserverCoordinator,
    private val syncPendingGuideProgressUseCase: SyncPendingGuideProgressUseCase,
    private val storeFcmTokenUseCase: StoreFcmTokenUseCase,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionRepository.sessionState

    val observerSyncStates: StateFlow<Map<ObserverKey, ObserverSyncState>> =
        observerCoordinator.observerSyncStates
    val activeObserverKeys: StateFlow<Set<ObserverKey>> =
        observerCoordinator.activeObserverKeys
    val observerHasSyncedOnce: StateFlow<Map<ObserverKey, Boolean>> =
        observerCoordinator.observerHasSyncedOnce

    private var guideProgressSyncJob: Job? = null
    private var lastObserverUid: String? = null
    private var lastObserverFamilyId: String? = null
    private var lastGuideSyncUid: String? = null
    private var lastGuideSyncFamilyId: String? = null
    private var lastFcmUid: String? = null
    private var lastFcmFamilyId: String? = null

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                when (state) {
                    is SessionState.Authenticated -> handleAuthenticated(state)
                    SessionState.Unauthenticated -> handleUnauthenticated()
                    SessionState.Loading -> Unit
                }
            }
        }
    }

    fun acquireObserver(key: ObserverKey) {
        observerCoordinator.acquireObserver(scope = viewModelScope, key = key)
    }

    fun releaseObserver(key: ObserverKey) {
        observerCoordinator.releaseObserver(key)
    }

    private fun handleAuthenticated(state: SessionState.Authenticated) {
        val uid = state.user.uid ?: return
        val familyId = state.user.familyId

        handleObserverSync(uid, familyId)
        handleGuideSync(uid, familyId)
        handleFcmSync(uid, familyId)
    }

    private fun handleObserverSync(uid: String, familyId: String?) {
        val uidChanged = lastObserverUid != uid
        val familyChanged = lastObserverFamilyId != familyId
        if (!uidChanged && !familyChanged) return

        lastObserverUid = uid
        lastObserverFamilyId = familyId
        observerCoordinator.syncGlobalObserverContext(
            scope = viewModelScope,
            uid = uid,
            familyId = familyId,
        )
    }

    private fun handleGuideSync(uid: String, familyId: String?) {
        if (familyId.isNullOrBlank()) {
            if (guideProgressSyncJob?.isActive == true) {
                guideProgressSyncJob?.cancel()
                lastGuideSyncUid = null
                lastGuideSyncFamilyId = null
            }
            return
        }

        val uidChanged = lastGuideSyncUid != uid
        val familyChanged = lastGuideSyncFamilyId != familyId
        if (!uidChanged && !familyChanged && guideProgressSyncJob?.isActive == true) return

        guideProgressSyncJob?.cancel()
        lastGuideSyncUid = uid
        lastGuideSyncFamilyId = familyId

        guideProgressSyncJob = viewModelScope.launch {
            syncPendingGuideProgressUseCase(familyId = familyId, uid = uid, force = true)
            while (isActive) {
                delay(30_000)
                syncPendingGuideProgressUseCase(familyId = familyId, uid = uid, force = false)
            }
        }
    }

    private fun handleFcmSync(uid: String, familyId: String?) {
        if (familyId.isNullOrBlank()) return
        if (lastFcmUid == uid && lastFcmFamilyId == familyId) return

        lastFcmUid = uid
        lastFcmFamilyId = familyId
        viewModelScope.launch {
            storeFcmTokenUseCase.invoke(uid, familyId)
        }
    }

    private fun handleUnauthenticated() {
        guideProgressSyncJob?.cancel()
        guideProgressSyncJob = null
        lastObserverUid = null
        lastObserverFamilyId = null
        lastGuideSyncUid = null
        lastGuideSyncFamilyId = null
        lastFcmUid = null
        lastFcmFamilyId = null
        observerCoordinator.cancelAllNonAuthObservers()
    }
}
