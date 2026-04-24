package com.example.lifetogether.domain.observer

import android.app.Application
import com.example.lifetogether.domain.usecase.observers.ObserveAlbumsUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveCategoriesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveFamilyInformationUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGalleryMediaUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGuidesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveRoutineListsUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveUserListsUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGroceryListUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveRecipesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveTipTrackerUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveUserInformationUseCase
import com.example.lifetogether.domain.usecase.observers.ObserverStartHandle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserverCoordinator @Inject constructor(
    private val application: Application,
    private val observeGroceryListUseCase: ObserveGroceryListUseCase,
    private val observeRecipesUseCase: ObserveRecipesUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val observeGrocerySuggestionsUseCase: ObserveGrocerySuggestionsUseCase,
    private val observeUserInformationUseCase: ObserveUserInformationUseCase,
    private val observeFamilyInformationUseCase: ObserveFamilyInformationUseCase,
    private val observeAlbumsUseCase: ObserveAlbumsUseCase,
    private val observeGalleryMediaUseCase: ObserveGalleryMediaUseCase,
    private val observeTipTrackerUseCase: ObserveTipTrackerUseCase,
    private val observeGuidesUseCase: ObserveGuidesUseCase,
    private val observeUserListsUseCase: ObserveUserListsUseCase,
    private val observeRoutineListsUseCase: ObserveRoutineListsUseCase,
) {
    private val globalObserverKeys = setOf(
        ObserverKey.USER,
        ObserverKey.FAMILY,
    )

    private val featureObserverKeys = setOf(
        ObserverKey.GROCERY_LIST,
        ObserverKey.GROCERY_CATEGORIES,
        ObserverKey.GROCERY_SUGGESTIONS,
        ObserverKey.RECIPES,
        ObserverKey.GUIDES,
        ObserverKey.TIP_TRACKER,
        ObserverKey.GALLERY_ALBUMS,
        ObserverKey.GALLERY_MEDIA,
        ObserverKey.USER_LISTS,
        ObserverKey.ROUTINE_LIST_ENTRIES,
    )

    private var observedUid: String? = null
    private var observedFamilyId: String? = null

    private val activeObserverHandles: MutableMap<ObserverKey, ObserverStartHandle> = mutableMapOf()
    private val activeObserverContexts: MutableMap<ObserverKey, ObserverContext> = mutableMapOf()
    private val observerRefCounts: MutableMap<ObserverKey, Int> = mutableMapOf()
    private val firstSuccessMonitorJobs: MutableMap<ObserverKey, Job> = mutableMapOf()

    private val _observerSyncStates =
        MutableStateFlow(ObserverKey.entries.associateWith { ObserverSyncState.IDLE })
    val observerSyncStates: StateFlow<Map<ObserverKey, ObserverSyncState>> =
        _observerSyncStates.asStateFlow()

    private val _activeObserverKeys = MutableStateFlow<Set<ObserverKey>>(emptySet())
    val activeObserverKeys: StateFlow<Set<ObserverKey>> = _activeObserverKeys.asStateFlow()

    private val _observerHasSyncedOnce =
        MutableStateFlow(ObserverKey.entries.associateWith { false })
    val observerHasSyncedOnce: StateFlow<Map<ObserverKey, Boolean>> =
        _observerHasSyncedOnce.asStateFlow()

    fun syncGlobalObserverContext(
        scope: CoroutineScope,
        uid: String,
        familyId: String?,
    ) {
        val uidChanged = observedUid != uid
        val familyChanged = observedFamilyId != familyId

        observedUid = uid
        observedFamilyId = familyId

        val context = ObserverContext(uid = uid, familyId = familyId)

        startOrRestartObserver(scope, ObserverKey.USER, context)
        if (familyId.isNullOrBlank()) {
            stopObserver(ObserverKey.FAMILY)
        } else {
            startOrRestartObserver(scope, ObserverKey.FAMILY, context)
        }

        if (uidChanged || familyChanged) {
            restartActiveFeatureObservers(scope, context)
        }
    }

    fun acquireObserver(
        scope: CoroutineScope,
        key: ObserverKey,
        uid: String? = null,
        familyId: String? = null,
    ) {
        if (key in globalObserverKeys) {
            val resolvedUid = uid ?: observedUid ?: return
            syncGlobalObserverContext(scope, resolvedUid, familyId ?: observedFamilyId)
            return
        }

        val current = observerRefCounts[key] ?: 0
        observerRefCounts[key] = current + 1
        if (current > 0) return

        val context = ObserverContext(
            uid = uid ?: observedUid,
            familyId = familyId ?: observedFamilyId,
        )
        startOrRestartObserver(scope, key, context)
    }

    fun releaseObserver(key: ObserverKey) {
        if (key in globalObserverKeys) return

        val current = observerRefCounts[key] ?: 0
        val updated = (current - 1).coerceAtLeast(0)
        observerRefCounts[key] = updated
        if (updated == 0) {
            stopObserver(key)
        }
    }

    fun cancelAllNonAuthObservers() {
        ObserverKey.entries.forEach { key ->
            stopObserver(key)
        }
        observerRefCounts.clear()
        observedUid = null
        observedFamilyId = null
    }

    private fun startOrRestartObserver(
        scope: CoroutineScope,
        key: ObserverKey,
        context: ObserverContext,
    ) {
        val existingContext = activeObserverContexts[key]
        if (existingContext == context && activeObserverHandles[key]?.job?.isActive == true) {
            return
        }

        stopObserver(key)

        val handle = createObserverHandle(scope, key, context) ?: run {
            _observerSyncStates.update { it + (key to ObserverSyncState.IDLE) }
            return
        }

        activeObserverContexts[key] = context
        activeObserverHandles[key] = handle
        _activeObserverKeys.update { it + key }
        _observerSyncStates.update { it + (key to ObserverSyncState.UPDATING) }

        firstSuccessMonitorJobs[key] = scope.launch {
            try {
                val firstSuccessResult = handle.firstSuccess.await()
                val nextState = if (firstSuccessResult.isSuccess) {
                    _observerHasSyncedOnce.update { it + (key to true) }
                    ObserverSyncState.READY
                } else {
                    ObserverSyncState.FAILED
                }
                _observerSyncStates.update { it + (key to nextState) }
            } catch (_: CancellationException) {
                // Observer was stopped before first success.
            } catch (_: Throwable) {
                _observerSyncStates.update { it + (key to ObserverSyncState.FAILED) }
            }
        }
    }

    private fun createObserverHandle(
        scope: CoroutineScope,
        key: ObserverKey,
        context: ObserverContext,
    ): ObserverStartHandle? {
        return when (key) {
            ObserverKey.USER -> {
                val uid = context.uid ?: return null
                observeUserInformationUseCase.start(scope, uid)
            }

            ObserverKey.FAMILY -> {
                val familyId = context.familyId ?: return null
                observeFamilyInformationUseCase.start(scope, familyId)
            }

            ObserverKey.GROCERY_LIST -> {
                val familyId = context.familyId ?: return null
                observeGroceryListUseCase.start(scope, familyId)
            }

            ObserverKey.GROCERY_CATEGORIES -> observeCategoriesUseCase.start(scope)

            ObserverKey.GROCERY_SUGGESTIONS -> observeGrocerySuggestionsUseCase.start(scope)

            ObserverKey.RECIPES -> {
                val familyId = context.familyId ?: return null
                observeRecipesUseCase.start(scope, familyId)
            }

            ObserverKey.GUIDES -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                observeGuidesUseCase.start(scope, uid, familyId)
            }

            ObserverKey.TIP_TRACKER -> {
                val familyId = context.familyId ?: return null
                observeTipTrackerUseCase.start(scope, familyId)
            }

            ObserverKey.GALLERY_ALBUMS -> {
                val familyId = context.familyId ?: return null
                observeAlbumsUseCase.start(scope, familyId)
            }

            ObserverKey.GALLERY_MEDIA -> {
                val familyId = context.familyId ?: return null
                observeGalleryMediaUseCase.start(
                    scope = scope,
                    familyId = familyId,
                    context = application.applicationContext,
                )
            }

            ObserverKey.USER_LISTS -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                observeUserListsUseCase.start(scope, uid, familyId)
            }

            ObserverKey.ROUTINE_LIST_ENTRIES -> {
                val familyId = context.familyId ?: return null
                observeRoutineListsUseCase.start(scope, familyId)
            }
        }
    }

    private fun restartActiveFeatureObservers(
        scope: CoroutineScope,
        context: ObserverContext,
    ) {
        featureObserverKeys.forEach { key ->
            if ((observerRefCounts[key] ?: 0) > 0) {
                startOrRestartObserver(scope, key, context)
            } else {
                stopObserver(key)
            }
        }
    }

    private fun stopObserver(key: ObserverKey) {
        firstSuccessMonitorJobs.remove(key)?.cancel()
        activeObserverHandles.remove(key)?.job?.cancel()
        activeObserverContexts.remove(key)
        _activeObserverKeys.update { it - key }
        _observerSyncStates.update { it + (key to ObserverSyncState.IDLE) }
    }
}
