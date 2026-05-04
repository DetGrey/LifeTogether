package com.example.lifetogether.domain.sync

import android.app.Application
import android.util.Log
import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.TipTrackerRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.repository.UserRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val application: Application,
    private val groceryRepository: GroceryRepository,
    private val recipeRepository: RecipeRepository,
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val galleryRepository: GalleryRepository,
    private val tipTrackerRepository: TipTrackerRepository,
    private val guideRepository: GuideRepository,
    private val userListRepository: UserListRepository,
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
        SyncKey.WISH_LIST_ENTRIES,
        SyncKey.NOTE_ENTRIES,
        SyncKey.CHECKLIST_ENTRIES,
        SyncKey.MEAL_PLAN_ENTRIES,
    )

    private var syncedUid: String? = null
    private var syncedFamilyId: String? = null

    private val activeSyncHandles: MutableMap<SyncKey, Job> = mutableMapOf()
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
        if (existingContext == context && activeSyncHandles[key]?.isActive == true) {
            return
        }

        stopSynchronizer(key)

        val job = createSynchronizerHandle(scope, key, context) ?: return

        activeSyncContexts[key] = context
        activeSyncHandles[key] = job
    }

    private fun createSynchronizerHandle(
        scope: CoroutineScope,
        key: SyncKey,
        context: SyncContext,
    ): Job? {
        return when (key) {
            SyncKey.USER -> {
                val uid = context.uid ?: return null
                startSync(scope, "ObserveUserInfo", "user info sync failure") {
                    userRepository.syncUserInformationFromRemote(uid)
                }
            }

            SyncKey.FAMILY -> {
                val familyId = context.familyId ?: return null
                startSync(scope, "ObserveFamilyInfo", "family info sync failure") {
                    familyRepository.syncFamilyInformationFromRemote(familyId)
                }
            }

            SyncKey.GROCERY_LIST -> {
                val familyId = context.familyId ?: return null
                startSync(scope, "ObserveGroceryList", "grocery list sync failure") {
                    groceryRepository.syncGroceryItems(familyId)
                }
            }

            SyncKey.GROCERY_CATEGORIES -> startSync(scope, "ObserveCategories", "categories sync failure") {
                groceryRepository.syncCategories()
            }

            SyncKey.GROCERY_SUGGESTIONS -> startSync(scope, "ObserveGrocerySugg", "grocery suggestions sync failure") {
                groceryRepository.syncGrocerySuggestions()
            }

            SyncKey.RECIPES -> {
                val familyId = context.familyId ?: return null
                startSync(scope, "ObserveRecipes", "recipes sync failure") {
                    recipeRepository.syncRecipesFromRemote(familyId)
                }
            }

            SyncKey.GUIDES -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                startSync(scope, "SyncGuidesUseCase", "guides sync failure") {
                    guideRepository.syncGuidesFromRemote(uid, familyId)
                }
            }

            SyncKey.TIP_TRACKER -> {
                val familyId = context.familyId ?: return null
                startSync(scope, "ObserveTipTracker", "tip sync failure") {
                    tipTrackerRepository.syncTipsFromRemote(familyId)
                }
            }

            SyncKey.GALLERY_ALBUMS -> {
                val familyId = context.familyId ?: return null
                startSync(scope, "ObserveAlbums", "albums sync failure") {
                    galleryRepository.syncAlbumsFromRemote(familyId)
                }
            }

            SyncKey.GALLERY_MEDIA -> {
                val familyId = context.familyId ?: return null
                startSync(scope, "ObserveGalleryMedia", "failure") {
                    galleryRepository.syncGalleryMediaFromRemote(familyId, application.applicationContext)
                }
            }

            SyncKey.USER_LISTS -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                startSync(scope, "SyncUserListsUseCase", "user lists sync failure") {
                    userListRepository.syncUserListsFromRemote(uid, familyId)
                }
            }

            SyncKey.ROUTINE_LIST_ENTRIES -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                startSync(scope, "SyncRoutineListsUseCase", "routine list sync failure") {
                    userListRepository.syncRoutineListEntriesFromRemote(uid, familyId)
                }
            }

            SyncKey.WISH_LIST_ENTRIES -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                startSync(scope, "SyncWishListEntriesUseCase", "wish list sync failure") {
                    userListRepository.syncWishListEntriesFromRemote(uid, familyId)
                }
            }

            SyncKey.NOTE_ENTRIES -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                startSync(scope, "SyncNoteEntriesUseCase", "note entries sync failure") {
                    userListRepository.syncNoteEntriesFromRemote(uid, familyId)
                }
            }

            SyncKey.CHECKLIST_ENTRIES -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                startSync(scope, "SyncChecklistEntriesUseCase", "checklist entries sync failure") {
                    userListRepository.syncChecklistEntriesFromRemote(uid, familyId)
                }
            }

            SyncKey.MEAL_PLAN_ENTRIES -> {
                val uid = context.uid ?: return null
                val familyId = context.familyId ?: return null
                startSync(scope, "SyncMealPlanEntriesUseCase", "meal plan entries sync failure") {
                    userListRepository.syncMealPlanEntriesFromRemote(uid, familyId)
                }
            }
        }
    }

    private fun startSync(
        scope: CoroutineScope,
        tag: String,
        failureMessage: String,
        block: () -> Flow<AppResult<Unit, AppError>>,
    ): Job {
        return scope.launch {
            block().collect { result ->
                when (result) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> Log.e(tag, "$failureMessage: ${result.error}")
                }
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
        activeSyncHandles.remove(key)?.cancel()
        activeSyncContexts.remove(key)
    }
}
