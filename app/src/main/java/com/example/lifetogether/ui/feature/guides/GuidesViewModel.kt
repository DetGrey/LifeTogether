package com.example.lifetogether.ui.feature.guides

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.logic.GuideParser
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuidesViewModel @Inject constructor(
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val saveItemUseCase: SaveItemUseCase,
) : ViewModel() {
    private var familyId: String? = null
    private var uid: String? = null
    private var guidesJob: Job? = null

    private val _guides = MutableStateFlow<List<Guide>>(emptyList())
    val guides: StateFlow<List<Guide>> = _guides.asStateFlow()

    var showAddOptionsDialog: Boolean by mutableStateOf(false)
    var showImportDialog: Boolean by mutableStateOf(false)
    var isImporting: Boolean by mutableStateOf(false)
    var importSummary: String by mutableStateOf("")

    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")

    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    fun setUpGuides(
        addedFamilyId: String,
        addedUid: String,
    ) {
        if (familyId == addedFamilyId && uid == addedUid && guidesJob != null) return

        familyId = addedFamilyId
        uid = addedUid

        guidesJob?.cancel()
        guidesJob = viewModelScope.launch {
            fetchListItemsUseCase(addedFamilyId, Constants.GUIDES_TABLE, Guide::class).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success -> {
                        _guides.value = result.listItems
                            .filterIsInstance<Guide>()
                            .sortedBy { it.itemName.lowercase() }
                    }

                    is ListItemsResultListener.Failure -> {
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    fun openAddOptionsDialog() {
        showAddOptionsDialog = true
    }

    fun closeAddOptionsDialog() {
        showAddOptionsDialog = false
    }

    fun openImportDialog() {
        showImportDialog = true
        showAddOptionsDialog = false
        importSummary = ""
    }

    fun closeImportDialog() {
        showImportDialog = false
    }

    fun importGuidesFromJson(json: String) {
        val activeFamilyId = familyId
        val activeUid = uid

        if (activeFamilyId.isNullOrBlank() || activeUid.isNullOrBlank()) {
            error = "Missing family or user context for import"
            showAlertDialog = true
            return
        }

        viewModelScope.launch {
            isImporting = true
            importSummary = ""

            val parsedGuides = runCatching {
                GuideParser.parseJsonGuides(
                    json = json,
                    familyId = activeFamilyId,
                    ownerUid = activeUid,
                )
            }.getOrElse {
                error = "Could not parse JSON: ${it.message}"
                showAlertDialog = true
                isImporting = false
                return@launch
            }

            if (parsedGuides.isEmpty()) {
                error = "No valid guides were found in the selected file"
                showAlertDialog = true
                isImporting = false
                return@launch
            }

            var successCount = 0
            var failCount = 0
            parsedGuides.forEach { guide ->
                when (saveItemUseCase.invoke(guide, Constants.GUIDES_TABLE)) {
                    is StringResultListener.Success -> successCount += 1
                    is StringResultListener.Failure -> failCount += 1
                }
            }

            importSummary = "Imported $successCount guide(s). Failed: $failCount"
            isImporting = false
        }
    }
}
