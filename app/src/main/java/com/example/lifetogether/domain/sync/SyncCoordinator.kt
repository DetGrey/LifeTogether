package com.example.lifetogether.domain.sync

import android.app.Application
import com.example.lifetogether.domain.usecase.sync.SyncAlbumsUseCase
import com.example.lifetogether.domain.usecase.sync.SyncCategoriesUseCase
import com.example.lifetogether.domain.usecase.sync.SyncFamilyInformationUseCase
import com.example.lifetogether.domain.usecase.sync.SyncGalleryMediaUseCase
import com.example.lifetogether.domain.usecase.sync.SyncGuidesUseCase
import com.example.lifetogether.domain.usecase.sync.SyncRoutineListsUseCase
import com.example.lifetogether.domain.usecase.sync.SyncUserListsUseCase
import com.example.lifetogether.domain.usecase.sync.SyncGroceryListUseCase
import com.example.lifetogether.domain.usecase.sync.SyncGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.sync.SyncRecipesUseCase
import com.example.lifetogether.domain.usecase.sync.SyncTipTrackerUseCase
import com.example.lifetogether.domain.usecase.sync.SyncUserInformationUseCase
import com.example.lifetogether.domain.usecase.sync.SyncStartHandle
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val application: Application,
    private val syncGroceryListUseCase: SyncGroceryListUseCase,
    private val syncRecipesUseCase: SyncRecipesUseCase,
    private val syncCategoriesUseCase: SyncCategoriesUseCase,
    private val syncGrocerySuggestionsUseCase: SyncGrocerySuggestionsUseCase,
    private val syncUserInformationUseCase: SyncUserInformationUseCase,
    private val syncFamilyInformationUseCase: SyncFamilyInformationUseCase,
    private val syncAlbumsUseCase: SyncAlbumsUseCase,
    private val syncGalleryMediaUseCase: SyncGalleryMediaUseCase,
    private val syncTipTrackerUseCase: SyncTipTrackerUseCase,
    private val syncGuidesUseCase: SyncGuidesUseCase,
    private val syncUserListsUseCase: SyncUserListsUseCase,
    private val syncRoutineListsUseCase: SyncRoutineListsUseCase,
) {
    private val globalSyncKeys = setOf(
        SyncKey.USER,
        SyncKey.FAMILY,
    )

    private val featureSyncKeys = setOf(
        SyncKey.GROCERY_LIST,
        SyncKey.GROCERY_CATEGORIES,
        SyncKey.GROCERY_SUGGESTIONS,
        SyncKey.RECIPES,
        SyncKey.GUIDES,
        SyncKey.TIP_TRACKER,
        SyncKey.GALLERY_ALBUMS,
        SyncKey.GALLERY_MEDIA,
        SyncKey.USER_LISTS,
        SyncKey.ROUTINE_LIST_ENTRIES,
    )

    private var syncedUid: String? = null
    private var syncedFamilyId: String? = null

    private val activeSyncHandles: MutableMap<SyncKey, SyncStartHandle> = mutableMapOf()
    private val activeSyncContexts: MutableMap<SyncKey, SyncContext> = mutableMapOf()
    private val syncRefCounts: MutableMap<SyncKey, Int> = mutableMapOf()

    fun syncGlobalSynchronizerContext(
        scope: CoroutineScope,
        uid: String,
        familyId: String?,
    ) {
        val uidChanged = syncedUid != uid
        val familyChanged = syncedFamilyId != familyId

        syncedUid = uid
        syncedFamilyId = familyId

        val context = SyncContext(uid = uid, familyId = familyId)

        startOrRestartSynchronizer(scope, SyncKey.USER, context)
        if (familyId.isNullOrBlank()) {
            stopSynchronizer(SyncKey.FAMILY)
        } else {
            startOrRestartSynchronizer(scope, SyncKey.FAMILY, context)
        }

        if (uidChanged || familyChanged) {
            restartActiveFeatureSynchronizers(scope, context)
        }
    }

    fun acquireSynchronizer(
        scope: CoroutineScope,
        key: SyncKey,
        uid: String? = null,
        familyId: String? = null,
    ) {
        if (key in globalSyncKeys) {
            val resolvedUid = uid ?: syncedUid ?: return
            syncGlobalSynchronizerContext(scope, resolvedUid, familyId ?: syncedFamilyId)
            return
        }

        val current = syncRefCounts[key] ?: 0
        syncRefCounts[key] = current + 1
        if (current > 0) return

        val context = SyncContext(
            uid = uid ?: syncedUid,
            familyId = familyId ?: syncedFamilyId,
        )
        startOrRestartSynchronizer(scope, key, context)
    }

    fun releaseSynchronizer(key: SyncKey) {
        if (key in globalSyncKeys) return

        val current = syncRefCounts[key] ?: 0
        val updated = (current - 1).coerceAtLeast(0)
        syncRefCounts[key] = updated
        if (updated == 0) {
            stopSynchronizer(key)
        }
    }

    fun cancelAllNonAuthSynchronizers() {
        SyncKey.entries.forEach { key ->
            stopSynchronizer(key)
        }
        syncRefCounts.clear()
        syncedUid = null
        syncedFamilyId = null
    }

    private fun startOrRestartSynchronizer(
        scope: CoroutineScope,
        key: SyncKey,
        context: SyncContext,
    ) {
        val existingContext = activeSyncContexts[key]
        if (existingContext == context && activeSyncHandles[key]?.job?.isActive == true) {
            return
        }

        stopSynchronizer(key)

        val handle = createSynchronizerHandle(scope, key, context) ?: return

        activeSyncContexts[key] = context
        activeSyncHandles[key] = handle
    }

    private fun createSynchronizerHandle(
        scope: CoroutineScope,
        key: SyncKey,
        context: SyncContext,
    ): SyncStartHandle? {
        return when (key) {
            SyncKey.USER -> {
                val uid = context.uid ?: return null
                syncUserInformationUseCase.start(scope, uid)
            }

            SyncKey.FAMILY -> {
                val familyId = context.familyId ?: return null
                syncFamilyInformationUseCase.start(scope, familyId)
            }

            SyncKey.GROCERY_LIST -> {
                val familyId = context.familyId ?: return null
                syncGroceryListUseCase.start(scope, familyId)
            }

            SyncKey.GROCERY_CATEGORIES -> syncCategoriesUseCase.start(scope)

            SyncKey.GROCERY_SUGGESTIONS -> syncGrocerySuggestionsUseCase.start(scope)

            SyncKey.RECIPES -> {
                val familyId = context.familyId ?: return null
                syncRecipesUseCase.start(scope, familyId)
            }

            SyncKey.GUIDES -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                syncGuidesUseCase.start(scope, uid, familyId)
            }

            SyncKey.TIP_TRACKER -> {
                val familyId = context.familyId ?: return null
                syncTipTrackerUseCase.start(scope, familyId)
            }

            SyncKey.GALLERY_ALBUMS -> {
                val familyId = context.familyId ?: return null
                syncAlbumsUseCase.start(scope, familyId)
            }

            SyncKey.GALLERY_MEDIA -> {
                val familyId = context.familyId ?: return null
                syncGalleryMediaUseCase.start(
                    scope = scope,
                    familyId = familyId,
                    context = application.applicationContext,
                )
            }

            SyncKey.USER_LISTS -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                syncUserListsUseCase.start(scope, uid, familyId)
            }

            SyncKey.ROUTINE_LIST_ENTRIES -> {
                val familyId = context.familyId ?: return null
                syncRoutineListsUseCase.start(scope, familyId)
            }
        }
    }

    private fun restartActiveFeatureSynchronizers(
        scope: CoroutineScope,
        context: SyncContext,
    ) {
        featureSyncKeys.forEach { key ->
            if ((syncRefCounts[key] ?: 0) > 0) {
                startOrRestartSynchronizer(scope, key, context)
            } else {
                stopSynchronizer(key)
            }
        }
    }

    private fun stopSynchronizer(key: SyncKey) {
        activeSyncHandles.remove(key)?.job?.cancel()
        activeSyncContexts.remove(key)
    }
}
