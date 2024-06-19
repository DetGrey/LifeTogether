package com.example.lifetogether.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.usecase.item.ObserveGroceryListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ObserverViewModel @Inject constructor(
    private val observeGroceryListUseCase: ObserveGroceryListUseCase,
) : ViewModel() {
    // ---------------------------------------------- ??
    init {
        viewModelScope.launch {
            observeGroceryListUseCase()
        }
    }
}
