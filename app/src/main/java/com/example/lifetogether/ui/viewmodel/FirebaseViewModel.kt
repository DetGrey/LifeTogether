package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.enums.UpdateType
import com.example.lifetogether.domain.usecase.observers.ObserveAlbumsUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveAuthStateUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveCategoriesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveFamilyInformationUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGalleryImagesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGroceryListUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveRecipesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveUserInformationUseCase
import com.example.lifetogether.domain.usecase.user.FetchUserInformationUseCase
import com.example.lifetogether.domain.usecase.user.RemoveSavedUserInformationUseCase
import com.example.lifetogether.domain.usecase.user.StoreFcmTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirebaseViewModel @Inject constructor(
    private val fetchUserInformationUseCase: FetchUserInformationUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val observeGroceryListUseCase: ObserveGroceryListUseCase,
    private val observeRecipesUseCase: ObserveRecipesUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val observeGrocerySuggestionsUseCase: ObserveGrocerySuggestionsUseCase,
    private val observeUserInformationUseCase: ObserveUserInformationUseCase,
    private val observeFamilyInformationUseCase: ObserveFamilyInformationUseCase,
    private val observeAlbumsUseCase: ObserveAlbumsUseCase,
    private val observeGalleryImagesUseCase: ObserveGalleryImagesUseCase,
    private val removeSavedUserInformationUseCase: RemoveSavedUserInformationUseCase,
    private val storeFcmTokenUseCase: StoreFcmTokenUseCase,
) : ViewModel() {
    // ---------------------------------------------- USER
    // Create a StateFlow to hold user information
    private val _userInformation = MutableStateFlow<UserInformation?>(null)
    val userInformation: StateFlow<UserInformation?> = _userInformation.asStateFlow()

    init {
        // Observe changes to user information
        viewModelScope.launch {
            observeAuthStateUseCase.invoke().collect { result ->
                println("FirebaseViewModel authState: $result")
                when (result) {
                    is AuthResultListener.Success -> {
                        result.userInformation.uid?.let { uid ->
                            fetchUserInformation(uid)
                            observeUserInformation(uid)
                        }
                    }
                    is AuthResultListener.Failure -> {
                        _userInformation.value = null
                    }
                }
            }
        }

        viewModelScope.launch {
            observeCategoriesUseCase()
        }

        viewModelScope.launch {
            observeGrocerySuggestionsUseCase()
        }
    }

    private fun observeUserInformation(
        uid: String,
    ) {
        println("FirebaseViewModel observeUserInformation()")
        viewModelScope.launch {
            observeUserInformationUseCase.invoke(uid)
        }
    }

    fun observeFirestoreFamilyData(
        familyId: String,
    ) {
        println("FirebaseViewModel observeFirestoreFamilyData() familyId: $familyId")

        viewModelScope.launch {
            println("observeFirestoreFamilyData() observeFamilyInformationUseCase invoked")
            observeFamilyInformationUseCase.invoke(familyId)
        }

        viewModelScope.launch {
            println("observeFirestoreFamilyData() observeGroceryListUseCase invoked")
            observeGroceryListUseCase.invoke(familyId)
        }

        viewModelScope.launch {
            println("observeFirestoreFamilyData() observeRecipesUseCase invoked")
            observeRecipesUseCase.invoke(familyId)
        }

        viewModelScope.launch {
            println("observeFirestoreFamilyData() observeAlbumsUseCase invoked")
            observeAlbumsUseCase.invoke(familyId)
        }

        viewModelScope.launch {
            println("observeFirestoreFamilyData() observeGalleryImagesUseCase invoked")
            observeGalleryImagesUseCase.invoke(familyId)
        }
    }

    private fun fetchUserInformation(uid: String) {
        println("FirebaseViewModel before calling fetchUserInformationUseCase")
        viewModelScope.launch {
            fetchUserInformationUseCase.invoke(uid = uid).collect { result ->
                println("FirebaseViewModel fetchUserInformationUseCase result: $result")
                when (result) {
                    is AuthResultListener.Success -> {
                        _userInformation.value = result.userInformation
                    }

                    is AuthResultListener.Failure -> {
                        _userInformation.value = null
                        // Handle failure, e.g., show an error message
                    }
                }
            }
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            removeSavedUserInformationUseCase.invoke()
        }
        _userInformation.value = null
    }

    // ---------------------------------------------- ITEM COUNT
    var itemCount: Map<String, Int> by mutableStateOf(mapOf())

    fun updateItemCount(collection: String, updateType: UpdateType) {
        val currentCount = itemCount[collection] ?: 0
        val updatedCount = when (updateType) {
            UpdateType.ADD -> currentCount + 1
            UpdateType.SUBTRACT -> (currentCount - 1).coerceAtLeast(0) // Prevents negative counts
        }
        itemCount = itemCount.toMutableMap().apply {
            this[collection] = updatedCount
        }
        println("itemCount: $itemCount")
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
