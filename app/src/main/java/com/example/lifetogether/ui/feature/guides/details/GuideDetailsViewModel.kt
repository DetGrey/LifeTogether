package com.example.lifetogether.ui.feature.guides.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ItemResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.logic.GuideLeafPointer
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideVisibility
import com.example.lifetogether.domain.usecase.item.DeleteItemUseCase
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.domain.usecase.item.UpdateItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import javax.inject.Inject

data class GuideDetailsUiState(
    val guide: Guide? = null,
    val isUpdatingVisibility: Boolean = false,
    val isStartingGuide: Boolean = false,
    val isDeletingGuide: Boolean = false,
    val showAlertDialog: Boolean = false,
    val error: String = "",
)

@HiltViewModel
class GuideDetailsViewModel @Inject constructor(
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
) : ViewModel() {
    private companion object {
        const val ALERT_DISMISS_DELAY_MS = 3000L
        const val ERROR_MISSING_GUIDE_ID_FOR_VISIBILITY = "Unable to update visibility. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_DELETE = "Unable to delete this guide. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_STEP_UPDATE = "Unable to update this step. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_OPEN = "Unable to open this guide. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_SAVE = "Unable to save guide changes. Missing guide id."
        const val ERROR_ONLY_OWNER_CAN_CHANGE_VISIBILITY = "Only the owner can change visibility"
        const val ERROR_ONLY_OWNER_CAN_DELETE = "Only the owner can delete this guide"
    }

    private var familyId: String? = null
    private var uid: String? = null
    private var guideId: String? = null
    private var guideJob: Job? = null
    private var dismissAlertJob: Job? = null

    private var lastSyncedGuide: Guide? = null
    private var latestPersistRequestVersion: Long = 0L
    private val persistMutex = Mutex()

    private var pointerCacheGuide: Guide? = null
    private var pointerCacheByStepId: Map<String, GuideLeafPointer> = emptyMap()
    internal var nowProvider: () -> Date = ::Date

    private val _uiState = MutableStateFlow(GuideDetailsUiState())
    val uiState: StateFlow<GuideDetailsUiState> = _uiState.asStateFlow()

    fun dismissAlert() {
        dismissAlertJob?.cancel()
        dismissAlertJob = viewModelScope.launch {
            delay(ALERT_DISMISS_DELAY_MS)
            clearError()
        }
    }

    fun setUpGuide(
        familyId: String,
        uid: String,
        guideId: String,
    ) {
        val isNewGuideContext = this.familyId != familyId || this.uid != uid || this.guideId != guideId
        if (!isNewGuideContext && guideJob?.isActive == true) {
            return
        }

        if (isNewGuideContext) {
            invalidatePointerCache()
            lastSyncedGuide = null
            _uiState.update { state ->
                state.copy(
                    guide = null,
                    isUpdatingVisibility = false,
                    isStartingGuide = false,
                    isDeletingGuide = false,
                )
            }
        }

        this.familyId = familyId
        this.uid = uid
        this.guideId = guideId

        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            fetchItemByIdUseCase(
                familyId = familyId,
                id = guideId,
                listName = Constants.GUIDES_TABLE,
                itemType = Guide::class,
            ).collect { result ->
                when (result) {
                    is ItemResultListener.Success -> {
                        val fetchedGuide = result.item as? Guide ?: return@collect
                        val normalizedGuide = normalizeFetchedGuide(
                            guide = fetchedGuide,
                            fallbackGuideId = guideId,
                        )
                        lastSyncedGuide = normalizedGuide
                        applyGuideUpdate(
                            uiGuide = normalizedGuide,
                        )
                    }

                    is ItemResultListener.Failure -> {
                        showError(result.message)
                    }
                }
            }
        }
    }

    fun canToggleVisibility(): Boolean {
        val currentGuide = _uiState.value.guide ?: return false
        val activeUid = uid ?: return false
        return currentGuide.ownerUid == activeUid
    }

    fun showOwnershipError(message: String) {
        showError(message)
    }

    fun showVisibilityOwnershipError() {
        showOwnershipError(ERROR_ONLY_OWNER_CAN_CHANGE_VISIBILITY)
    }

    fun showDeleteOwnershipError() {
        showOwnershipError(ERROR_ONLY_OWNER_CAN_DELETE)
    }

    fun toggleVisibility() {
        val currentGuide = _uiState.value.guide ?: return
        val activeUid = uid ?: return
        if (_uiState.value.isUpdatingVisibility) return

        if (!canToggleVisibility()) {
            showVisibilityOwnershipError()
            return
        }

        val currentGuideId = resolveGuideId(currentGuide) ?: run {
            showError(ERROR_MISSING_GUIDE_ID_FOR_VISIBILITY)
            return
        }

        val newVisibility = if (currentGuide.visibility == GuideVisibility.FAMILY) {
            GuideVisibility.PRIVATE
        } else {
            GuideVisibility.FAMILY
        }

        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            visibility = newVisibility,
            ownerUid = activeUid,
            lastUpdated = now(),
        )

        applyGuideUpdate(
            uiGuide = updatedGuide,
            persistedGuide = updatedGuide,
            beforePersist = { updateLoadingState(isUpdatingVisibility = true) },
            afterPersist = { updateLoadingState(isUpdatingVisibility = false) },
        )
    }

    fun deleteGuide(onSuccess: () -> Unit) {
        if (_uiState.value.isDeletingGuide) return

        val currentGuide = _uiState.value.guide
        val currentGuideId = resolveGuideId(currentGuide) ?: run {
            showError(ERROR_MISSING_GUIDE_ID_FOR_DELETE)
            return
        }

        viewModelScope.launch {
            updateLoadingState(isDeletingGuide = true)
            when (val result = deleteItemUseCase(currentGuideId, Constants.GUIDES_TABLE)) {
                is ResultListener.Success -> onSuccess()
                is ResultListener.Failure -> showError(result.message)
            }
            updateLoadingState(isDeletingGuide = false)
        }
    }

    fun canToggleStep(stepId: String): Boolean {
        if (stepId.isBlank()) return false

        val currentGuide = _uiState.value.guide ?: return false
        val pointer = pointerForStepId(currentGuide, stepId) ?: return false
        return GuideProgress.canTogglePointer(currentGuide.sections, pointer)
    }

    fun toggleStepCompletion(stepId: String) {
        if (stepId.isBlank()) return

        val currentGuide = _uiState.value.guide ?: return
        val currentGuideId = resolveGuideId(currentGuide) ?: run {
            showError(ERROR_MISSING_GUIDE_ID_FOR_STEP_UPDATE)
            return
        }

        val pointer = pointerForStepId(currentGuide, stepId) ?: return
        if (!GuideProgress.canTogglePointer(currentGuide.sections, pointer)) return

        val updatedSections = GuideProgress.applyLeafCompletion(
            sections = currentGuide.sections,
            pointer = pointer,
            completed = !GuideProgress.isPointerCompleted(currentGuide.sections, pointer),
        )

        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            started = true,
            sections = updatedSections,
            resume = GuideProgress.firstIncompletePointer(updatedSections)?.let(GuideProgress::resumeFromPointer),
            lastUpdated = now(),
        )

        applyGuideUpdate(
            uiGuide = updatedGuide,
            persistedGuide = updatedGuide,
        )
    }

    fun onStartOrContinue(onNavigate: (String) -> Unit) {
        val currentGuide = _uiState.value.guide ?: return
        val currentGuideId = resolveGuideId(currentGuide) ?: run {
            showError(ERROR_MISSING_GUIDE_ID_FOR_OPEN)
            return
        }

        if (currentGuide.started) {
            onNavigate(currentGuideId)
            return
        }

        if (_uiState.value.isStartingGuide) return

        val firstPointer = GuideProgress.defaultPointerForGuide(currentGuide)
        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            started = true,
            resume = firstPointer?.let(GuideProgress::resumeFromPointer),
            lastUpdated = now(),
        )

        applyGuideUpdate(
            uiGuide = updatedGuide,
            persistedGuide = updatedGuide,
            beforePersist = { updateLoadingState(isStartingGuide = true) },
            afterPersist = { updateLoadingState(isStartingGuide = false) },
            onPersistSuccess = { onNavigate(currentGuideId) },
        )
    }

    private fun applyGuideUpdate(
        uiGuide: Guide,
        persistedGuide: Guide? = null,
        beforePersist: (() -> Unit)? = null,
        afterPersist: (() -> Unit)? = null,
        onPersistSuccess: (() -> Unit)? = null,
    ) {
        updateGuideState(uiGuide)
        if (persistedGuide == null) return

        beforePersist?.invoke()
        persistGuideUpdate(
            guide = persistedGuide,
            onSuccess = {
                afterPersist?.invoke()
                onPersistSuccess?.invoke()
            },
            onFailure = {
                afterPersist?.invoke()
            },
        )
    }

    private fun persistGuideUpdate(
        guide: Guide,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        val resolvedGuideId = resolveGuideId(guide) ?: run {
            showError(ERROR_MISSING_GUIDE_ID_FOR_SAVE)
            onFailure()
            return
        }
        val normalizedGuide = if (guide.id == resolvedGuideId) {
            guide
        } else {
            guide.copy(id = resolvedGuideId)
        }
        val requestVersion = ++latestPersistRequestVersion

        viewModelScope.launch {
            persistMutex.withLock {
                val result = updateItemUseCase(normalizedGuide, Constants.GUIDES_TABLE)

                when (result) {
                    is ResultListener.Success -> {
                        lastSyncedGuide = normalizedGuide
                        onSuccess()
                    }

                    is ResultListener.Failure -> {
                        if (requestVersion == latestPersistRequestVersion) {
                            lastSyncedGuide?.let(::updateGuideState)
                        }
                        showError(result.message)
                        onFailure()
                    }
                }
            }
        }
    }

    private fun pointerForStepId(
        guide: Guide,
        stepId: String,
    ): GuideLeafPointer? {
        return stepPointerIndex(guide)[stepId]
    }

    private fun stepPointerIndex(guide: Guide): Map<String, GuideLeafPointer> {
        if (pointerCacheGuide === guide) {
            return pointerCacheByStepId
        }

        val rebuiltIndex = mutableMapOf<String, GuideLeafPointer>()
        GuideProgress.buildLeafPointers(guide.sections).forEach { pointer ->
            val stepId = GuideProgress.getStepAtPointer(guide.sections, pointer)?.id
            if (!stepId.isNullOrBlank() && rebuiltIndex[stepId] == null) {
                rebuiltIndex[stepId] = pointer
            }
        }

        pointerCacheGuide = guide
        pointerCacheByStepId = rebuiltIndex
        return rebuiltIndex
    }

    private fun normalizeFetchedGuide(
        guide: Guide,
        fallbackGuideId: String,
    ): Guide {
        val resolvedId = guide.id?.takeIf { it.isNotBlank() } ?: fallbackGuideId
        return if (guide.id == resolvedId) {
            guide
        } else {
            guide.copy(
                id = resolvedId,
            )
        }
    }

    private fun resolveGuideId(guide: Guide?): String? {
        return guide?.id?.takeIf { it.isNotBlank() }
            ?: guideId?.takeIf { it.isNotBlank() }
    }

    private fun updateGuideState(guide: Guide) {
        invalidatePointerCache()
        _uiState.update { state ->
            state.copy(guide = guide)
        }
    }

    private fun invalidatePointerCache() {
        pointerCacheGuide = null
        pointerCacheByStepId = emptyMap()
    }

    private fun updateLoadingState(
        isUpdatingVisibility: Boolean = _uiState.value.isUpdatingVisibility,
        isStartingGuide: Boolean = _uiState.value.isStartingGuide,
        isDeletingGuide: Boolean = _uiState.value.isDeletingGuide,
    ) {
        _uiState.update { state ->
            state.copy(
                isUpdatingVisibility = isUpdatingVisibility,
                isStartingGuide = isStartingGuide,
                isDeletingGuide = isDeletingGuide,
            )
        }
    }

    private fun showError(message: String) {
        dismissAlertJob?.cancel()
        _uiState.update { state ->
            state.copy(
                showAlertDialog = true,
                error = message,
            )
        }
    }

    private fun clearError() {
        _uiState.update { state ->
            state.copy(
                showAlertDialog = false,
                error = "",
            )
        }
    }

    private fun now(): Date = nowProvider()
}
