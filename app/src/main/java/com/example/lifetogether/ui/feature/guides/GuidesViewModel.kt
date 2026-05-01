package com.example.lifetogether.ui.feature.guides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.guide.ImportGuidesUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuidesViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val guideRepository: GuideRepository,
    private val importGuidesUseCase: ImportGuidesUseCase,
) : ViewModel() {
    private var familyId: String? = null
    private var uid: String? = null
    private var guidesJob: Job? = null

    private val _uiState = MutableStateFlow<GuidesUiState>(GuidesUiState.Loading)
    val uiState: StateFlow<GuidesUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val authenticated = state as? SessionState.Authenticated
                val newFamilyId = authenticated?.user?.familyId
                val newUid = authenticated?.user?.uid
                if (newFamilyId != null && newUid != null &&
                    (newFamilyId != familyId || newUid != uid)
                ) {
                    familyId = newFamilyId
                    uid = newUid
                    setUpGuides()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                    _uiState.value = GuidesUiState.Loading
                }
            }
        }
    }

    fun onEvent(event: GuidesUiEvent) {
        when (event) {
            GuidesUiEvent.OpenAddOptionsDialog -> updateContentState {
                it.copy(showAddOptionsDialog = true)
            }

            GuidesUiEvent.CloseAddOptionsDialog -> updateContentState {
                it.copy(showAddOptionsDialog = false)
            }

            GuidesUiEvent.OpenImportDialog -> updateContentState {
                it.copy(
                    showAddOptionsDialog = false,
                    showImportDialog = true,
                    importSummary = "",
                )
            }

            GuidesUiEvent.CloseImportDialog -> updateContentState {
                it.copy(showImportDialog = false)
            }

            is GuidesUiEvent.ImportGuidesFromJson -> importGuidesFromJson(event.json)
        }
    }

    private fun setUpGuides() {
        val familyIdValue = familyId ?: return
        val uidValue = uid ?: return

        guidesJob?.cancel()
        guidesJob = viewModelScope.launch {
            guideRepository.observeGuides(familyId = familyIdValue, uid = uidValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        if (_uiState.value is GuidesUiState.Loading) {
                            _uiState.value = GuidesUiState.Content(guides = result.data)
                        } else {
                            updateContentState { it.copy(guides = result.data) }
                        }
                    }
                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun importGuidesFromJson(json: String) {
        val activeFamilyId = familyId
        val activeUid = uid

        if (activeFamilyId.isNullOrBlank() || activeUid.isNullOrBlank()) {
            showError("Missing family or user context for import")
            return
        }

        viewModelScope.launch {
            updateContentState {
                it.copy(
                    isImporting = true,
                    importSummary = "",
                )
            }

            when (val result = importGuidesUseCase(json, activeFamilyId, activeUid)) {
                is Result.Success -> {
                    updateContentState {
                        it.copy(
                            isImporting = false,
                            importSummary = "Imported ${result.data.successCount} guide(s). Failed: ${result.data.failureCount}",
                        )
                    }
                }
                is Result.Failure -> {
                    updateContentState { it.copy(isImporting = false) }
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                ),
            )
        }
    }

    private fun updateContentState(transform: (GuidesUiState.Content) -> GuidesUiState.Content) {
        _uiState.update { state ->
            val contentState = state as? GuidesUiState.Content ?: return@update state
            transform(contentState)
        }
    }
}
