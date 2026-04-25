package com.example.lifetogether.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.sync.SyncCoordinator
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootCoordinatorViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncCoordinator: SyncCoordinator,
    private val guideRepository: GuideRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

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
        syncCoordinator.syncGlobalSynchronizerContext(
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
            guideRepository.syncPendingGuideProgress(familyId = familyId, uid = uid, force = true, guideId = null)
            while (isActive) {
                delay(30_000)
                guideRepository.syncPendingGuideProgress(familyId = familyId, uid = uid, force = false, guideId = null)
            }
        }
    }

    private fun handleFcmSync(uid: String, familyId: String?) {
        if (familyId.isNullOrBlank()) return
        if (lastFcmUid == uid && lastFcmFamilyId == familyId) return

        lastFcmUid = uid
        lastFcmFamilyId = familyId
        viewModelScope.launch {
            when (val result = userRepository.storeFcmToken(uid, familyId)) {
                is Result.Success -> Unit
                is Result.Failure -> Unit
            }
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
        syncCoordinator.cancelAllNonAuthSynchronizers()
    }
}
