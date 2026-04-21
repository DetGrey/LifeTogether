package com.example.lifetogether.ui.feature.guides.details

import com.example.lifetogether.domain.result.toUserMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.GuideLeafPointer
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class GuideDetailsUiState(
    val guide: Guide? = null,
    val sectionExpandedState: Map<String, Boolean> = emptyMap(),
    val selectedSectionAmountState: Map<String, Int> = emptyMap(),
    val isUpdatingVisibility: Boolean = false,
    val isStartingGuide: Boolean = false,
    val isDeletingGuide: Boolean = false,
    val showAlertDialog: Boolean = false,
    val error: String = "",
)

internal fun guideSectionKey(
    section: GuideSection,
    sectionIndex: Int,
): String = section.id.ifBlank { "section-$sectionIndex" }

internal fun reconcileSectionExpandedState(
    sections: List<GuideSection>,
    existingState: Map<String, Boolean>,
): Map<String, Boolean> {
    return buildMap {
        sections.forEachIndexed { sectionIndex, section ->
            val sectionKey = guideSectionKey(section, sectionIndex)
            put(sectionKey, existingState[sectionKey] ?: !section.completed)
        }
    }
}

internal fun defaultSectionAmountIndex(section: GuideSection): Int {
    val amount = section.amount.coerceAtLeast(1)
    val completedAmount = section.completedAmount.coerceIn(0, amount)
    return if (completedAmount >= amount) amount - 1 else completedAmount
}

internal fun reconcileSelectedSectionAmountState(
    sections: List<GuideSection>,
    existingState: Map<String, Int>,
): Map<String, Int> {
    return buildMap {
        sections.forEachIndexed { sectionIndex, section ->
            val sectionKey = guideSectionKey(section, sectionIndex)
            val amount = section.amount.coerceAtLeast(1)
            val fallbackIndex = defaultSectionAmountIndex(section)
            val selectedIndex = existingState[sectionKey]
                ?.coerceIn(0, amount - 1)
                ?: fallbackIndex
            put(sectionKey, selectedIndex)
        }
    }
}

