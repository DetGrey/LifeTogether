package com.example.lifetogether.ui.feature.lists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
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
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val saveItemUseCase: SaveItemUseCase,
) : ViewModel() {
    private var familyId: String? = null
    private var uid: String? = null
    private var listsJob: Job? = null

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
                    setUpLists()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                }
            }
        }
    }

    private val _userLists = MutableStateFlow<List<UserList>>(emptyList())
    val userLists: StateFlow<List<UserList>> = _userLists.asStateFlow()

    // Create-list dialog state
    var showCreateDialog: Boolean by mutableStateOf(false)
    var newListName: String by mutableStateOf("")
    var newListType: ListType by mutableStateOf(ListType.ROUTINE)
    var newListVisibility: Visibility by mutableStateOf(Visibility.PRIVATE)
    var isSaving: Boolean by mutableStateOf(false)

    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")

    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    private fun setUpLists() {
        val familyIdValue = familyId ?: return
        val uidValue = uid ?: return

        listsJob?.cancel()
        listsJob = viewModelScope.launch {
            fetchListItemsUseCase(
                familyId = familyIdValue,
                listName = Constants.USER_LISTS_TABLE,
                itemType = UserList::class,
                uid = uidValue,
            ).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success ->
                        _userLists.value = result.listItems
                            .filterIsInstance<UserList>()
                            .sortedBy { it.itemName.lowercase() }
                    is ListItemsResultListener.Failure -> {
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    fun openCreateDialog() {
        newListName = ""
        newListType = ListType.ROUTINE
        newListVisibility = Visibility.PRIVATE
        showCreateDialog = true
    }

    fun createList(onCreated: (String) -> Unit) {
        val activeFamilyId = familyId
        val activeUid = uid
        if (activeFamilyId.isNullOrBlank() || activeUid.isNullOrBlank()) return
        if (newListName.isBlank()) {
            error = "Name cannot be empty"
            showAlertDialog = true
            return
        }
        isSaving = true
        viewModelScope.launch {
            val list = UserList(
                familyId = activeFamilyId,
                itemName = newListName.trim(),
                lastUpdated = Date(),
                dateCreated = Date(),
                type = newListType,
                visibility = newListVisibility,
                ownerUid = activeUid,
            )
            when (val result = saveItemUseCase(list, Constants.USER_LISTS_TABLE)) {
                is StringResultListener.Success -> {
                    showCreateDialog = false
                    onCreated(result.string)
                }
                is StringResultListener.Failure -> {
                    error = result.message
                    showAlertDialog = true
                }
            }
            isSaving = false
        }
    }
}
