package com.example.lifetogether.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.lifetogether.domain.usecase.observers.ObserveCategoriesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGroceryListUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveUserInformationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ObserverViewModel @Inject constructor(
    private val observeGroceryListUseCase: ObserveGroceryListUseCase,
    private val observeGrocerySuggestionsUseCase: ObserveGrocerySuggestionsUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val observeUserInformationUseCase: ObserveUserInformationUseCase,
) : ViewModel() {
    // ---------------------------------------------- ??
    init {
        println("ObserverViewModel init")
    }
}