@HiltViewModel
class GuideDetailsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val guideRepository: GuideRepository,
) : ViewModel() {
    private companion object {
        const val ALERT_DISMISS_DELAY_MS = 3000L
        const val ERROR_MISSING_GUIDE_ID_FOR_VISIBILITY = "Unable to update visibility. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_DELETE = "Unable to delete this guide. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_STEP_UPDATE = "Unable to update this step. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_OPEN = "Unable to open this guide. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_RESET = "Unable to reset progress. Missing guide id."
        const val ERROR_MISSING_GUIDE_ID_FOR_SAVE = "Unable to save guide changes. Missing guide id."
        const val ERROR_ONLY_OWNER_CAN_CHANGE_VISIBILITY = "Only the owner can change visibility"
        const val ERROR_ONLY_OWNER_CAN_DELETE = "Only the owner can delete this guide"
    }

    private var familyId: String? = null
    private var uid: String? = null
    private var guideId: String? = null
    private var guideJob: Job? = null
    private var dismissAlertJob: Job? = null

    private var pointerCacheGuide: Guide? = null
    private var pointerCacheByStepId: Map<String, GuideLeafPointer> = emptyMap()
    internal var nowProvider: () -> Date = ::Date

    private val _uiState = MutableStateFlow(GuideDetailsUiState())
    val uiState: StateFlow<GuideDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val authenticated = state as? SessionState.Authenticated
                val newFamilyId = authenticated?.user?.familyId
                val newUid = authenticated?.user?.uid
                if ((newFamilyId != null && newUid != null) &&
                    (newFamilyId != familyId || newUid != uid)
                ) {
                    familyId = newFamilyId
                    uid = newUid
                    guideId?.let { startGuideJob(it) }
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                }
            }
        }
    }

    fun dismissAlert() {
        dismissAlertJob?.cancel()
        dismissAlertJob = viewModelScope.launch {
            delay(ALERT_DISMISS_DELAY_MS)
            clearError()
        }
    }

    fun setUp(guideId: String) {
        val isNewGuide = this.guideId != guideId
        if (isNewGuide) {
            this.guideId = guideId
            invalidatePointerCache()
            _uiState.update { state ->
                state.copy(
                    guide = null,
                    sectionExpandedState = emptyMap(),
                    selectedSectionAmountState = emptyMap(),
                    isUpdatingVisibility = false,
                    isStartingGuide = false,
                    isDeletingGuide = false,
                )
            }
        }
        if (familyId != null && uid != null) {
            startGuideJob(guideId)
        }
    }

    private fun startGuideJob(guideId: String) {
        val familyIdValue = familyId ?: return
        val uidValue = uid ?: return

        if (this.guideId == guideId && guideJob?.isActive == true &&
            familyId == familyIdValue && uid == uidValue
        ) return

        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            guideRepository.observeGuideById(
                familyId = familyIdValue,
                id = guideId,
                uid = uidValue,
            ).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val normalizedGuide = normalizeFetchedGuide(
                            guide = result.data,
                            fallbackGuideId = guideId,
                        )
                        applyGuideUpdate(uiGuide = normalizedGuide)
                    }
                    is Result.Failure -> showError(result.error.toUserMessage())
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

        val newVisibility = if (currentGuide.visibility == Visibility.FAMILY)
            Visibility.PRIVATE else Visibility.FAMILY
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
            when (val result = guideRepository.deleteGuide(currentGuideId)) {
                is Result.Success -> onSuccess()
                is Result.Failure -> showError(result.error.toUserMessage())
            }
            updateLoadingState(isDeletingGuide = false)
        }
    }

    fun canToggleStep(stepId: String, amountIndex: Int): Boolean {
        if (stepId.isBlank()) return false
        val currentGuide = _uiState.value.guide ?: return false
        val pointer = pointerForStepId(
            guide = currentGuide,
            stepId = stepId,
            amountIndex = amountIndex,
        ) ?: return false
        return GuideProgress.canTogglePointer(currentGuide.sections, pointer)
    }

    fun toggleStepCompletion(stepId: String, amountIndex: Int) {
        if (stepId.isBlank()) return

        val currentGuide = _uiState.value.guide ?: return
        val currentGuideId = resolveGuideId(currentGuide) ?: run {
            showError(ERROR_MISSING_GUIDE_ID_FOR_STEP_UPDATE)
            return
        }

        val pointer = pointerForStepId(
            guide = currentGuide,
            stepId = stepId,
            amountIndex = amountIndex,
        ) ?: return
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

        applyGuideUpdate(uiGuide = updatedGuide)
        persistGuideProgress(updatedGuide)
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

        updateLoadingState(isStartingGuide = true)
        applyGuideUpdate(uiGuide = updatedGuide)
        persistGuideProgress(updatedGuide) {
            updateLoadingState(isStartingGuide = false)
            onNavigate(currentGuideId)
        }
    }

    fun resetAllProgress() {
        val currentGuide = _uiState.value.guide ?: return
        val currentGuideId = resolveGuideId(currentGuide) ?: run {
            showError(ERROR_MISSING_GUIDE_ID_FOR_RESET)
            return
        }

        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            started = false,
            sections = GuideProgress.resetSectionsProgress(currentGuide.sections),
            resume = null,
            lastUpdated = now(),
        )

        applyGuideUpdate(uiGuide = updatedGuide)
        persistGuideProgress(updatedGuide)
    }

    fun toggleSectionExpanded(sectionKey: String) {
        if (sectionKey.isBlank()) return
        _uiState.update { state ->
            val isExpanded = state.sectionExpandedState[sectionKey] ?: true
            state.copy(
                sectionExpandedState = state.sectionExpandedState + (sectionKey to !isExpanded),
            )
        }
    }

    fun selectSectionAmount(sectionKey: String, amountIndex: Int) {
        if (sectionKey.isBlank() || amountIndex < 0) return
        val guide = _uiState.value.guide ?: return
        val section = guide.sections
            .withIndex()
            .firstOrNull { (index, section) -> guideSectionKey(section, index) == sectionKey }
            ?.value
            ?: return
        val maxIndex = section.amount.coerceAtLeast(1) - 1
        val normalizedAmountIndex = amountIndex.coerceIn(0, maxIndex)
        _uiState.update { state ->
            state.copy(
                selectedSectionAmountState = state.selectedSectionAmountState + (sectionKey to normalizedAmountIndex),
            )
        }
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
            onFailure = { afterPersist?.invoke() },
        )
    }

    private fun persistGuideUpdate(guide: Guide, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val resolvedGuideId = resolveGuideId(guide) ?: run {
            showError(ERROR_MISSING_GUIDE_ID_FOR_SAVE)
            onFailure()
            return
        }
        val normalizedGuide = if (guide.id == resolvedGuideId) guide else guide.copy(id = resolvedGuideId)

        viewModelScope.launch {
            when (val result = guideRepository.updateGuide(normalizedGuide)) {
                is Result.Success -> onSuccess()
                is Result.Failure -> {
                    showError(result.error.toUserMessage())
                    onFailure()
                }
            }
        }
    }

    fun flushPendingChanges() {
        val activeFamilyId = familyId ?: return
        val activeUid = uid ?: return
        val activeGuideId = guideId
        viewModelScope.launch {
            guideRepository.syncPendingGuideProgress(
                familyId = activeFamilyId,
                uid = activeUid,
                force = true,
                guideId = activeGuideId,
            )
        }
    }

    private fun persistGuideProgress(guide: Guide, onComplete: (() -> Unit)? = null) {
        val activeUid = uid ?: run { onComplete?.invoke(); return }
        val activeFamilyId = familyId ?: run { onComplete?.invoke(); return }
        viewModelScope.launch {
            val marked = guideRepository.markGuideProgressDirty(guide, activeUid)
            if (marked) {
                guideRepository.syncPendingGuideProgress(
                    familyId = activeFamilyId,
                    uid = activeUid,
                    force = false,
                    guideId = guide.id,
                )
            }
            onComplete?.invoke()
        }
    }

    private fun pointerForStepId(
        guide: Guide,
        stepId: String,
        amountIndex: Int,
    ): GuideLeafPointer? {
        val normalizedAmountIndex = amountIndex.coerceAtLeast(0)
        return stepPointerIndex(guide)[pointerIndexKey(stepId, normalizedAmountIndex)]
    }

    private fun stepPointerIndex(guide: Guide): Map<String, GuideLeafPointer> {
        if (pointerCacheGuide === guide) return pointerCacheByStepId

        val rebuiltIndex = mutableMapOf<String, GuideLeafPointer>()
        GuideProgress.buildLeafPointers(guide.sections).forEach { pointer ->
            val stepId = GuideProgress.getStepAtPointer(guide.sections, pointer)?.id
            if (!stepId.isNullOrBlank()) {
                val key = pointerIndexKey(stepId, pointer.sectionAmountIndex)
                if (rebuiltIndex[key] == null) rebuiltIndex[key] = pointer
            }
        }

        pointerCacheGuide = guide
        pointerCacheByStepId = rebuiltIndex
        return rebuiltIndex
    }

    private fun normalizeFetchedGuide(guide: Guide, fallbackGuideId: String): Guide {
        val resolvedId = guide.id?.takeIf { it.isNotBlank() } ?: fallbackGuideId
        return if (guide.id == resolvedId) guide else guide.copy(id = resolvedId)
    }

    private fun resolveGuideId(guide: Guide?): String? {
        return guide?.id?.takeIf { it.isNotBlank() } ?: guideId?.takeIf { it.isNotBlank() }
    }

    private fun updateGuideState(guide: Guide) {
        invalidatePointerCache()
        _uiState.update { state ->
            state.copy(
                guide = guide,
                sectionExpandedState = reconcileSectionExpandedState(
                    sections = guide.sections,
                    existingState = state.sectionExpandedState,
                ),
                selectedSectionAmountState = reconcileSelectedSectionAmountState(
                    sections = guide.sections,
                    existingState = state.selectedSectionAmountState,
                ),
            )
        }
    }

    private fun pointerIndexKey(
        stepId: String,
        amountIndex: Int,
    ): String = "${amountIndex.coerceAtLeast(0)}:$stepId"

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
            state.copy(showAlertDialog = true, error = message)
        }
    }

    private fun clearError() {
        _uiState.update { state ->
            state.copy(showAlertDialog = false, error = "")
        }
    }

    private fun now(): Date = nowProvider()
}
