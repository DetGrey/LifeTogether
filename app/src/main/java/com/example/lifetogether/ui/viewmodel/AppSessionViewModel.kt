package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.observer.ObserverCoordinator
import com.example.lifetogether.domain.observer.ObserverKey
import com.example.lifetogether.domain.observer.ObserverSyncState
import com.example.lifetogether.domain.listener.AuthResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.enums.UpdateType
import com.example.lifetogether.domain.usecase.guides.SyncPendingGuideProgressUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveAuthStateUseCase
import com.example.lifetogether.domain.usecase.user.FetchUserInformationUseCase
import com.example.lifetogether.domain.usecase.user.RemoveSavedUserInformationUseCase
import com.example.lifetogether.domain.usecase.user.StoreFcmTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val fetchUserInformationUseCase: FetchUserInformationUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val removeSavedUserInformationUseCase: RemoveSavedUserInformationUseCase,
    private val storeFcmTokenUseCase: StoreFcmTokenUseCase,
    private val observerCoordinator: ObserverCoordinator,
    private val syncPendingGuideProgressUseCase: SyncPendingGuideProgressUseCase,
) : ViewModel() {
    private val _userInformation = MutableStateFlow<UserInformation?>(null)
    val userInformation: StateFlow<UserInformation?> = _userInformation.asStateFlow()

    private var _loading by mutableStateOf(true)
    val loading: Boolean
        get() = _loading

    private var fetchUserInfoJob: Job? = null
    private var guideProgressSyncJob: Job? = null
    private var guideProgressSyncUid: String? = null
    private var guideProgressSyncFamilyId: String? = null

    val observerSyncStates: StateFlow<Map<ObserverKey, ObserverSyncState>> =
        observerCoordinator.observerSyncStates
    val activeObserverKeys: StateFlow<Set<ObserverKey>> =
        observerCoordinator.activeObserverKeys
    val observerHasSyncedOnce: StateFlow<Map<ObserverKey, Boolean>> =
        observerCoordinator.observerHasSyncedOnce

    init {
        viewModelScope.launch {
            observeAuthStateUseCase.invoke().collect { result ->
                when (result) {
                    is AuthResultListener.Success -> {
                        val uid = result.userInformation.uid
                        if (!uid.isNullOrBlank()) {
                            fetchUserInformation(uid)
                            syncGlobalObserverContext(uid, _userInformation.value?.familyId)
                        }
                    }

                    is AuthResultListener.Failure -> {
                        _userInformation.value = null
                        guideProgressSyncJob?.cancel()
                        guideProgressSyncUid = null
                        guideProgressSyncFamilyId = null
                        observerCoordinator.cancelAllNonAuthObservers()
                    }
                }
                delay(1000)
                _loading = false
            }
        }
    }

    fun syncGlobalObserverContext(
        uid: String,
        familyId: String?,
    ) {
        observerCoordinator.syncGlobalObserverContext(
            scope = viewModelScope,
            uid = uid,
            familyId = familyId,
        )
    }

    fun acquireObserver(
        key: ObserverKey,
        uid: String? = null,
        familyId: String? = null,
    ) {
        observerCoordinator.acquireObserver(
            scope = viewModelScope,
            key = key,
            uid = uid,
            familyId = familyId,
        )
    }

    fun releaseObserver(key: ObserverKey) {
        observerCoordinator.releaseObserver(key)
    }

    private fun fetchUserInformation(uid: String) {
        fetchUserInfoJob?.cancel()
        fetchUserInfoJob = viewModelScope.launch {
            fetchUserInformationUseCase.invoke(uid = uid).collect { result ->
                when (result) {
                    is AuthResultListener.Success -> {
                        _userInformation.value = result.userInformation
                        val latestUid = result.userInformation.uid
                        val latestFamilyId = result.userInformation.familyId
                        if (!latestUid.isNullOrBlank()) {
                            syncGlobalObserverContext(latestUid, latestFamilyId)
                            startGuideProgressSync(
                                uid = latestUid,
                                familyId = latestFamilyId,
                            )
                        }
                    }

                    is AuthResultListener.Failure -> {
                        _userInformation.value = null
                    }
                }
            }
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            removeSavedUserInformationUseCase.invoke()
        }
        guideProgressSyncJob?.cancel()
        guideProgressSyncUid = null
        guideProgressSyncFamilyId = null
        observerCoordinator.cancelAllNonAuthObservers()
        _userInformation.value = null
    }

    private fun startGuideProgressSync(
        uid: String,
        familyId: String?,
    ) {
        if (familyId.isNullOrBlank()) {
            guideProgressSyncJob?.cancel()
            guideProgressSyncUid = null
            guideProgressSyncFamilyId = null
            return
        }

        val contextUnchanged =
            guideProgressSyncJob?.isActive == true &&
                guideProgressSyncUid == uid &&
                guideProgressSyncFamilyId == familyId
        if (contextUnchanged) return

        guideProgressSyncJob?.cancel()
        guideProgressSyncUid = uid
        guideProgressSyncFamilyId = familyId

        guideProgressSyncJob = viewModelScope.launch {
            syncPendingGuideProgressUseCase(
                familyId = familyId,
                uid = uid,
                force = true,
            )
            while (isActive) {
                delay(30_000)
                syncPendingGuideProgressUseCase(
                    familyId = familyId,
                    uid = uid,
                    force = false,
                )
            }
        }
    }

    var itemCount: Map<String, Int> by mutableStateOf(mapOf())

    fun updateItemCount(collection: String, updateType: UpdateType) {
        val currentCount = itemCount[collection] ?: 0
        val updatedCount = when (updateType) {
            UpdateType.ADD -> currentCount + 1
            UpdateType.SUBTRACT -> (currentCount - 1).coerceAtLeast(0)
        }
        itemCount = itemCount.toMutableMap().apply {
            this[collection] = updatedCount
        }
    }

    fun storeFcmToken(
        uid: String,
        familyId: String,
    ) {
        viewModelScope.launch {
            storeFcmTokenUseCase.invoke(uid, familyId)
        }
    }
}
