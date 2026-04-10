package com.example.lifetogether.ui.feature.lists.listDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.UpdateItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ListDetailsViewModel @Inject constructor(
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
) : ViewModel() {

    private var familyId: String? = null
    private var uid: String? = null
    private var currentListId: String? = null
    private var entriesJob: Job? = null
    private var listsJob: Job? = null

    private val _entries = MutableStateFlow<List<RoutineListEntry>>(emptyList())
    val entries: StateFlow<List<RoutineListEntry>> = _entries.asStateFlow()

    var listName: String by mutableStateOf("")
    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")

    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    fun setUp(addedFamilyId: String, addedUid: String, listId: String) {
        if (familyId == addedFamilyId && uid == addedUid && currentListId == listId && entriesJob != null) return
        familyId = addedFamilyId
        uid = addedUid
        currentListId = listId

        // Fetch the parent list's name from UserLists
        listsJob?.cancel()
        listsJob = viewModelScope.launch {
            fetchListItemsUseCase(
                familyId = addedFamilyId,
                listName = Constants.USER_LISTS_TABLE,
                itemType = UserList::class,
                uid = addedUid,
            ).collect { result ->
                if (result is ListItemsResultListener.Success) {
                    val found = result.listItems.filterIsInstance<UserList>()
                        .firstOrNull { it.id == listId }
                    if (found != null) listName = found.itemName
                }
            }
        }

        // Fetch entries for this list (uid param reused as listId filter)
        entriesJob?.cancel()
        entriesJob = viewModelScope.launch {
            fetchListItemsUseCase(
                familyId = addedFamilyId,
                listName = Constants.ROUTINE_LIST_ENTRIES_TABLE,
                itemType = RoutineListEntry::class,
                uid = listId,
            ).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success ->
                        _entries.value = result.listItems
                            .filterIsInstance<RoutineListEntry>()
                            .sortedWith(compareBy(nullsLast()) { it.nextDate })
                    is ListItemsResultListener.Failure -> {
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    fun completeEntry(entry: RoutineListEntry) {
        val updated = RecurrenceCalculator.applyCompletion(entry, completedAt = Date())
        viewModelScope.launch {
            when (val result = updateItemUseCase(updated, Constants.ROUTINE_LIST_ENTRIES_TABLE)) {
                is ResultListener.Success -> Unit
                is ResultListener.Failure -> {
                    error = result.message
                    showAlertDialog = true
                }
            }
        }
    }
}
